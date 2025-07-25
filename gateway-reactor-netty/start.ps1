# Reactor Netty Gateway 启动脚本
# Windows PowerShell 版本

Write-Host "Starting Reactor Netty Gateway..." -ForegroundColor Green

# 检查Java版本
Write-Host "Checking Java version..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-String "version"
Write-Host $javaVersion

# 创建日志目录
if (!(Test-Path "logs")) {
    New-Item -ItemType Directory -Path "logs"
    Write-Host "Created logs directory" -ForegroundColor Yellow
}

# 编译项目
Write-Host "Compiling project..." -ForegroundColor Yellow
mvn clean compile

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    
    # 启动网关
    Write-Host "Starting gateway server..." -ForegroundColor Green
    Write-Host "Gateway will be available at: http://localhost:8080" -ForegroundColor Cyan
    Write-Host "Health check endpoint: http://localhost:8080/health" -ForegroundColor Cyan
    Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
    
    # 使用优化的JVM参数启动
    java -Xmx1G -Xms512M -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dreactor.netty.ioWorkerCount=4 -cp "target/classes;$env:USERPROFILE\.m2\repository\*" com.gateway.ReactorGatewayApplication
} else {
    Write-Host "Compilation failed! Please check the error messages above." -ForegroundColor Red
    exit 1
} 