package com.gateway.server;

import com.gateway.config.GatewayConfig;
import com.gateway.handler.GatewayChannelInitializer;
import com.gateway.service.AsyncHttpForwardService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty HTTP网关服务器 - 性能优化版
 */
public class GatewayServer {
    private static final Logger logger = LoggerFactory.getLogger(GatewayServer.class);
    
    private final GatewayConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private GatewayChannelInitializer channelInitializer;
    
    public GatewayServer(GatewayConfig config) {
        this.config = config;
    }
    
    /**
     * 启动网关服务器
     */
    public void start() throws InterruptedException {
        // 优化线程组配置 - 避免过多线程导致上下文切换
        int bossThreads = 1;
        // 优化：IO密集型应用，worker线程数 = CPU核心数，避免过度竞争
        int workerThreads = Runtime.getRuntime().availableProcessors();
        
        bossGroup = new NioEventLoopGroup(bossThreads); // 处理连接请求
        workerGroup = new NioEventLoopGroup(workerThreads); // 处理业务逻辑
        
        logger.info("启动网关服务器，Boss线程数: {}, Worker线程数: {}", bossThreads, workerThreads);
        
        try {
            channelInitializer = new GatewayChannelInitializer(config);
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 2048)  // 增加backlog
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)  // 接收缓冲区
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)  // 发送缓冲区
                    .childHandler(channelInitializer);
            
            // 绑定端口并启动服务器
            channelFuture = bootstrap.bind(config.getPort()).sync();
            
            logger.info("Netty HTTP网关启动成功，监听端口: {}", config.getPort());
            logger.info("后端服务配置:");
            config.getBackendServices().forEach(service -> {
                logger.info("  - {} -> {} (路径: {})", service.getName(), service.getUrl(), service.getPath());
            });
            
            // 等待服务器关闭
            channelFuture.channel().closeFuture().sync();
            
        } finally {
            shutdown();
        }
    }
    
    /**
     * Shuts down the gateway server
     */
    public void shutdown() {
        logger.info("Shutting down gateway server...");
        
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        
        // 清理共享资源
        if (channelInitializer != null) {
            AsyncHttpForwardService forwardService = channelInitializer.getForwardService();
            if (forwardService != null) {
                forwardService.shutdown();
            }
        }
        
        // 关闭全局EventLoopGroup
        AsyncHttpForwardService.globalShutdown();
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        logger.info("Gateway server has been shut down.");
    }
} 