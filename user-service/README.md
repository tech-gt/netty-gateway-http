# Employee Service - 员工服务

基于Spring Boot实现的员工管理微服务，使用MySQL数据库的employees表，提供完整的员工CRUD操作。

## 功能特性

- ✅ **员工管理**: 创建、查询、更新、删除员工
- ✅ **多种查询**: 支持员工编号、姓名、性别、入职日期查询
- ✅ **模糊搜索**: 支持姓名模糊搜索
- ✅ **分页查询**: 支持分页和排序
- ✅ **日期范围查询**: 支持入职日期和出生日期范围查询
- ✅ **统计功能**: 性别统计、年度入职统计
- ✅ **数据验证**: 完整的字段验证和业务规则验证
- ✅ **异常处理**: 统一的异常处理和错误响应
- ✅ **MySQL数据库**: 连接现有的employees数据库

## 技术栈

- **Spring Boot 2.7.14**: 主框架
- **Spring Data JPA**: 数据访问层
- **MySQL 8**: 数据库
- **Bean Validation**: 数据验证
- **SLF4J + Logback**: 日志框架

## 数据库配置

### MySQL连接信息
- **地址**: 172.29.179.55:3306
- **数据库**: employees
- **用户名**: root
- **密码**: x1zhimen

### employees表结构
```sql
CREATE TABLE employees (
    emp_no      INT             NOT NULL,
    birth_date  DATE            NOT NULL,
    first_name  VARCHAR(14)     NOT NULL,
    last_name   VARCHAR(16)     NOT NULL,
    gender      ENUM ('M','F')  NOT NULL,
    hire_date   DATE            NOT NULL,
    PRIMARY KEY (emp_no)
);
```

## 快速启动

### 1. 编译项目
```bash
mvn clean compile
```

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问服务
- **服务地址**: http://localhost:8083
- **健康检查**: http://localhost:8083/employees/health

## API 接口

### 基础操作

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/employees` | 获取所有员工（支持分页） |
| GET | `/employees/{empNo}` | 根据员工编号获取员工 |
| POST | `/employees` | 创建员工 |
| PUT | `/employees/{empNo}` | 更新员工 |
| DELETE | `/employees/{empNo}` | 删除员工 |

### 查询操作

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/employees/firstName/{firstName}` | 根据名字获取员工 |
| GET | `/employees/lastName/{lastName}` | 根据姓氏获取员工 |
| GET | `/employees/gender/{gender}` | 根据性别获取员工(M/F) |
| GET | `/employees/hireYear/{year}` | 根据入职年份获取员工 |
| GET | `/employees/hireDate?startDate=&endDate=` | 根据入职日期范围查询 |
| GET | `/employees/search?firstName=&lastName=&fullName=` | 模糊搜索 |
| GET | `/employees/yearsOfService/{years}` | 查询工作年限超过指定年数的员工 |

### 统计功能

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/employees/statistics/gender` | 获取性别统计 |
| GET | `/employees/statistics/hireYear` | 获取年度入职统计 |
| GET | `/employees/{empNo}/exists` | 检查员工是否存在 |
| GET | `/employees/health` | 健康检查 |

## 使用示例

### 1. 获取员工列表
```bash
curl "http://localhost:8083/employees?page=0&size=10&sortBy=empNo&sortDir=asc"
```

### 2. 根据员工编号获取员工
```bash
curl http://localhost:8083/employees/10001
```

### 3. 根据性别查询员工
```bash
curl http://localhost:8083/employees/gender/M
```

### 4. 根据入职年份查询员工
```bash
curl http://localhost:8083/employees/hireYear/1985
```

### 5. 根据入职日期范围查询
```bash
curl "http://localhost:8083/employees/hireDate?startDate=1985-01-01&endDate=1985-12-31"
```

### 6. 模糊搜索员工
```bash
# 根据名字搜索
curl "http://localhost:8083/employees/search?firstName=John"

# 根据姓氏搜索
curl "http://localhost:8083/employees/search?lastName=Smith"

# 根据全名搜索
curl "http://localhost:8083/employees/search?fullName=John Smith"
```

### 7. 创建员工
```bash
curl -X POST http://localhost:8083/employees \
  -H "Content-Type: application/json" \
  -d '{
    "empNo": 999999,
    "firstName": "John",
    "lastName": "Doe",
    "gender": "M",
    "birthDate": "1980-01-01",
    "hireDate": "2024-01-01"
  }'
```

### 8. 更新员工
```bash
curl -X PUT http://localhost:8083/employees/999999 \
  -H "Content-Type: application/json" \
  -d '{
    "empNo": 999999,
    "firstName": "Jane",
    "lastName": "Doe",
    "gender": "F",
    "birthDate": "1980-01-01",
    "hireDate": "2024-01-01"
  }'
```

### 9. 获取统计信息
```bash
# 性别统计
curl http://localhost:8083/employees/statistics/gender

# 年度入职统计
curl http://localhost:8083/employees/statistics/hireYear
```

### 10. 删除员工
```bash
curl -X DELETE http://localhost:8083/employees/999999
```

## 响应格式

### 成功响应
```json
{
  "success": true,
  "message": "员工查询成功",
  "timestamp": 1640995200000,
  "data": {
    "empNo": 10001,
    "firstName": "Georgi",
    "lastName": "Facello",
    "gender": "M",
    "birthDate": "1953-09-02",
    "hireDate": "1986-06-26"
  }
}
```

### 错误响应
```json
{
  "success": false,
  "error": "员工不存在，员工编号: 999999",
  "timestamp": 1640995200000
}
```

## 通过网关访问

员工服务已配置为通过网关的 `/employees/*` 路径访问：

```bash
# 通过网关访问员工服务
curl http://localhost:8080/employees/health
curl http://localhost:8080/employees/10001
curl "http://localhost:8080/employees?page=0&size=5"
```

## 数据模型

### Employee 实体
- `empNo`: 员工编号（主键）
- `firstName`: 名字（最大14字符）
- `lastName`: 姓氏（最大16字符）
- `gender`: 性别（M/F）
- `birthDate`: 出生日期
- `hireDate`: 入职日期

### 计算字段
- `fullName`: 全名（firstName + lastName）
- `age`: 年龄（根据出生日期计算）
- `yearsOfService`: 工作年限（根据入职日期计算）

## 开发说明

### 项目结构
```
src/main/java/com/userservice/
├── UserServiceApplication.java        # 启动类
├── entity/
│   └── Employee.java                  # 员工实体
├── repository/
│   └── EmployeeRepository.java       # 数据访问层
├── service/
│   └── EmployeeService.java         # 业务服务层
├── controller/
│   └── EmployeeController.java      # REST控制器
└── exception/
    ├── EmployeeNotFoundException.java
    └── EmployeeValidationException.java
```

### 数据库连接说明
- 服务连接到远程MySQL数据库
- 使用已有的employees数据库结构
- 不会创建或修改表结构（ddl-auto: none）
- 支持现有的employees数据

### 扩展功能
可以根据需要添加以下功能：
- 部门信息关联
- 薪资信息查询
- 职位历史记录
- 员工认证和授权
- 缓存机制
- 数据导出功能 