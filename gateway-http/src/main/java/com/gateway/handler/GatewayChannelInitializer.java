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

/**
 * 网关通道初始化器 - 高性能优化版
 */
public class GatewayChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final GatewayConfig config;
    private final LoadBalancer loadBalancer;
    private final AsyncHttpForwardService forwardService;
    
    // 是否使用自定义编解码器 (可以通过配置控制)
    private final boolean useCustomCodec = true; // 临时改为true测试修复后的代码
    
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
        
        // HTTP编解码器 - 可选择使用自定义实现
        if (useCustomCodec) {
            // 使用自定义的简单编解码器
            pipeline.addLast("customHttpRequestDecoder", new SimpleHttpRequestDecoder());
            pipeline.addLast("customHttpResponseEncoder", new SimpleHttpResponseEncoder());
        } else {
            // 使用Netty内置的编解码器 (默认，更稳定)
            pipeline.addLast("httpRequestDecoder", new HttpRequestDecoder());
            pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
        }
        
        // HTTP消息聚合器，增加最大内容长度以支持更大的请求
        // 注意：自定义解码器已经生成FullHttpRequest，这里可能不需要聚合器
        if (!useCustomCodec) {
            pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024)); // 8MB
        }
        
        // 自定义的网关处理器 - 使用共享的服务实例
        pipeline.addLast("gatewayHandler", new AsyncGatewayRequestHandler(config, loadBalancer, forwardService));
    }
    
    /**
     * 获取转发服务实例（用于清理资源）
     */
    public AsyncHttpForwardService getForwardService() {
        return forwardService;
    }
    
    /**
     * 是否使用自定义编解码器
     */
    public boolean isUsingCustomCodec() {
        return useCustomCodec;
    }
} 