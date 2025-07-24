package com.gateway.config;

/**
 * 后端服务配置
 */
public class BackendService {
    private String name;
    private String url;
    private String path;
    private int weight = 1;
    private boolean enabled = true;
    
    public BackendService() {}
    
    public BackendService(String name, String url, String path) {
        this.name = name;
        this.url = url;
        this.path = path;
    }
    
    /**
     * 检查请求路径是否匹配此服务
     */
    public boolean matches(String requestPath) {
        if (path == null || requestPath == null) {
            return false;
        }
        
        // 简单的路径匹配逻辑
        if (path.endsWith("/*")) {
            String prefix = path.substring(0, path.length() - 2);
            return requestPath.startsWith(prefix);
        } else {
            return path.equals(requestPath);
        }
    }
    
    /**
     * 构建完整的后端URL
     */
    public String buildBackendUrl(String requestPath) {
        if (url.endsWith("/")) {
            return url + (requestPath.startsWith("/") ? requestPath.substring(1) : requestPath);
        } else {
            return url + (requestPath.startsWith("/") ? requestPath : "/" + requestPath);
        }
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