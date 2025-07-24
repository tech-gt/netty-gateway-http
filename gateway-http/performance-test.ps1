# 网关性能测试脚本 - /employees/health 接口
# PowerShell 脚本用于测试网关优化后的性能

param(
    [int]$Concurrent = 10,
    [int]$Requests = 100,
    [string]$GatewayUrl = "http://localhost:8080/employees/health"
)

Write-Host "======================================"
Write-Host "    网关性能测试 - /employees/health"
Write-Host "======================================"
Write-Host "测试参数:"
Write-Host "  并发数: $Concurrent"
Write-Host "  总请求数: $Requests"
Write-Host "  目标URL: $GatewayUrl"
Write-Host "======================================"

# 检查网关是否可用
try {
    $response = Invoke-WebRequest -Uri $GatewayUrl -TimeoutSec 5 -Method GET
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ 网关服务正常，状态码: $($response.StatusCode)" -ForegroundColor Green
    } else {
        Write-Host "✗ 网关服务异常，状态码: $($response.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ 无法连接到网关服务: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "开始性能测试..." -ForegroundColor Yellow

# 记录开始时间
$startTime = Get-Date

# 创建任务数组
$jobs = @()

# 每个任务的请求数
$requestsPerJob = [math]::Ceiling($Requests / $Concurrent)

# 启动并发任务
for ($i = 0; $i -lt $Concurrent; $i++) {
    $job = Start-Job -ScriptBlock {
        param($url, $requestCount, $jobId)
        
        $results = @{
            JobId = $jobId
            SuccessCount = 0
            ErrorCount = 0
            TotalTime = 0
            MinTime = [double]::MaxValue
            MaxTime = 0
            Times = @()
        }
        
        for ($j = 0; $j -lt $requestCount; $j++) {
            try {
                $requestStart = Get-Date
                $response = Invoke-WebRequest -Uri $url -TimeoutSec 10 -Method GET
                $requestEnd = Get-Date
                $duration = ($requestEnd - $requestStart).TotalMilliseconds
                
                if ($response.StatusCode -eq 200) {
                    $results.SuccessCount++
                    $results.Times += $duration
                    $results.TotalTime += $duration
                    
                    if ($duration -lt $results.MinTime) { $results.MinTime = $duration }
                    if ($duration -gt $results.MaxTime) { $results.MaxTime = $duration }
                } else {
                    $results.ErrorCount++
                }
            } catch {
                $results.ErrorCount++
            }
        }
        
        return $results
    } -ArgumentList $GatewayUrl, $requestsPerJob, $i
    
    $jobs += $job
}

# 等待所有任务完成
Write-Host "等待所有任务完成..." -ForegroundColor Yellow
$jobs | Wait-Job | Out-Null

# 收集结果
$totalSuccess = 0
$totalErrors = 0
$allTimes = @()

foreach ($job in $jobs) {
    $result = Receive-Job $job
    $totalSuccess += $result.SuccessCount
    $totalErrors += $result.ErrorCount
    $allTimes += $result.Times
    Remove-Job $job
}

# 记录结束时间
$endTime = Get-Date
$totalDuration = ($endTime - $startTime).TotalSeconds

# 计算统计信息
$totalRequests = $totalSuccess + $totalErrors
$successRate = if ($totalRequests -gt 0) { ($totalSuccess / $totalRequests) * 100 } else { 0 }
$qps = if ($totalDuration -gt 0) { $totalSuccess / $totalDuration } else { 0 }

if ($allTimes.Count -gt 0) {
    $avgTime = ($allTimes | Measure-Object -Average).Average
    $minTime = ($allTimes | Measure-Object -Minimum).Minimum
    $maxTime = ($allTimes | Measure-Object -Maximum).Maximum
    
    # 计算百分位数
    $sortedTimes = $allTimes | Sort-Object
    $p50Index = [math]::Floor($sortedTimes.Count * 0.5)
    $p90Index = [math]::Floor($sortedTimes.Count * 0.9)
    $p95Index = [math]::Floor($sortedTimes.Count * 0.95)
    $p99Index = [math]::Floor($sortedTimes.Count * 0.99)
    
    $p50 = $sortedTimes[$p50Index]
    $p90 = $sortedTimes[$p90Index]
    $p95 = $sortedTimes[$p95Index]
    $p99 = $sortedTimes[$p99Index]
} else {
    $avgTime = $minTime = $maxTime = $p50 = $p90 = $p95 = $p99 = 0
}

# 输出结果
Write-Host ""
Write-Host "======================================"
Write-Host "            测试结果"
Write-Host "======================================"
Write-Host "总体统计:"
Write-Host "  总请求数: $totalRequests"
Write-Host "  成功请求: $totalSuccess" -ForegroundColor Green
Write-Host "  失败请求: $totalErrors" -ForegroundColor $(if ($totalErrors -gt 0) { "Red" } else { "Green" })
Write-Host "  成功率: $([math]::Round($successRate, 2))%"
Write-Host "  总耗时: $([math]::Round($totalDuration, 2)) 秒"
Write-Host "  QPS: $([math]::Round($qps, 2))" -ForegroundColor $(if ($qps -gt 100) { "Green" } elseif ($qps -gt 50) { "Yellow" } else { "Red" })
Write-Host ""
Write-Host "响应时间统计 (毫秒):"
Write-Host "  平均响应时间: $([math]::Round($avgTime, 2)) ms"
Write-Host "  最小响应时间: $([math]::Round($minTime, 2)) ms"
Write-Host "  最大响应时间: $([math]::Round($maxTime, 2)) ms"
Write-Host "  P50 响应时间: $([math]::Round($p50, 2)) ms"
Write-Host "  P90 响应时间: $([math]::Round($p90, 2)) ms"
Write-Host "  P95 响应时间: $([math]::Round($p95, 2)) ms"
Write-Host "  P99 响应时间: $([math]::Round($p99, 2)) ms"
Write-Host ""

# 性能评估
Write-Host "======================================"
Write-Host "            性能评估"
Write-Host "======================================"

if ($qps -gt 200) {
    Write-Host "🚀 性能优秀！QPS > 200" -ForegroundColor Green
} elseif ($qps -gt 100) {
    Write-Host "✅ 性能良好！100 < QPS <= 200" -ForegroundColor Green
} elseif ($qps -gt 50) {
    Write-Host "⚠️  性能一般，50 < QPS <= 100，可进一步优化" -ForegroundColor Yellow
} else {
    Write-Host "❌ 性能较差，QPS <= 50，需要优化" -ForegroundColor Red
}

if ($avgTime -lt 10) {
    Write-Host "🚀 响应时间优秀！平均 < 10ms" -ForegroundColor Green
} elseif ($avgTime -lt 50) {
    Write-Host "✅ 响应时间良好！10ms <= 平均 < 50ms" -ForegroundColor Green
} elseif ($avgTime -lt 100) {
    Write-Host "⚠️  响应时间一般，50ms <= 平均 < 100ms" -ForegroundColor Yellow
} else {
    Write-Host "❌ 响应时间较慢，平均 >= 100ms" -ForegroundColor Red
}

Write-Host ""
Write-Host "测试完成！" -ForegroundColor Green 