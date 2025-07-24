package com.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;



/**
 * 网关配置类
 */
public class GatewayConfig {
    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);
    
    private int port = 8080;
    private List<BackendService> backendServices = new ArrayList<>();
    private int connectionTimeout = 5000;
    private int socketTimeout = 10000;
    private int maxConnections = 100;
    private LoadBalancerConfig loadBalancer = new LoadBalancerConfig();
    
    public static GatewayConfig load() {
        try {
            // 尝试从resources加载配置文件
            InputStream configStream = GatewayConfig.class.getResourceAsStream("/gateway.yml");
            if (configStream != null) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                return mapper.readValue(configStream, GatewayConfig.class);
            } else {
                logger.warn("配置文件gateway.yml不存在，使用默认配置");
                return createDefaultConfig();
            }
        } catch (Exception e) {
            logger.error("加载配置文件失败，使用默认配置: ", e);
            return createDefaultConfig();
        }
    }
    
    private static GatewayConfig createDefaultConfig() {
        GatewayConfig config = new GatewayConfig();
        
        // 添加默认的后端服务配置
        BackendService service1 = new BackendService();
        service1.setName("backend-api-1");
        service1.setUrl("http://localhost:8081");
        service1.setPath("/api/v1/*");
        service1.setWeight(1);
        
        BackendService service2 = new BackendService();
        service2.setName("backend-api-2");
        service2.setUrl("http://localhost:8082");
        service2.setPath("/api/v2/*");
        service2.setWeight(1);
        
        config.getBackendServices().add(service1);
        config.getBackendServices().add(service2);
        
        return config;
    }
    
    // Getters and Setters
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public List<BackendService> getBackendServices() {
        return backendServices;
    }
    
    public void setBackendServices(List<BackendService> backendServices) {
        this.backendServices = backendServices;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public LoadBalancerConfig getLoadBalancer() {
        return loadBalancer;
    }
    
    public void setLoadBalancer(LoadBalancerConfig loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
} 