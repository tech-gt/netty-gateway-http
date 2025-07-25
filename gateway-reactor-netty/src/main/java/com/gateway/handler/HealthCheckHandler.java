package com.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健康检查处理器
 * 提供网关健康状态检查接口
 */
public class HealthCheckHandler {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckHandler.class);
    
    private static final String HEALTH_RESPONSE_TEMPLATE = """
            {
                "status": "UP",
                "timestamp": "%s",
                "service": "reactor-netty-gateway",
                "version": "1.0.0",
                "uptime": "%d seconds"
            }
            """;
    
    private final long startTime = System.currentTimeMillis();
    
    /**
     * 处理健康检查请求
     */
    public Mono<Void> handleHealthCheck(HttpServerRequest request, HttpServerResponse response) {
        logger.debug("Health check request received from: {}", 
                    request.remoteAddress() != null ? request.remoteAddress().toString() : "unknown");
        
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String healthResponse = String.format(HEALTH_RESPONSE_TEMPLATE, timestamp, uptime);
        
        return response
                .status(200)
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache")
                .sendString(Mono.just(healthResponse))
                .then()
                .doOnSuccess(unused -> logger.debug("Health check response sent successfully"));
    }
    
    /**
     * 检查是否为健康检查请求
     */
    public static boolean isHealthCheckRequest(String path) {
        return "/health".equals(path) || "/health/".equals(path);
    }
} 