package com.gateway.service;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 响应式HTTP转发服务
 * 负责将请求转发到后端服务并返回响应
 */
public class ReactiveHttpForwardService {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveHttpForwardService.class);
    
    private final GatewayConfig config;
    private final LoadBalancer loadBalancer;
    private final HttpClient httpClient;
    
    public ReactiveHttpForwardService(GatewayConfig config) {
        this.config = config;
        this.loadBalancer = new LoadBalancer(config);
        
        // Configure HTTP client with connection pooling and timeouts
        this.httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(config.getSocketTimeout()))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeout())
                .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
                .option(io.netty.channel.ChannelOption.TCP_NODELAY, true);
    }
    
    /**
     * 转发HTTP请求到后端服务
     */
    public Mono<Void> forwardRequest(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.path();
        String queryString = request.uri().contains("?") ? 
            request.uri().substring(request.uri().indexOf("?") + 1) : null;
        
        // Find matching backend service
        BackendService targetService = findTargetService(requestPath);
        if (targetService == null) {
            return handleServiceNotFound(response, requestPath);
        }
        
        // Build target URL
        String targetUrl = targetService.buildTargetUrl(requestPath, queryString);
        logger.debug("Forwarding request to: {}", targetUrl);
        
        // Forward request and handle response
        return httpClient
                .headers(headers -> {
                    // Copy request headers (excluding hop-by-hop headers)
                    request.requestHeaders().forEach(entry -> {
                        String headerName = entry.getKey().toLowerCase();
                        if (!isHopByHopHeader(headerName)) {
                            headers.add(entry.getKey(), entry.getValue());
                        }
                    });
                    
                    // Add/modify headers for backend
                    headers.set("X-Forwarded-For", getClientIp(request));
                    headers.set("X-Forwarded-Proto", "http");
                    headers.set("X-Gateway", "reactor-netty-gateway");
                })
                .request(request.method())
                .uri(targetUrl)
                .responseContent()
                .aggregate()
                .asString()
                .flatMap(responseBody -> {
                    logger.debug("Received response from: {}", targetUrl);
                    return response
                            .status(200)
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just(responseBody))
                            .then();
                })
                .doOnSuccess(unused -> {
                    logger.debug("Response forwarded successfully from: {}", targetUrl);
                })
                .doOnError(throwable -> {
                    logger.error("Error forwarding request to {}: {}", targetUrl, throwable.getMessage());
                })
                .onErrorResume(throwable -> {
                    // Handle connection errors (backend service unavailable)
                    logger.warn("Backend service unavailable: {}", targetUrl);
                    return response
                            .status(502)
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just("{\"error\":\"Backend Service Unavailable\",\"message\":\"Cannot connect to " + targetUrl + "\"}"))
                            .then();
                });
    }
    
    /**
     * 查找匹配的后端服务
     */
    private BackendService findTargetService(String requestPath) {
        List<BackendService> matchingServices = config.getBackendServices().stream()
                .filter(service -> service.matches(requestPath))
                .collect(Collectors.toList());
        
        if (matchingServices.isEmpty()) {
            logger.warn("No backend service found for path: {}", requestPath);
            return null;
        }
        
        // Use load balancer to select service
        return loadBalancer.selectService(matchingServices);
    }
    
    /**
     * 处理服务未找到的情况
     */
    private Mono<Void> handleServiceNotFound(HttpServerResponse response, String requestPath) {
        logger.warn("No backend service configured for path: {}", requestPath);
        
        String errorResponse = String.format(
                "{\"error\":\"Service Not Found\",\"message\":\"No backend service configured for path: %s\"}",
                requestPath
        );
        
        return response
                .status(404)
                .header("Content-Type", "application/json")
                .sendString(Mono.just(errorResponse))
                .then();
    }
    
    /**
     * 复制响应头（排除hop-by-hop头）
     */
    private void copyResponseHeaders(HttpClientResponse clientResponse, HttpServerResponse response) {
        clientResponse.responseHeaders().forEach(entry -> {
            String headerName = entry.getKey().toLowerCase();
            if (!isHopByHopHeader(headerName)) {
                response.header(entry.getKey(), entry.getValue());
            }
        });
    }
    
    /**
     * 检查是否为hop-by-hop头部
     */
    private boolean isHopByHopHeader(String headerName) {
        return headerName.equals("connection") ||
               headerName.equals("keep-alive") ||
               headerName.equals("proxy-authenticate") ||
               headerName.equals("proxy-authorization") ||
               headerName.equals("te") ||
               headerName.equals("trailers") ||
               headerName.equals("transfer-encoding") ||
               headerName.equals("upgrade");
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServerRequest request) {
        String xForwardedFor = request.requestHeaders().get("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.requestHeaders().get("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.remoteAddress() != null ? 
               request.remoteAddress().getAddress().getHostAddress() : "unknown";
    }
} 