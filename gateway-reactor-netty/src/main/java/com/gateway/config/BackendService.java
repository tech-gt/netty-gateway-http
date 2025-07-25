package com.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 后端服务配置
 */
public class BackendService {
    private static final Logger logger = LoggerFactory.getLogger(BackendService.class);
    private String name;
    private String url;
    private String path;
    private int weight = 1;
    private boolean enabled = true;
    
    // Default constructor for Jackson
    public BackendService() {}
    
    public BackendService(String name, String url, String path) {
        this.name = name;
        this.url = url;
        this.path = path;
    }
    
    public BackendService(String name, String url, String path, int weight, boolean enabled) {
        this.name = name;
        this.url = url;
        this.path = path;
        this.weight = weight;
        this.enabled = enabled;
    }
    
    /**
     * 检查该服务是否匹配给定的请求路径
     */
    public boolean matches(String requestPath) {
        if (!enabled) {
            return false;
        }
        
        // Normalize request path - ensure it starts with /
        String normalizedRequestPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
        
        // Remove wildcard for matching
        String pathPattern = path.replace("/*", "");
        
        // Ensure pattern starts with /
        if (!pathPattern.startsWith("/")) {
            pathPattern = "/" + pathPattern;
        }
        
        boolean matches = normalizedRequestPath.startsWith(pathPattern);
        
        if (matches) {
            logger.debug("Service {} matches request path: {} (pattern: {})", name, requestPath, path);
        }
        
        return matches;
    }
    
    /**
     * 构建完整的目标URL
     */
    public String buildTargetUrl(String requestPath, String queryString) {
        StringBuilder targetUrl = new StringBuilder(url);
        
        // Normalize request path - ensure it starts with /
        String normalizedPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
        
        // Add the request path
        targetUrl.append(normalizedPath);
        
        // Add query string if present
        if (queryString != null && !queryString.isEmpty()) {
            targetUrl.append("?").append(queryString);
        }
        
        String result = targetUrl.toString();
        logger.debug("Built target URL: {} for request path: {}", result, requestPath);
        return result;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "BackendService{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", weight=" + weight +
                ", enabled=" + enabled +
                '}';
    }
} 