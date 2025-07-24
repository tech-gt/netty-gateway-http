package com.gateway.service;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP转发服务
 */
public class HttpForwardService {
    private static final Logger logger = LoggerFactory.getLogger(HttpForwardService.class);
    
    private final GatewayConfig config;
    private final CloseableHttpClient httpClient;
    
    public HttpForwardService(GatewayConfig config) {
        this.config = config;
        
        // 配置HTTP客户端
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(config.getConnectionTimeout())
                .setConnectTimeout(config.getConnectionTimeout())
                .setSocketTimeout(config.getSocketTimeout())
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnTotal(config.getMaxConnections())
                .setMaxConnPerRoute(config.getMaxConnections() / 2)
                .build();
    }
    
    /**
     * 转发请求到后端服务
     */
    public void forwardRequest(ChannelHandlerContext ctx, FullHttpRequest request, BackendService service) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return doForwardRequest(request, service);
            } catch (Exception e) {
                logger.error("转发请求失败: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }).whenComplete((response, throwable) -> {
            if (throwable != null) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_GATEWAY, 
                    "Failed to forward request: " + throwable.getMessage());
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        });
    }
    
    /**
     * 执行实际的请求转发
     */
    private FullHttpResponse doForwardRequest(FullHttpRequest request, BackendService service) throws Exception {
        String originalUri = request.uri();
        URI uri = new URI(originalUri);
        String path = uri.getPath();
        String query = uri.getQuery();
        
        // 构建后端URL
        String backendUrl = service.buildBackendUrl(path);
        if (query != null && !query.isEmpty()) {
            backendUrl += "?" + query;
        }
        
        logger.info("转发请求到: {} -> {}", originalUri, backendUrl);
        
        // 创建HTTP请求
        HttpRequestBase backendRequest = createBackendRequest(request.method(), backendUrl);
        
        // 复制请求头
        copyHeaders(request, backendRequest);
        
        // 复制请求体（如果有）
        if (request.content().readableBytes() > 0) {
            if (backendRequest instanceof HttpEntityEnclosingRequestBase) {
                byte[] content = new byte[request.content().readableBytes()];
                request.content().readBytes(content);
                ((HttpEntityEnclosingRequestBase) backendRequest).setEntity(new ByteArrayEntity(content));
            }
        }
        
        // 执行请求
        try (CloseableHttpResponse backendResponse = httpClient.execute(backendRequest)) {
            return convertToNettyResponse(backendResponse);
        }
    }
    
    /**
     * 创建后端HTTP请求
     */
    private HttpRequestBase createBackendRequest(HttpMethod method, String url) {
        switch (method.name()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "DELETE":
                return new HttpDelete(url);
            case "HEAD":
                return new HttpHead(url);
            case "OPTIONS":
                return new HttpOptions(url);
            case "PATCH":
                return new HttpPatch(url);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
    
    /**
     * 复制请求头
     */
    private void copyHeaders(FullHttpRequest request, HttpRequestBase backendRequest) {
        for (String name : request.headers().names()) {
            String value = request.headers().get(name);
            
            // 跳过一些不应该转发的头部
            if (shouldSkipHeader(name)) {
                continue;
            }
            
            backendRequest.setHeader(name, value);
        }
    }
    
    /**
     * 检查是否应该跳过某个头部
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("host") || 
               lowerName.equals("connection") || 
               lowerName.equals("content-length") ||
               lowerName.equals("transfer-encoding");
    }
    
    /**
     * 将Apache HttpClient响应转换为Netty响应
     */
    private FullHttpResponse convertToNettyResponse(CloseableHttpResponse backendResponse) throws IOException {
        HttpResponseStatus status = HttpResponseStatus.valueOf(backendResponse.getStatusLine().getStatusCode());
        
        // 读取响应体
        byte[] content = new byte[0];
        HttpEntity entity = backendResponse.getEntity();
        if (entity != null) {
            content = EntityUtils.toByteArray(entity);
        }
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content)
        );
        
        // 复制响应头
        for (Header header : backendResponse.getAllHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            
            if (!shouldSkipHeader(name)) {
                response.headers().set(name, value);
            }
        }
        
        // 设置内容长度
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        
        return response;
    }
    
    /**
     * 发送错误响应
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
    
    /**
     * 关闭HTTP客户端
     */
    public void shutdown() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.warn("关闭HTTP客户端时发生错误: ", e);
        }
    }
} 