# Spring Cloud Gateway

这是一个使用 Spring Cloud Gateway 构建的 API 网关，用于将请求转发到后端的 employee-service。

## 功能特性

- 将 `http://localhost:8080/employees/health` 转发到后端的 `http://localhost:8083/employees/health`
- 支持所有 `/employees/**` 路径的请求转发
- 集成 CORS 支持
- 健康检查和监控端点
- 路由规则的灵活配置

## 快速启动

### 1. 启动后端服务

首先启动 user-service (运行在端口 8083)：

```bash
cd ../user-service
mvn spring-boot:run
```

### 2. 启动网关

```bash
cd gateway-spring
mvn spring-boot:run
```

### 3. 测试网关

网关启动后，您可以通过以下方式测试：

```bash
# 测试健康检查接口
curl http://localhost:8080/employees/health

# 测试员工列表接口
curl http://localhost:8080/employees

# 测试特定员工接口
curl http://localhost:8080/employees/1
```

## 路由配置

网关配置了以下路由规则：

- `/employees/health` -> `http://localhost:8083/employees/health`
- `/employees/**` -> `http://localhost:8083/employees/**`

## 监控端点

- 健康检查: `http://localhost:8080/actuator/health`
- 网关路由信息: `http://localhost:8080/actuator/gateway/routes`

## 配置文件

主要配置在 `src/main/resources/application.yml` 中，包括：

- 服务端口配置
- 路由规则配置
- CORS 配置
- 监控端点配置 