#!/bin/bash

echo "正在编译用户服务..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo "编译成功!"

# 创建日志目录
mkdir -p logs

echo "启动用户服务..."
echo "服务地址: http://localhost:8083"
echo "H2控制台: http://localhost:8083/h2-console"
echo "健康检查: http://localhost:8083/users/health"
echo "按 Ctrl+C 停止服务"

mvn spring-boot:run 