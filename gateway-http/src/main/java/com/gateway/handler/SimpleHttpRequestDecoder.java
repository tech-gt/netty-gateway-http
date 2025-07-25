package com.gateway.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 简单的HTTP请求解码器
 * 自定义实现，用于学习和特定场景的性能优化
 */
public class SimpleHttpRequestDecoder extends ByteToMessageDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpRequestDecoder.class);
    
    // HTTP协议相关常量
    private static final byte CR = 13;  // \r
    private static final byte LF = 10;  // \n
    private static final byte[] CRLF = {CR, LF};
    private static final byte[] CRLFCRLF = {CR, LF, CR, LF};
    
    // 解码状态
    private enum State {
        READ_REQUEST_LINE,
        READ_HEADERS,
        READ_CONTENT
    }
    
    private State currentState = State.READ_REQUEST_LINE;
    private HttpRequest currentRequest;
    private int contentLength = 0;
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            switch (currentState) {
                case READ_REQUEST_LINE:
                    if (decodeRequestLine(in, out)) {
                        currentState = State.READ_HEADERS;
                    }
                    break;
                    
                case READ_HEADERS:
                    if (decodeHeaders(in, out)) {
                        // decodeHeaders 方法内部已经处理了没有请求体的情况
                        // 只有当有请求体时才进入 READ_CONTENT 状态
                        if (contentLength > 0) {
                            currentState = State.READ_CONTENT;
                        }
                        // 如果 contentLength <= 0，decodeHeaders 内部已经完成处理并 reset()
                    }
                    break;
                    
                case READ_CONTENT:
                    if (decodeContent(in, out)) {
                        reset();
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("HTTP解码失败", e);
            reset();
            throw e;
        }
    }
    
    /**
     * 解码请求行 (如: GET /api/v1/users HTTP/1.1)
     */
    private boolean decodeRequestLine(ByteBuf buffer, List<Object> out) {
        String line = readLine(buffer);
        if (line == null) {
            return false; // 需要更多数据
        }
        
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid HTTP request line: " + line);
        }
        
        HttpMethod method = HttpMethod.valueOf(parts[0]);
        String uri = parts[1];
        HttpVersion version = HttpVersion.valueOf(parts[2]);
        
        currentRequest = new DefaultHttpRequest(version, method, uri);
        
        logger.debug("解码请求行: {} {} {}", method, uri, version);
        return true;
    }
    
    /**
     * 解码HTTP头部
     */
    private boolean decodeHeaders(ByteBuf buffer, List<Object> out) {
        String line;
        while ((line = readLine(buffer)) != null) {
            if (line.isEmpty()) {
                // 空行表示头部结束
                String contentLengthHeader = currentRequest.headers().get(HttpHeaderNames.CONTENT_LENGTH);
                if (contentLengthHeader != null) {
                    contentLength = Integer.parseInt(contentLengthHeader);
                }
                logger.debug("头部解码完成，Content-Length: {}", contentLength);
                
                // 如果没有请求体，创建一个空的 FullHttpRequest
                if (contentLength <= 0) {
                    FullHttpRequest fullRequest = new DefaultFullHttpRequest(
                        currentRequest.protocolVersion(),
                        currentRequest.method(),
                        currentRequest.uri(),
                        io.netty.buffer.Unpooled.EMPTY_BUFFER  // 空的请求体
                    );
                    
                    // 复制头部
                    fullRequest.headers().add(currentRequest.headers());
                    
                    out.add(fullRequest);
                    logger.debug("无请求体的 FullHttpRequest 解码完成");
                    reset();
                    return true;
                }
                
                return true;
            }
            
            // 解析头部 "Key: Value"
            int colonPos = line.indexOf(':');
            if (colonPos > 0) {
                String name = line.substring(0, colonPos).trim();
                String value = line.substring(colonPos + 1).trim();
                currentRequest.headers().add(name, value);
                logger.debug("解码头部: {} = {}", name, value);
            }
        }
        return false; // 需要更多数据
    }
    
    /**
     * 解码请求体内容
     */
    private boolean decodeContent(ByteBuf buffer, List<Object> out) {
        if (buffer.readableBytes() >= contentLength) {
            // 读取请求体
            ByteBuf content = buffer.readRetainedSlice(contentLength);
            
            // 创建完整的HTTP请求对象
            FullHttpRequest fullRequest = new DefaultFullHttpRequest(
                currentRequest.protocolVersion(),
                currentRequest.method(),
                currentRequest.uri(),
                content
            );
            
            // 复制头部
            fullRequest.headers().add(currentRequest.headers());
            
            out.add(fullRequest);
            logger.debug("完整HTTP请求解码完成，内容长度: {}", contentLength);
            return true;
        }
        return false; // 需要更多数据
    }
    
    /**
     * 从ByteBuf中读取一行 (以\r\n结尾)
     */
    private String readLine(ByteBuf buffer) {
        int lineEnd = findCRLF(buffer);
        if (lineEnd == -1) {
            return null; // 没有找到完整的行
        }
        
        int lineLength = lineEnd - buffer.readerIndex();
        String line = buffer.toString(buffer.readerIndex(), lineLength, io.netty.util.CharsetUtil.UTF_8);
        buffer.readerIndex(lineEnd + 2); // 跳过 \r\n
        
        return line;
    }
    
    /**
     * 在ByteBuf中查找\r\n的位置
     */
    private int findCRLF(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int writerIndex = buffer.writerIndex();
        
        for (int i = readerIndex; i < writerIndex - 1; i++) {
            if (buffer.getByte(i) == CR && buffer.getByte(i + 1) == LF) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 重置解码器状态
     */
    private void reset() {
        currentState = State.READ_REQUEST_LINE;
        currentRequest = null;
        contentLength = 0;
    }
} 