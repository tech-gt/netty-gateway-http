package com.gateway.config;

/**
 * 负载均衡器配置
 */
public class LoadBalancerConfig {
    private String type = "round_robin";
    private boolean enableHealthCheck = true;
    
    // Default constructor for Jackson
    public LoadBalancerConfig() {}
    
    public LoadBalancerConfig(String type, boolean enableHealthCheck) {
        this.type = type;
        this.enableHealthCheck = enableHealthCheck;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }
    
    public void setEnableHealthCheck(boolean enableHealthCheck) {
        this.enableHealthCheck = enableHealthCheck;
    }
    
    @Override
    public String toString() {
        return "LoadBalancerConfig{" +
                "type='" + type + '\'' +
                ", enableHealthCheck=" + enableHealthCheck +
                '}';
    }
} 