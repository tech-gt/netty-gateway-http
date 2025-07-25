package com.gateway.service;

import com.gateway.config.BackendService;
import com.gateway.config.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 负载均衡器
 * 支持轮询(round_robin)和加权轮询(weighted_round_robin)算法
 */
public class LoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);
    
    private final GatewayConfig config;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    public LoadBalancer(GatewayConfig config) {
        this.config = config;
    }
    
    /**
     * 从可用服务列表中选择一个服务
     */
    public BackendService selectService(List<BackendService> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }
        
        // Filter enabled services
        List<BackendService> enabledServices = services.stream()
                .filter(BackendService::isEnabled)
                .collect(Collectors.toList());
        
        if (enabledServices.isEmpty()) {
            logger.warn("No enabled backend services available");
            return null;
        }
        
        if (enabledServices.size() == 1) {
            return enabledServices.get(0);
        }
        
        String loadBalancerType = config.getLoadBalancer().getType();
        
        switch (loadBalancerType.toLowerCase()) {
            case "weighted_round_robin":
                return selectByWeightedRoundRobin(enabledServices);
            case "round_robin":
            default:
                return selectByRoundRobin(enabledServices);
        }
    }
    
    /**
     * 轮询算法选择服务
     */
    private BackendService selectByRoundRobin(List<BackendService> services) {
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % services.size();
        BackendService selected = services.get(index);
        
        logger.debug("Round robin selected service: {} (index: {})", selected.getName(), index);
        return selected;
    }
    
    /**
     * 加权轮询算法选择服务
     */
    private BackendService selectByWeightedRoundRobin(List<BackendService> services) {
        // Calculate total weight
        int totalWeight = services.stream()
                .mapToInt(BackendService::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            logger.warn("Total weight is 0, falling back to round robin");
            return selectByRoundRobin(services);
        }
        
        // Use weighted selection
        int randomWeight = Math.abs(roundRobinCounter.getAndIncrement()) % totalWeight;
        int currentWeight = 0;
        
        for (BackendService service : services) {
            currentWeight += service.getWeight();
            if (randomWeight < currentWeight) {
                logger.debug("Weighted round robin selected service: {} (weight: {})", 
                           service.getName(), service.getWeight());
                return service;
            }
        }
        
        // Fallback to the last service
        BackendService lastService = services.get(services.size() - 1);
        logger.debug("Weighted round robin fallback to last service: {}", lastService.getName());
        return lastService;
    }
    
    /**
     * 重置负载均衡器状态
     */
    public void reset() {
        roundRobinCounter.set(0);
        logger.debug("Load balancer state reset");
    }
    
    /**
     * 获取当前负载均衡器状态信息
     */
    public String getStatus() {
        return String.format("LoadBalancer{type=%s, counter=%d}", 
                           config.getLoadBalancer().getType(), 
                           roundRobinCounter.get());
    }
} 