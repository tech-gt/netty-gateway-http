package com.gateway.config;

import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Arrays;
import java.util.List;

@Configuration
public class LoadBalancerConfig {

    @Bean
    @Primary
    public ServiceInstanceListSupplier serviceInstanceListSupplier() {
        return new EmployeeServiceInstanceListSupplier("employee-service");
    }

    class EmployeeServiceInstanceListSupplier implements ServiceInstanceListSupplier {

        private final String serviceId;

        EmployeeServiceInstanceListSupplier(String serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            return Flux.just(Arrays.asList(
                    new DefaultServiceInstance(serviceId + "-1", serviceId, "localhost", 8083, false),
                    new DefaultServiceInstance(serviceId + "-2", serviceId, "localhost", 8084, false),
                    new DefaultServiceInstance(serviceId + "-3", serviceId, "localhost", 8085, false)
            ));
        }
    }
} 