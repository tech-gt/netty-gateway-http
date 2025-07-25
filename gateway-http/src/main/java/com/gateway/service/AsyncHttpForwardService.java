package com.gateway.service;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.net.ConnectException;

/**
 * 高性能的非阻塞HTTP转发服务 - 连接池深度优化版
 * 关键优化：
 * 1. 使用FixedChannelPool严格控制连接数，避免连接泄漏
 * 2. 优化连接池大小分配策略
 * 3. 强化HTTP Keep-Alive机制
 * 4. 减少不必要的日志输出
 */
public class AsyncHttpForwardService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpForwardService.class);
    
    // 优化：使用CPU核心数的线程，避免过度竞争
    private static final EventLoopGroup GLOBAL_WORKER_GROUP = 
        new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 6);
    
    // 连接池缓存 - 基于host:port
    private static final ConcurrentHashMap<String, ChannelPool> CONNECTION_POOLS = new ConcurrentHashMap<>();
    
    private final GatewayConfig config;
    private final int connectionsPerPool;
    
    public AsyncHttpForwardService(GatewayConfig config) {
        this.config = config;
        // 计算每个连接池的最大连接数：总连接数 / 后端服务数，最少20个
        int backendCount = Math.max(1, config.getBackendServices().size());
        this.connectionsPerPool = Math.max(20, config.getMaxConnections());
        
        logger.warn("AsyncHttpForwardService initialized: totalConnections={}, backendCount={}, connectionsPerPool={}", 
                   config.getMaxConnections(), backendCount, connectionsPerPool);
    }
    
    /**
     * 获取或创建连接池 - 使用FixedChannelPool严格控制连接数
     */
    private ChannelPool getOrCreateChannelPool(String host, int port) {
        String key = host + ":" + port;
        return CONNECTION_POOLS.computeIfAbsent(key, k -> {
            InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(GLOBAL_WORKER_GROUP)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeout())
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 关键优化：增加缓冲区大小，提升网络性能
                    .option(ChannelOption.SO_RCVBUF, 128 * 1024 )
                    .option(ChannelOption.SO_SNDBUF, 128 * 1024)
                    // 优化连接处理
                    .option(ChannelOption.SO_LINGER, 0)
                    .option(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT)
                    .remoteAddress(remoteAddress);
            
            // 连接池处理器
            ChannelPoolHandler poolHandler = new AbstractChannelPoolHandler() {
                @Override
                public void channelCreated(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    
                    // 设置较短的超时时间
                    pipeline.addFirst("readTimeout", 
                        new io.netty.handler.timeout.ReadTimeoutHandler(
                            config.getSocketTimeout(), TimeUnit.MILLISECONDS));
                    
                    pipeline.addLast(new HttpClientCodec());
                    // 优化：增加聚合器大小
                    pipeline.addLast(new HttpObjectAggregator(16 * 1024 * 1024));
                    
                    // 减少日志输出
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created backend connection: {}", ch.remoteAddress());
                    }
                }
            };
            
            // 关键优化：使用FixedChannelPool严格控制连接数
            FixedChannelPool pool = new FixedChannelPool(
                bootstrap, 
                poolHandler, 
                connectionsPerPool  // 严格限制最大连接数
            );
            
            logger.warn("Created FixedChannelPool for {}:{} with maxConnections={}", host, port, connectionsPerPool);
            return pool;
        });
    }
    
    /**
     * 非阻塞转发请求到后端服务
     */
    public void forwardRequest(ChannelHandlerContext gatewayCtx, FullHttpRequest request, BackendService service) {
        // 为异步操作增加引用计数
        request.retain();
        forwardRequestWithRetry(gatewayCtx, request, service, 2); // 允许2次重试
    }

    /**
     * 支持重试的非阻塞转发请求
     */
    private void forwardRequestWithRetry(
            ChannelHandlerContext gatewayCtx, FullHttpRequest request, BackendService service, int retriesLeft) {
        
        try {
            URI uri = new URI(request.uri());
            String path = uri.getPath();
            String query = uri.getQuery();
            
            // 构建后端URL
            String backendUrl = service.buildBackendUrl(path);
            if (query != null && !query.isEmpty()) {
                backendUrl += "?" + query;
            }
            
            URI backendUri = new URI(backendUrl);
            String host = backendUri.getHost();
            int port = backendUri.getPort();
            if (port == -1) {
                port = "https".equals(backendUri.getScheme()) ? 443 : 80;
            }
            
            // 从连接池获取连接
            ChannelPool channelPool = getOrCreateChannelPool(host, port);
            Future<Channel> channelFuture = channelPool.acquire();
            
            channelFuture.addListener(new FutureListener<Channel>() {
                @Override
                public void operationComplete(Future<Channel> future) throws Exception {
                    if (future.isSuccess()) {
                        Channel backendChannel = future.getNow();
                        handleChannelAcquired(gatewayCtx, request, service, retriesLeft, backendUri, channelPool, backendChannel);
                    } else {
                        // 获取连接失败
                        handleConnectionFailure(gatewayCtx, request, service, retriesLeft, "Failed to acquire connection from pool", future.cause());
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Error forwarding request: ", e);
            sendErrorResponse(gatewayCtx, HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                "Internal server error: " + e.getMessage());
            request.release();
        }
    }

    private void handleChannelAcquired(ChannelHandlerContext gatewayCtx, FullHttpRequest request, BackendService service, int retriesLeft, URI backendUri, ChannelPool channelPool, Channel backendChannel) {
        // 检查连接有效性
        if (!backendChannel.isActive()) {
            handleConnectionFailure(gatewayCtx, request, service, retriesLeft, "No active backend connection available", null);
            return;
        }

        // 创建后端请求
        FullHttpRequest backendRequest = createBackendRequest(request, backendUri);

        // 发送请求到后端
        backendChannel.writeAndFlush(backendRequest).addListener(writeFuture -> {
            if (writeFuture.isSuccess()) {
                // 请求发送成功后，再添加响应处理器
                backendChannel.pipeline().addLast("responseHandler",
                        new BackendResponseHandler(gatewayCtx, channelPool, backendChannel, request, service, retriesLeft));
            } else {
                // 发送失败，销毁连接并重试
                backendChannel.close();
                handleConnectionFailure(gatewayCtx, request, service, retriesLeft, "Failed to send request to backend after retries", writeFuture.cause());
            }
        });
    }

    private void handleConnectionFailure(ChannelHandlerContext gatewayCtx, FullHttpRequest request, BackendService service, int retriesLeft, String logMessage, Throwable cause) {
        if (retriesLeft > 0) {
            long delay = (cause instanceof ConnectException) ? 100 : 50 * (3 - retriesLeft);
            gatewayCtx.channel().eventLoop().schedule(() -> {
                forwardRequestWithRetry(gatewayCtx, request, service, retriesLeft - 1);
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            if (cause != null) {
                logger.error(logMessage, cause);
            } else {
                logger.error(logMessage);
            }
            sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, logMessage);
            request.release();
        }
    }
    
    /**
     * 创建后端请求 - 优化HTTP连接复用
     */
    private FullHttpRequest createBackendRequest(FullHttpRequest originalRequest, URI backendUri) {
        FullHttpRequest backendRequest = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            originalRequest.method(),
            backendUri.getRawPath() + (backendUri.getRawQuery() != null ? "?" + backendUri.getRawQuery() : ""),
            originalRequest.content().copy()
        );
        
        // 复制请求头
        for (String name : originalRequest.headers().names()) {
            String value = originalRequest.headers().get(name);
            if (!shouldSkipHeader(name)) {
                backendRequest.headers().set(name, value);
            }
        }
        
        // 关键优化：强化HTTP Keep-Alive设置
        backendRequest.headers().set(HttpHeaderNames.HOST, backendUri.getHost());
        backendRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        // 设置更长的Keep-Alive时间
        backendRequest.headers().set(HttpHeaderNames.KEEP_ALIVE, "timeout=1200, max=20000");
        backendRequest.headers().set(HttpHeaderNames.USER_AGENT, "Netty-Gateway/2.0");
        
        return backendRequest;
    }
    
    /**
     * 检查是否应该跳过某个头部
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("host") || 
               lowerName.equals("connection") ||
               lowerName.equals("keep-alive");
    }
    
    /**
     * 后端响应处理器 - 优化连接复用
     */
    private class BackendResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final ChannelHandlerContext gatewayCtx;
        private final ChannelPool channelPool;
        private final Channel backendChannel;
        private final FullHttpRequest originalRequest;
        private final BackendService service;
        private final int retriesLeft;
        private volatile boolean processed = false;
        
        public BackendResponseHandler(
                ChannelHandlerContext gatewayCtx, ChannelPool channelPool,
                Channel backendChannel, FullHttpRequest originalRequest,
                BackendService service, int retriesLeft) {
            this.gatewayCtx = gatewayCtx;
            this.channelPool = channelPool;
            this.backendChannel = backendChannel;
            this.originalRequest = originalRequest;
            this.service = service;
            this.retriesLeft = retriesLeft;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse backendResponse) throws Exception {
            if (processed) {
                return;
            }
            
            try {
                // 创建网关响应
                FullHttpResponse gatewayResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    backendResponse.status(),
                    backendResponse.content().copy()
                );
                
                // 复制响应头，但移除连接相关头部
                for (String name : backendResponse.headers().names()) {
                    String lowerName = name.toLowerCase();
                    if (!lowerName.equals("connection") && !lowerName.equals("keep-alive")) {
                        String value = backendResponse.headers().get(name);
                        gatewayResponse.headers().set(name, value);
                    }
                }
                
                // 设置客户端连接为关闭
                gatewayResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                
                // 发送响应给客户端
                if (gatewayCtx.channel().isActive()) {
                    gatewayCtx.writeAndFlush(gatewayResponse).addListener(ChannelFutureListener.CLOSE);
                }
                
                processed = true;
                
            } finally {
                cleanupAndRelease(ctx, false);
                originalRequest.release();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // A channel that throws an exception is considered faulty and should be closed.
            cleanupAndRelease(ctx, true);

            if (processed) {
                return;
            }
            processed = true;

            // If the client channel is no longer active, we can't send a response or retry.
            if (!gatewayCtx.channel().isActive()) {
                originalRequest.release();
                return;
            }

            // For specific, transient network errors, we attempt a retry.
            if (isRetryableException(cause)) {
                handleConnectionFailure(gatewayCtx, originalRequest, service, retriesLeft, "Backend connection error, retrying...", cause);
            } else {
                // For other errors, we fail fast and report to the client.
                logger.error("Non-retryable backend error: ", cause);
                sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, "Backend processing error: " + cause.getMessage());
                originalRequest.release();
            }
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (!processed) {
                processed = true;
                // 后端连接意外关闭，这是一个典型的可重试场景 (stale connection)
                if (gatewayCtx.channel().isActive()) {
                    handleConnectionFailure(gatewayCtx, originalRequest, service, retriesLeft, "Backend connection closed unexpectedly, retrying...", null);
                } else {
                    originalRequest.release();
                }
            }
        }
        
        private boolean isRetryableException(Throwable cause) {
            // A read timeout suggests the backend is slow or unresponsive. A retry may succeed with another instance.
            if (cause instanceof io.netty.handler.timeout.ReadTimeoutException) {
                return true;
            }
            // IOExceptions like 'Connection reset by peer' or 'Connection aborted' indicate a sudden connection loss,
            // which is a classic case for a retry.
            if (cause instanceof java.io.IOException) {
                return true;
            }
            return false;
        }

        /**
         * 清理资源并释放连接回池
         */
        private void cleanupAndRelease(ChannelHandlerContext ctx, boolean forceClose) {
            try {
                if (ctx.pipeline().get("responseHandler") != null) {
                    ctx.pipeline().remove("responseHandler");
                }
            } catch (Exception e) {
                // 忽略清理错误
            }
            
            try {
                if (forceClose) {
                    backendChannel.close();
                    return;
                }

                if (backendChannel.isActive()) {
                    channelPool.release(backendChannel);
                } else {
                    // 连接已关闭，池会自动处理
                }
            } catch (Exception e) {
                // 释放失败，关闭连接
                backendChannel.close();
            }
        }
    }
    
    /**
     * 发送错误响应
     */
    private static void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        if (!ctx.channel().isActive()) {
            return;
        }
        
        String errorBody = String.format(
            "{\"error\": \"%s\", \"message\": \"%s\", \"status\": %d, \"timestamp\": %d}", 
            status.reasonPhrase(), message, status.code(), System.currentTimeMillis()
        );
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            status, 
            Unpooled.copiedBuffer(errorBody, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    /**
     * 关闭服务，优雅关闭所有连接池
     */
    public static void globalShutdown() {
        logger.warn("Shutting down AsyncHttpForwardService...");
        
        // 关闭所有连接池
        for (ChannelPool pool : CONNECTION_POOLS.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                logger.warn("Error closing connection pool: ", e);
            }
        }
        CONNECTION_POOLS.clear();
        
        if (GLOBAL_WORKER_GROUP != null) {
            GLOBAL_WORKER_GROUP.shutdownGracefully();
        }
        
        logger.warn("AsyncHttpForwardService shutdown completed");
    }
    
    /**
     * 实例关闭
     */
    public void shutdown() {
        // 实例级别的清理
    }
} 