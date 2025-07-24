package com.gateway.service;

import com.gateway.config.BackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {
    /**
     * 根据路径选择合适的后端服务实例
     * @param path 请求路径
     * @param services 可用的服务列表
     * @return 选中的服务实例，如果没有可用服务则返回null
     */
    BackendService selectService(String path, List<BackendService> services);
}

/**
 * 轮询负载均衡器实现 - 性能优化版
 */
class RoundRobinLoadBalancer implements LoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalancer.class);
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public BackendService selectService(String path, List<BackendService> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }
        
        // 过滤出匹配路径且启用的服务
        List<BackendService> matchingServices = services.stream()
                .filter(BackendService::isEnabled)
                .filter(service -> service.matches(path))
                .collect(Collectors.toList());
        
        if (matchingServices.isEmpty()) {
            logger.warn("No available services found for path: {}", path);
            return null;
        }
        
        // 如果只有一个服务，直接返回
        if (matchingServices.size() == 1) {
            BackendService selected = matchingServices.get(0);
            logger.debug("Only one service available: {}", selected.getName());
            return selected;
        }
        
        // 修复轮询选择服务的逻辑
        int currentCount = counter.getAndIncrement();
        // 使用Math.abs确保正数，避免负数取模问题
        int index = Math.abs(currentCount) % matchingServices.size();
        BackendService selected = matchingServices.get(index);
        
        logger.info("get service: {} (index: {}/{}, count: {})", 
                selected.getName(), index, matchingServices.size(), currentCount);
        
        return selected;
    }
}

/**
 * 加权轮询负载均衡器实现 - 性能优化版
 */
class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(WeightedRoundRobinLoadBalancer.class);
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public BackendService selectService(String path, List<BackendService> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }
        
        // 过滤出匹配路径且启用的服务
        List<BackendService> matchingServices = services.stream()
                .filter(BackendService::isEnabled)
                .filter(service -> service.matches(path))
                .collect(Collectors.toList());
        
        if (matchingServices.isEmpty()) {
            logger.warn("No available services found for path: {}", path);
            return null;
        }
        
        // 如果只有一个服务，直接返回
        if (matchingServices.size() == 1) {
            BackendService selected = matchingServices.get(0);
            logger.debug("Only one service available: {}", selected.getName());
            return selected;
        }
        
        // 计算总权重
        int totalWeight = matchingServices.stream()
                .mapToInt(BackendService::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            // 如果所有权重都为0或负数，回退到简单轮询
            int currentCount = counter.getAndIncrement();
            int index = Math.abs(currentCount) % matchingServices.size();
            BackendService selected = matchingServices.get(index);
            logger.debug("Invalid weights, falling back to simple round-robin. Selected service: {}", selected.getName());
            return selected;
        }
        
        // 加权轮询选择
        int currentCount = counter.getAndIncrement();
        int target = Math.abs(currentCount) % totalWeight;
        int currentWeight = 0;
        
        for (BackendService service : matchingServices) {
            currentWeight += service.getWeight();
            if (target < currentWeight) {
                logger.debug("加权轮询负载均衡选择服务: {} (权重: {}, 目标: {}, 计数: {})", 
                        service.getName(), service.getWeight(), target, currentCount);
                return service;
            }
        }
        
        // 理论上不应该到达这里，但作为后备方案
        BackendService fallback = matchingServices.get(0);
        logger.warn("Weighted round-robin failed, using first service as fallback: {}", fallback.getName());
        return fallback;
    }
}

 