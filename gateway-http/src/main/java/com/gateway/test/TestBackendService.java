package com.gateway.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 测试用的后端服务
 */
public class TestBackendService {
    private static final Logger logger = LoggerFactory.getLogger(TestBackendService.class);
    
    private final int port;
    private final String serviceName;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    
    public TestBackendService(int port, String serviceName) {
        this.port = port;
        this.serviceName = serviceName;
    }
    
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpRequestDecoder());
                            pipeline.addLast(new HttpResponseEncoder());
                            pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                            pipeline.addLast(new TestHttpHandler());
                        }
                    });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("测试后端服务 {} 启动成功，端口: {}", serviceName, port);
            
            future.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("测试后端服务 {} 已关闭", serviceName);
    }
    
    private class TestHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            String uri = request.uri();
            String method = request.method().name();
            
            logger.info("[{}] 收到请求: {} {}", serviceName, method, uri);
            
            // 构建响应数据
            String responseBody = createTestResponse(method, uri, request);
            
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
        
        private String createTestResponse(String method, String uri, FullHttpRequest request) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            
            // 读取请求体（如果有）
            String requestBody = "";
            if (request.content().readableBytes() > 0) {
                requestBody = request.content().toString(CharsetUtil.UTF_8);
            }
            
            // 构建JSON响应
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"service\": \"").append(serviceName).append("\",\n");
            json.append("  \"port\": ").append(port).append(",\n");
            json.append("  \"method\": \"").append(method).append("\",\n");
            json.append("  \"uri\": \"").append(uri).append("\",\n");
            json.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
            json.append("  \"headers\": {\n");
            
            boolean first = true;
            for (String name : request.headers().names()) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("    \"").append(name).append("\": \"").append(request.headers().get(name)).append("\"");
                first = false;
            }
            
            json.append("\n  }");
            
            if (!requestBody.isEmpty()) {
                json.append(",\n  \"requestBody\": \"").append(requestBody.replace("\"", "\\\"")).append("\"");
            }
            
            json.append(",\n  \"status\": \"success\",\n");
            json.append("  \"message\": \"Request processed successfully by ").append(serviceName).append("\"\n");
            json.append("}");
            
            return json.toString();
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("[{}] 处理请求时发生错误: ", serviceName, cause);
            ctx.close();
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法: java TestBackendService <port> <serviceName>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        String serviceName = args[1];
        
        TestBackendService service = new TestBackendService(port, serviceName);
        
        try {
            service.start();
        } catch (InterruptedException e) {
            logger.error("启动测试后端服务失败: ", e);
        }
    }
} 