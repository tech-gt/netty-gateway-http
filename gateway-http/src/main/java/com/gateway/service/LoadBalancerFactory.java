package com.gateway.service;

/**
 * 负载均衡器工厂
 */
public class LoadBalancerFactory {
    public static LoadBalancer createLoadBalancer(String type) {
        switch (type.toLowerCase()) {
            case "round_robin":
            case "roundrobin":
                return new RoundRobinLoadBalancer();
            case "weighted_round_robin":
            case "weightedroundrobin":
                return new WeightedRoundRobinLoadBalancer();
            default:
                return new RoundRobinLoadBalancer(); // 默认使用轮询
        }
    }
} 