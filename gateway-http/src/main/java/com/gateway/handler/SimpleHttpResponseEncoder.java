package com.gateway.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简单的HTTP响应编码器
 * 自定义实现，用于学习和特定场景的性能优化
 */
public class SimpleHttpResponseEncoder extends MessageToByteEncoder<HttpResponse> {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpResponseEncoder.class);
    
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] HEADER_SEPARATOR = {':', ' '};
    
    @Override
    protected void encode(ChannelHandlerContext ctx, HttpResponse response, ByteBuf out) throws Exception {
        try {
            // 获取响应体内容
            ByteBuf content = null;
            if (response instanceof FullHttpResponse) {
                content = ((FullHttpResponse) response).content();
            }
            
            // 自动设置 Content-Length 头部（如果还没有设置的话）
            if (content != null && !response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                logger.debug("自动设置 Content-Length: {}", content.readableBytes());
            }
            
            // 编码状态行: HTTP/1.1 200 OK
            encodeStatusLine(response, out);
            
            // 编码响应头部
            encodeHeaders(response, out);
            
            // 编码响应体 (如果是FullHttpResponse)
            if (content != null && content.isReadable()) {
                out.writeBytes(content, content.readerIndex(), content.readableBytes());
            }
            
            logger.debug("HTTP响应编码完成: {} {}", 
                response.status().code(), response.status().reasonPhrase());
                
        } catch (Exception e) {
            logger.error("HTTP响应编码失败", e);
            throw e;
        }
    }
    
    /**
     * 编码HTTP状态行
     */
    private void encodeStatusLine(HttpResponse response, ByteBuf out) {
        // HTTP版本
        String version = response.protocolVersion().text();
        out.writeBytes(version.getBytes(CharsetUtil.US_ASCII));
        out.writeByte(' ');
        
        // 状态码
        String statusCode = String.valueOf(response.status().code());
        out.writeBytes(statusCode.getBytes(CharsetUtil.US_ASCII));
        out.writeByte(' ');
        
        // 状态描述
        String reasonPhrase = response.status().reasonPhrase();
        out.writeBytes(reasonPhrase.getBytes(CharsetUtil.US_ASCII));
        out.writeBytes(CRLF);
        
        logger.debug("编码状态行: {} {} {}", version, statusCode, reasonPhrase);
    }
    
    /**
     * 编码HTTP头部
     */
    private void encodeHeaders(HttpResponse response, ByteBuf out) {
        HttpHeaders headers = response.headers();
        
        for (String name : headers.names()) {
            for (String value : headers.getAll(name)) {
                // 头部名称
                out.writeBytes(name.getBytes(CharsetUtil.US_ASCII));
                out.writeBytes(HEADER_SEPARATOR);
                
                // 头部值
                out.writeBytes(value.getBytes(CharsetUtil.US_ASCII));
                out.writeBytes(CRLF);
                
                logger.debug("编码头部: {} = {}", name, value);
            }
        }
        
        // 头部结束标记 (空行)
        out.writeBytes(CRLF);
        logger.debug("头部编码完成");
    }
    
    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof HttpResponse;
    }
} 