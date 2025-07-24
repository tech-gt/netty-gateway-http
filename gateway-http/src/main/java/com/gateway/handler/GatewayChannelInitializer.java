package com.gateway.handler;

import com.gateway.config.GatewayConfig;
import com.gateway.service.AsyncHttpForwardService;
import com.gateway.service.LoadBalancer;
import com.gateway.service.LoadBalancerFactory;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * 网关通道初始化器 - 使用单例服务优化性能
 */
public class GatewayChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final GatewayConfig config;
    private final LoadBalancer loadBalancer;
    private final AsyncHttpForwardService forwardService;
    
    public GatewayChannelInitializer(GatewayConfig config) {
        this.config = config;
        // 创建单例服务，避免重复创建
        this.loadBalancer = LoadBalancerFactory.createLoadBalancer(
            config.getLoadBalancer().getType()
        );
        this.forwardService = new AsyncHttpForwardService(config);
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP编解码器
        pipeline.addLast("httpRequestDecoder", new HttpRequestDecoder());
        pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
        
        // HTTP消息聚合器，将多个HTTP消息聚合成一个完整的HTTP消息
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(1024 * 1024)); // 1MB
        
        // 自定义的网关处理器 - 使用共享的服务实例
        pipeline.addLast("gatewayHandler", new AsyncGatewayRequestHandler(config, loadBalancer, forwardService));
    }
    
    /**
     * 获取转发服务实例（用于清理资源）
     */
    public AsyncHttpForwardService getForwardService() {
        return forwardService;
    }
} 