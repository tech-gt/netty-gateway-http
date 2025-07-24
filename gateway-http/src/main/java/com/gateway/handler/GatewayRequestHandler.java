package com.gateway.handler;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import com.gateway.service.HttpForwardService;
import com.gateway.service.LoadBalancer;
import com.gateway.service.LoadBalancerFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 网关请求处理器
 */
public class GatewayRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(GatewayRequestHandler.class);
    
    private final GatewayConfig config;
    private final HttpForwardService forwardService;
    private final LoadBalancer loadBalancer;
    
    public GatewayRequestHandler(GatewayConfig config, LoadBalancer loadBalancer) {
        this.config = config;
        this.forwardService = new HttpForwardService(config);
        this.loadBalancer = loadBalancer;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.info("Received request: {} {}", method, uri);
        
        try {
            // Parse request path
            String path = new URI(uri).getPath();
            
            // Select backend service using the load balancer
            BackendService targetService = loadBalancer.selectService(path, config.getBackendServices());
            
            if (targetService != null) {
                // Forward the request to the backend service
                forwardService.forwardRequest(ctx, request, targetService);
            } else {
                // No matching service found, return 404
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, 
                    "No backend service found for path: " + path);
            }
            
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage(), e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                "Internal server error: " + e.getMessage());
        }
    }
    

    
    /**
     * Send an error response to the client
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
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
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Channel exception caught: ", cause);
        
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                "Server error: " + cause.getMessage());
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Channel inactive: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
} 