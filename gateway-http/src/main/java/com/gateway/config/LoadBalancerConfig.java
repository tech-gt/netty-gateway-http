package com.gateway.config;

/**
 * 负载均衡器配置类
 */
public class LoadBalancerConfig {
    private String type = "round_robin";
    private boolean enableHealthCheck = true;
    
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
} 