package com.gateway;

import com.gateway.server.GatewayServer;
import com.gateway.config.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关应用主启动类
 */
public class GatewayApplication {
    private static final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);
    
    public static void main(String[] args) {
        try {
            // 加载配置
            GatewayConfig config = GatewayConfig.load();
            
            // 启动网关服务器
            GatewayServer server = new GatewayServer(config);
            
            logger.info("正在启动Netty HTTP网关...");
            logger.info("网关端口: {}", config.getPort());
            logger.info("后端服务数量: {}", config.getBackendServices().size());
            
            // 启动服务器
            server.start();
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在关闭网关服务器...");
                server.shutdown();
            }));
            
        } catch (Exception e) {
            logger.error("启动网关失败: ", e);
            System.exit(1);
        }
    }
} 