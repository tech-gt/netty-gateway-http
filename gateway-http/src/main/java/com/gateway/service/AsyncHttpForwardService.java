package com.gateway.service;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 高性能的非阻塞HTTP转发服务 (基于Netty连接池)
 */
public class AsyncHttpForwardService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpForwardService.class);
    
    // 全局共享的EventLoopGroup，避免重复创建
    private static final EventLoopGroup GLOBAL_WORKER_GROUP = new NioEventLoopGroup();
    
    // 连接池 - 基于host:port缓存Bootstrap
    private static final ConcurrentHashMap<String, Bootstrap> CONNECTION_POOL = new ConcurrentHashMap<>();
    
    private final GatewayConfig config;
    
    public AsyncHttpForwardService(GatewayConfig config) {
        this.config = config;
    }
    
    /**
     * 获取或创建Bootstrap连接
     */
    private Bootstrap getOrCreateBootstrap(String host, int port) {
        String key = host + ":" + port;
        return CONNECTION_POOL.computeIfAbsent(key, k -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(GLOBAL_WORKER_GROUP)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeout())
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_REUSEADDR, true);
            
            logger.debug("Created new bootstrap for {}:{}", host, port);
            return bootstrap;
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
            
            // 获取复用的Bootstrap
            Bootstrap bootstrap = getOrCreateBootstrap(host, port);
            
            // 建立到后端的连接
            ChannelFuture connectFuture = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    
                    // 只设置一次读超时，使用配置的socketTimeout
                    pipeline.addFirst("readTimeout", 
                        new io.netty.handler.timeout.ReadTimeoutHandler(
                            config.getSocketTimeout(), TimeUnit.MILLISECONDS));
                    
                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                    pipeline.addLast(new BackendResponseHandler(gatewayCtx));
                }
            }).connect(host, port);
            
            // 连接成功后发送请求
            connectFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel backendChannel = future.channel();
                    
                    // 发送请求到后端
                    backendChannel.writeAndFlush(backendRequest).addListener(writeFuture -> {
                        if (!writeFuture.isSuccess()) {
                            logger.error("发送请求到后端失败: ", writeFuture.cause());
                            sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                                "Failed to send request to backend");
                            backendChannel.close();
                        }
                    });
                } else {
                    logger.error("连接后端服务失败: ", future.cause());
                    sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                        "Failed to connect to backend service");
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
     * 后端响应处理器
     */
    private static class BackendResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final ChannelHandlerContext gatewayCtx;
        
        public BackendResponseHandler(ChannelHandlerContext gatewayCtx) {
            this.gatewayCtx = gatewayCtx;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse backendResponse) throws Exception {
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
            gatewayCtx.writeAndFlush(gatewayResponse).addListener(ChannelFutureListener.CLOSE);
            
            // 关闭后端连接
            ctx.close();
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("后端响应处理出错: ", cause);
            sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                "Backend response error: " + cause.getMessage());
            ctx.close();
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // 后端连接意外关闭
            if (gatewayCtx.channel().isActive()) {
                sendErrorResponse(gatewayCtx, HttpResponseStatus.BAD_GATEWAY, 
                    "Backend connection closed unexpectedly");
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
     * 关闭服务，优雅关闭全局EventLoopGroup
     */
    public static void globalShutdown() {
        if (GLOBAL_WORKER_GROUP != null) {
            GLOBAL_WORKER_GROUP.shutdownGracefully();
        }
        CONNECTION_POOL.clear();
    }
    
    /**
     * 实例关闭（不关闭全局资源）
     */
    public void shutdown() {
        // 不关闭全局资源，只在应用关闭时调用globalShutdown
    }
} 