package com.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * 网关配置类
 * 负责加载和管理网关的配置信息
 */
public class GatewayConfig {
    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);
    
    private int port;
    private int connectionTimeout;
    private int socketTimeout;
    private int maxConnections;
    private LoadBalancerConfig loadBalancer;
    private List<BackendService> backendServices;
    
    // Default constructor for Jackson
    public GatewayConfig() {}
    
    /**
     * 从classpath加载配置文件
     */
    public static GatewayConfig load() {
        return load("gateway.yml");
    }
    
    /**
     * 从指定配置文件加载
     */
    public static GatewayConfig load(String configFile) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream inputStream = GatewayConfig.class.getClassLoader()
                    .getResourceAsStream(configFile);
            
            if (inputStream == null) {
                throw new RuntimeException("Configuration file not found: " + configFile);
            }
            
            GatewayConfig config = mapper.readValue(inputStream, GatewayConfig.class);
            logger.info("Gateway configuration loaded successfully from: {}", configFile);
            logger.info("Gateway port: {}", config.getPort());
            logger.info("Backend services count: {}", config.getBackendServices().size());
            
            return config;
        } catch (Exception e) {
            logger.error("Failed to load gateway configuration from: " + configFile, e);
            throw new RuntimeException("Failed to load gateway configuration", e);
        }
    }
    
    // Getters and Setters
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
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
    
    public List<BackendService> getBackendServices() {
        return backendServices;
    }
    
    public void setBackendServices(List<BackendService> backendServices) {
        this.backendServices = backendServices;
    }
} 