# Reactor Netty Gateway

基于Reactor Netty的高性能响应式HTTP网关，支持HTTP请求转发、负载均衡和流量管理。

## 特性

- **响应式编程模型**: 基于Project Reactor，支持非阻塞IO和背压处理
- **高性能**: 利用Reactor Netty的高性能网络栈，支持大量并发连接
- **负载均衡**: 支持轮询(Round Robin)和加权轮询(Weighted Round Robin)算法
- **请求转发**: 透明的HTTP请求转发，保持请求头和响应头
- **CORS支持**: 内置跨域资源共享支持
- **错误处理**: 完善的错误处理和超时机制
- **配置驱动**: 基于YAML的灵活配置

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 编译和运行

1. 编译项目:
```bash
mvn clean compile
```

2. 运行网关:
```bash
mvn exec:java -Dexec.mainClass="com.gateway.ReactorGatewayApplication"
```

或者打包后运行:
```bash
mvn clean package
java -jar target/reactor-netty-gateway-1.0.0.jar
```

### 配置说明

编辑 `src/main/resources/gateway.yml` 配置文件:

```yaml
# 网关端口
port: 8080

# 连接配置
connectionTimeout: 1000  # 连接超时(毫秒)
socketTimeout: 5000      # 读超时(毫秒)
maxConnections: 10000    # 最大连接数

# 负载均衡配置
loadBalancer:
  type: "round_robin"    # round_robin 或 weighted_round_robin
  enableHealthCheck: true

# 后端服务配置
backendServices:
  - name: "service-1"
    url: "http://localhost:8081"
    path: "/api/*"
    weight: 1
    enabled: true
```

## 架构设计

### 核心组件

1. **ReactorGatewayServer**: 主服务器，处理HTTP请求和响应
2. **ReactiveHttpForwardService**: 负责请求转发和响应处理
3. **LoadBalancer**: 负载均衡器，支持多种算法
4. **GatewayConfig**: 配置管理器，支持YAML配置

### 请求处理流程

```
Client Request → ReactorGatewayServer → ReactiveHttpForwardService → LoadBalancer → Backend Service
                                   ←                              ←               ←
Client Response ← ReactorGatewayServer ← ReactiveHttpForwardService ← Backend Response
```

### 负载均衡算法

#### 轮询 (Round Robin)
```yaml
loadBalancer:
  type: "round_robin"
```

#### 加权轮询 (Weighted Round Robin)
```yaml
loadBalancer:
  type: "weighted_round_robin"

backendServices:
  - name: "service-1"
    weight: 3  # 更高权重，获得更多请求
  - name: "service-2"
    weight: 1
```

## 性能特性

### 响应式编程优势

- **非阻塞IO**: 所有操作都是非阻塞的，提高资源利用率
- **背压处理**: 自动处理生产者和消费者速度不匹配的情况
- **内存效率**: 流式处理，减少内存占用
- **高并发**: 单线程处理大量并发连接

### 性能优化配置

```yaml
# HTTP客户端优化
connectionTimeout: 1000    # 快速连接超时
socketTimeout: 5000        # 合理的读超时
maxConnections: 10000      # 高并发连接数

# JVM优化参数 (启动时添加)
-Xmx2G -Xms2G
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-Dreactor.netty.ioWorkerCount=8
```

## 监控和日志

### 日志配置

日志文件位置:
- 应用日志: `logs/reactor-netty-gateway.log`
- 访问日志: `logs/access.log`

### 关键指标

监控以下指标来评估网关性能:
- 请求响应时间
- 吞吐量 (RPS)
- 错误率
- 连接数
- 内存和CPU使用率

## 错误处理

### 常见错误码

- `404`: 未找到匹配的后端服务
- `502`: 后端服务不可用或响应错误
- `504`: 请求超时

### 错误响应格式

```json
{
  "error": "Service Not Found",
  "message": "No backend service configured for path: /api/test"
}
```

## 最佳实践

### 配置建议

1. **超时设置**: 根据后端服务特性调整超时配置
2. **连接池**: 合理设置最大连接数，避免资源耗尽
3. **权重配置**: 根据服务器性能差异设置权重
4. **健康检查**: 启用健康检查确保服务可用性

### 部署建议

1. **容器化部署**: 使用Docker容器化部署
2. **负载均衡**: 在网关前部署负载均衡器
3. **监控告警**: 设置关键指标的监控告警
4. **日志收集**: 集中收集和分析日志

## 扩展开发

### 添加新的负载均衡算法

1. 在 `LoadBalancer` 类中添加新的选择方法
2. 在配置中添加新的算法类型
3. 更新文档

### 添加中间件

可以在 `ReactorGatewayServer.handleRequest()` 方法中添加中间件逻辑:
- 认证和授权
- 限流和熔断
- 请求日志
- 指标收集

## 故障排除

### 常见问题

1. **连接被拒绝**: 检查后端服务是否启动
2. **请求超时**: 调整超时配置或检查网络延迟
3. **内存泄漏**: 检查是否有未正确释放的资源

### 调试模式

启用调试日志:
```yaml
# logback.xml
<logger name="com.gateway" level="DEBUG"/>
<logger name="reactor.netty" level="DEBUG"/>
```

## 许可证

本项目采用 MIT 许可证。 