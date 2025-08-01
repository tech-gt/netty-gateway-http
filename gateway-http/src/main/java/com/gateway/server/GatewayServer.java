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
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty HTTP网关服务器 - 高性能优化版
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
        int bossThreads = 1; // Boss线程通常只需要1个就足够了
        // Worker线程数 = CPU核心数，处理I/O事件
        int workerThreads = 4;
        
        bossGroup = new NioEventLoopGroup(bossThreads, new DefaultThreadFactory("GatewayBoss"));
        workerGroup = new NioEventLoopGroup(workerThreads, new DefaultThreadFactory("GatewayWorker"));
        
        logger.warn("Starting gateway server with Boss threads: {}, Worker threads: {}", bossThreads, workerThreads);
        
        try {
            channelInitializer = new GatewayChannelInitializer(config);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 关键优化：大幅提升网络配置参数
                    .option(ChannelOption.SO_BACKLOG, 1024*16)  // 大幅增加backlog
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // // 优化客户端连接配置
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // // .childOption(ChannelOption.TCP_NODELAY, true) //不重要
                    //缓冲区不是越大越好. 可以设置为默认，由系统自动调整
                    // .childOption(ChannelOption.SO_RCVBUF, 256 * 1024)  // 大幅增加接收缓冲区
                    // .childOption(ChannelOption.SO_SNDBUF, 256 * 1024)  // 大幅增加发送缓冲区
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, 
                        new io.netty.channel.WriteBufferWaterMark(64 * 1024, 128 * 1024))
                    .childOption(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT)
                    // 关键参数
                    .childOption(ChannelOption.SO_LINGER, 0)  // 快速关闭连接
                    .childHandler(channelInitializer);
            
            // 绑定端口并启动服务器
            channelFuture = bootstrap.bind(config.getPort()).sync();
            
            logger.warn("Netty HTTP Gateway started successfully on port: {}", config.getPort());
            logger.warn("Backend services configured:");
            config.getBackendServices().forEach(service -> {
                logger.warn("  - {} -> {} (path: {})", service.getName(), service.getUrl(), service.getPath());
            });
            
            // 等待服务器关闭
            channelFuture.channel().closeFuture().sync();
            
        } finally {
            shutdown();
        }
    }
    
    /**
     * 关闭网关服务器
     */
    public void shutdown() {
        logger.warn("Shutting down gateway server...");
        
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
        
        logger.warn("Gateway server has been shut down.");
    }
} 