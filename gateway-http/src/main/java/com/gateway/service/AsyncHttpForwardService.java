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

/**
 * 高性能的非阻塞HTTP转发服务 (基于Netty连接池和连接复用)
 */
public class AsyncHttpForwardService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpForwardService.class);
    
    // 优化：减少线程数，只使用CPU核心数的线程
    private static final EventLoopGroup GLOBAL_WORKER_GROUP = 
        new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
    
    // 真正的连接池 - 基于host:port缓存连接池
    private static final ConcurrentHashMap<String, ChannelPool> CONNECTION_POOLS = new ConcurrentHashMap<>();
    
    private final GatewayConfig config;
    
    public AsyncHttpForwardService(GatewayConfig config) {
        this.config = config;
    }
    
    /**
     * 获取或创建连接池
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
                    .remoteAddress(remoteAddress);
            
            // 创建固定大小的连接池，支持连接复用
            ChannelPoolHandler poolHandler = new AbstractChannelPoolHandler() {
                @Override
                public void channelCreated(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    
                    // 设置读超时
                    pipeline.addFirst("readTimeout", 
                        new io.netty.handler.timeout.ReadTimeoutHandler(
                            config.getSocketTimeout(), TimeUnit.MILLISECONDS));
                    
                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                    
                    logger.debug("创建新的后端连接: {}", ch.remoteAddress());
                }
            };
            
            // 优化：使用SimpleChannelPool而非FixedChannelPool，避免连接池满时的阻塞
            // FixedChannelPool在连接池满时会阻塞，SimpleChannelPool会动态创建连接
            SimpleChannelPool pool = new SimpleChannelPool(bootstrap, poolHandler);
            
            logger.info("创建连接池for {}:{}", host, port);
            return pool;
        });
    }
    
    /**
     * 非阻塞转发请求到后端服务
     */
    public void forwardRequest(ChannelHandlerContext gatewayCtx, FullHttpRequest request, BackendService service) {
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
            
            logger.debug("转发请求: {} -> {}", request.uri(), backendUrl);
            
            // 创建后端请求
            FullHttpRequest backendRequest = createBackendRequest(request, backendUri);
            
            // 从连接池获取连接
            ChannelPool channelPool = getOrCreateChannelPool(host, port);
            Future<Channel> channelFuture = channelPool.acquire();
            
            channelFuture.addListener(new FutureListener<Channel>() {
                @Override
                public void operationComplete(Future<Channel> future) throws Exception {
                    if (future.isSuccess()) {
                        Channel backendChannel = future.getNow();
                        
                        // 设置响应处理器
                        backendChannel.pipeline().addLast("responseHandler", 
                            new BackendResponseHandler(gatewayCtx, channelPool, backendChannel));
                        
                        // 发送请求到后端
                        backendChannel.writeAndFlush(backendRequest).addListener(writeFuture -> {
                            if (!writeFuture.isSuccess()) {
                                logger.error("发送请求到后端失败: ", writeFuture.cause());
                                sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                                    "Failed to send request to backend");
                                // 释放连接回池
                                channelPool.release(backendChannel);
                            }
                        });
                    } else {
                        logger.error("从连接池获取连接失败: ", future.cause());
                        sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                            "Failed to acquire connection from pool");
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("转发请求时发生错误: ", e);
            sendErrorResponse(gatewayCtx, HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * 创建后端请求
     */
    private FullHttpRequest createBackendRequest(FullHttpRequest originalRequest, URI backendUri) {
        // 创建新的请求
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
        
        // 设置Host头和连接复用
        backendRequest.headers().set(HttpHeaderNames.HOST, backendUri.getHost());
        backendRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        
        return backendRequest;
    }
    
    /**
     * 检查是否应该跳过某个头部
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("host") || 
               lowerName.equals("connection");
    }
    
    /**
     * 后端响应处理器 - 支持连接复用
     */
    private static class BackendResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final ChannelHandlerContext gatewayCtx;
        private final ChannelPool channelPool;
        private final Channel backendChannel;
        private volatile boolean processed = false; // 防止重复处理
        
        public BackendResponseHandler(ChannelHandlerContext gatewayCtx, ChannelPool channelPool, Channel backendChannel) {
            this.gatewayCtx = gatewayCtx;
            this.channelPool = channelPool;
            this.backendChannel = backendChannel;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse backendResponse) throws Exception {
            if (processed) {
                return; // 防止重复处理
            }
            
            try {
                // 创建网关响应
                FullHttpResponse gatewayResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    backendResponse.status(),
                    backendResponse.content().copy()
                );
                
                // 复制响应头
                for (String name : backendResponse.headers().names()) {
                    String value = backendResponse.headers().get(name);
                    gatewayResponse.headers().set(name, value);
                }
                
                gatewayResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                
                // 发送响应给客户端
                if (gatewayCtx.channel().isActive()) {
                    gatewayCtx.writeAndFlush(gatewayResponse).addListener(ChannelFutureListener.CLOSE);
                }
                
                processed = true;
                
            } finally {
                // 确保资源清理：移除处理器并释放连接回池
                cleanupAndRelease(ctx);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (!processed) {
                logger.error("后端响应处理出错: ", cause);
                if (gatewayCtx.channel().isActive()) {
                    sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                        "Backend response error: " + cause.getMessage());
                }
                processed = true;
            }
            
            // 异常时也要清理资源
            cleanupAndRelease(ctx);
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // 后端连接意外关闭
            if (!processed && gatewayCtx.channel().isActive()) {
                sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                    "Backend connection closed unexpectedly");
                processed = true;
            }
            // 连接已关闭，不需要释放回池
        }
        
        /**
         * 清理资源并释放连接回池
         */
        private void cleanupAndRelease(ChannelHandlerContext ctx) {
            try {
                // 安全移除处理器（使用handler名称）
                if (ctx.pipeline().get("responseHandler") != null) {
                    ctx.pipeline().remove("responseHandler");
                }
            } catch (Exception e) {
                logger.warn("移除处理器时出错: ", e);
            }
            
            try {
                // 释放连接回池
                if (backendChannel.isActive()) {
                    channelPool.release(backendChannel);
                    logger.debug("连接已释放回池: {}", backendChannel.remoteAddress());
                }
            } catch (Exception e) {
                logger.warn("释放连接回池时出错: ", e);
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
            "{\"error\": \"%s\", \"message\": \"%s\", \"status\": %d}", 
            status.reasonPhrase(), message, status.code()
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
     * 关闭服务，优雅关闭所有连接池和EventLoopGroup
     */
    public static void globalShutdown() {
        // 关闭所有连接池
        for (ChannelPool pool : CONNECTION_POOLS.values()) {
            pool.close();
        }
        CONNECTION_POOLS.clear();
        
        if (GLOBAL_WORKER_GROUP != null) {
            GLOBAL_WORKER_GROUP.shutdownGracefully();
        }
    }
    
    /**
     * 实例关闭（不关闭全局资源）
     */
    public void shutdown() {
        // 不关闭全局资源，只在应用关闭时调用globalShutdown
    }
} 