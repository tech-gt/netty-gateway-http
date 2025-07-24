#!/bin/bash

echo "======================================"
echo "        员工服务测试脚本"
echo "======================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local test_name="$1"
    local url="$2"
    local expected_status="$3"
    
    echo -e "${BLUE}测试: $test_name${NC}"
    echo "URL: $url"
    
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" "$url")
    http_status=$(echo "$response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    response_body=$(echo "$response" | sed -e 's/HTTPSTATUS\:.*//g')
    
    if [ "$http_status" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ 成功 (HTTP $http_status)${NC}"
        if command -v jq &> /dev/null; then
            echo "$response_body" | jq .
        else
            echo "$response_body"
        fi
    else
        echo -e "${RED}✗ 失败 (期望: $expected_status, 实际: $http_status)${NC}"
        echo "$response_body"
    fi
    echo "--------------------------------------"
}

echo -e "${YELLOW}检查服务是否启动...${NC}"

# 1. 健康检查
test_api "员工服务健康检查" "http://localhost:8083/employees/health" 200

# 2. 网关健康检查
test_api "通过网关的健康检查" "http://localhost:8080/employees/health" 200

echo -e "${YELLOW}执行数据库查询测试...${NC}"

# 3. 获取员工列表
test_api "获取员工列表（前5个）" "http://localhost:8080/employees?page=0&size=5" 200

# 4. 获取特定员工
test_api "获取员工编号10001" "http://localhost:8080/employees/10001" 200

# 5. 根据性别查询
test_api "查询男性员工" "http://localhost:8080/employees/gender/M" 200

# 6. 根据入职年份查询
test_api "查询1985年入职的员工" "http://localhost:8080/employees/hireYear/1985" 200

# 7. 搜索员工
test_api "搜索名字包含'Georgi'的员工" "http://localhost:8080/employees/search?firstName=Georgi" 200

# 8. 获取统计信息
test_api "获取性别统计" "http://localhost:8080/employees/statistics/gender" 200

# 9. 入职日期范围查询
test_api "查询1985年入职的员工（日期范围）" "http://localhost:8080/employees/hireDate?startDate=1985-01-01&endDate=1985-12-31" 200

echo -e "${YELLOW}执行创建和更新测试...${NC}"

# 10. 创建员工测试
echo -e "${BLUE}测试: 创建新员工${NC}"
create_response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "http://localhost:8080/employees" \
  -H "Content-Type: application/json" \
  -d '{
    "empNo": 999999,
    "firstName": "Test",
    "lastName": "User",
    "gender": "M",
    "birthDate": "1990-01-01",
    "hireDate": "2024-01-01"
  }')

create_status=$(echo "$create_response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
create_body=$(echo "$create_response" | sed -e 's/HTTPSTATUS\:.*//g')

if [ "$create_status" -eq 201 ] || [ "$create_status" -eq 200 ]; then
    echo -e "${GREEN}✓ 员工创建成功 (HTTP $create_status)${NC}"
    if command -v jq &> /dev/null; then
        echo "$create_body" | jq .
    else
        echo "$create_body"
    fi
    
    # 11. 验证创建的员工
    test_api "验证创建的员工" "http://localhost:8080/employees/999999" 200
    
    # 12. 更新员工
    echo -e "${BLUE}测试: 更新员工信息${NC}"
    update_response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PUT "http://localhost:8080/employees/999999" \
      -H "Content-Type: application/json" \
      -d '{
        "empNo": 999999,
        "firstName": "Updated",
        "lastName": "User",
        "gender": "F",
        "birthDate": "1990-01-01",
        "hireDate": "2024-01-01"
      }')
    
    update_status=$(echo "$update_response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    update_body=$(echo "$update_response" | sed -e 's/HTTPSTATUS\:.*//g')
    
    if [ "$update_status" -eq 200 ]; then
        echo -e "${GREEN}✓ 员工更新成功 (HTTP $update_status)${NC}"
        if command -v jq &> /dev/null; then
            echo "$update_body" | jq .
        else
            echo "$update_body"
        fi
    else
        echo -e "${RED}✗ 员工更新失败 (HTTP $update_status)${NC}"
        echo "$update_body"
    fi
    
    # 13. 删除测试员工
    echo -e "${BLUE}测试: 删除员工${NC}"
    delete_response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE "http://localhost:8080/employees/999999")
    delete_status=$(echo "$delete_response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    delete_body=$(echo "$delete_response" | sed -e 's/HTTPSTATUS\:.*//g')
    
    if [ "$delete_status" -eq 200 ]; then
        echo -e "${GREEN}✓ 员工删除成功 (HTTP $delete_status)${NC}"
        if command -v jq &> /dev/null; then
            echo "$delete_body" | jq .
        else
            echo "$delete_body"
        fi
    else
        echo -e "${RED}✗ 员工删除失败 (HTTP $delete_status)${NC}"
        echo "$delete_body"
    fi
    
else
    echo -e "${RED}✗ 员工创建失败 (HTTP $create_status)${NC}"
    echo "$create_body"
    echo -e "${YELLOW}注意: 可能员工编号999999已存在${NC}"
fi

echo "--------------------------------------"

echo -e "${YELLOW}测试错误处理...${NC}"

# 14. 测试不存在的员工
test_api "测试不存在的员工" "http://localhost:8080/employees/9999999" 404

# 15. 测试无效的性别参数
test_api "测试无效的性别参数" "http://localhost:8080/employees/gender/X" 500

echo "======================================"
echo -e "${GREEN}测试完成！${NC}"
echo "======================================"

echo -e "${BLUE}总结:${NC}"
echo "- 如果所有测试通过，说明员工服务运行正常"
echo "- 如果有测试失败，请检查:"
echo "  1. 员工服务是否启动 (端口8083)"
echo "  2. 网关服务是否启动 (端口8080)" 
echo "  3. MySQL数据库连接是否正常"
echo "  4. employees表是否存在数据"

echo ""
echo -e "${BLUE}可用的API端点:${NC}"
echo "• 获取员工列表: GET /employees"
echo "• 获取特定员工: GET /employees/{empNo}"
echo "• 根据性别查询: GET /employees/gender/{M|F}"
echo "• 根据入职年份: GET /employees/hireYear/{year}"
echo "• 模糊搜索: GET /employees/search?firstName=xxx"
echo "• 性别统计: GET /employees/statistics/gender"
echo "• 创建员工: POST /employees"
echo "• 更新员工: PUT /employees/{empNo}"
echo "• 删除员工: DELETE /employees/{empNo}" 