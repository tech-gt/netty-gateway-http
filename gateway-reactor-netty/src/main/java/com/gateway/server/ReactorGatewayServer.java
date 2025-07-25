package com.gateway.server;

import com.gateway.config.GatewayConfig;
import com.gateway.handler.HealthCheckHandler;
import com.gateway.service.ReactiveHttpForwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;

/**
 * Reactor Netty 网关服务器
 * 基于响应式编程模型的高性能HTTP网关
 */
public class ReactorGatewayServer {
    private static final Logger logger = LoggerFactory.getLogger(ReactorGatewayServer.class);
    
    private final GatewayConfig config;
    private final ReactiveHttpForwardService forwardService;
    private final HealthCheckHandler healthCheckHandler;
    
    public ReactorGatewayServer(GatewayConfig config) {
        this.config = config;
        this.forwardService = new ReactiveHttpForwardService(config);
        this.healthCheckHandler = new HealthCheckHandler();
    }
    
    /**
     * 启动网关服务器
     */
    public Mono<DisposableServer> start() {
        return HttpServer.create()
                .port(config.getPort())
                .handle(this::handleRequest)
                .bind()
                .cast(DisposableServer.class)
                .doOnSuccess(server -> {
                    logger.info("Reactor Netty Gateway started on port: {}", server.port());
                })
                .doOnError(throwable -> {
                    logger.error("Failed to start Reactor Netty Gateway", throwable);
                });
    }
    
    /**
     * 处理HTTP请求的核心方法
     */
    private Mono<Void> handleRequest(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.path();
        String method = request.method().name();
        String remoteAddress = request.remoteAddress() != null ? 
            request.remoteAddress().toString() : "unknown";
        
        logger.debug("Received {} request for path: {} from: {}", method, requestPath, remoteAddress);
        
        // Add CORS headers
        response.header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight OPTIONS requests
        if ("OPTIONS".equals(method)) {
            return response.status(200).send();
        }
        
        // Handle health check requests
        if (HealthCheckHandler.isHealthCheckRequest(requestPath)) {
            return healthCheckHandler.handleHealthCheck(request, response);
        }
        
        // Forward request to backend service
        return forwardService.forwardRequest(request, response)
                .timeout(Duration.ofMillis(config.getSocketTimeout()))
                .doOnSuccess(unused -> {
                    logger.debug("Request {} {} forwarded successfully", method, requestPath);
                })
                .doOnError(throwable -> {
                    logger.error("Failed to forward request {} {}: {}", 
                               method, requestPath, throwable.getMessage());
                    
                                                              // Send error response if not already committed
                     if (!response.hasSentHeaders()) {
                         response.status(502)
                                .header("Content-Type", "application/json")
                                .sendString(Mono.just("{\"error\":\"Gateway Error\",\"message\":\"" + 
                                          throwable.getMessage() + "\"}"))
                                .then()
                                .subscribe();
                     }
                })
                .onErrorComplete(); // Complete the stream even on error to prevent connection leaks
    }
} 