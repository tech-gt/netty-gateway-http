package com.gateway;

import com.gateway.config.GatewayConfig;
import com.gateway.server.ReactorGatewayServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Reactor Netty 网关应用主启动类
 * 基于响应式编程模型，提供高性能的HTTP请求转发服务
 */
public class ReactorGatewayApplication {
    private static final Logger logger = LoggerFactory.getLogger(ReactorGatewayApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Reactor Netty Gateway...");
        
        try {
            // Load configuration
            GatewayConfig config = GatewayConfig.load();
            
            // Create and start gateway server
            ReactorGatewayServer server = new ReactorGatewayServer(config);
            
            logger.info("Gateway server starting on port: {}", config.getPort());
            logger.info("Backend services configured: {}", config.getBackendServices().size());
            
            // Start server and block to keep application running
            server.start()
                .doOnNext(disposableServer -> {
                    logger.info("Reactor Netty Gateway started successfully on port: {}", 
                              disposableServer.port());
                })
                .doOnError(throwable -> {
                    logger.error("Failed to start gateway server: ", throwable);
                    System.exit(1);
                })
                .flatMap(disposableServer -> {
                    // Register shutdown hook
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        logger.info("Shutting down gateway server...");
                        disposableServer.disposeNow();
                    }));
                    
                    // Block until the server is disposed
                    return disposableServer.onDispose();
                })
                .block();
                
        } catch (Exception e) {
            logger.error("Failed to start Reactor Netty Gateway: ", e);
            System.exit(1);
        }
    }
} 