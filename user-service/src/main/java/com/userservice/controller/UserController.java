package com.userservice.controller;

import com.userservice.entity.User;
import com.userservice.exception.UserNotFoundException;
import com.userservice.exception.UserValidationException;
import com.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户REST控制器
 */
@RestController
@RequestMapping("/users")
@Validated
@CrossOrigin(origins = "*")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.info("GET /users - page: {}, size: {}", page, size);
        
        try {
            Page<User> userPage = userService.getAllUsers(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getRecords());
            response.put("currentPage", userPage.getCurrent());
            response.put("totalItems", userPage.getTotal());
            response.put("totalPages", userPage.getPages());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户列表失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取用户列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        logger.info("GET /users/{}", id);
        
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(createSuccessResponse("用户查询成功", user));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("查询用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("查询用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        logger.info("GET /users/username/{}", username);
        
        try {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(createSuccessResponse("用户查询成功", user));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("查询用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("查询用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据邮箱获取用户
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        logger.info("GET /users/email/{}", email);
        
        try {
            User user = userService.getUserByEmail(email);
            return ResponseEntity.ok(createSuccessResponse("用户查询成功", user));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("查询用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("查询用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据状态获取用户列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getUsersByStatus(@PathVariable User.UserStatus status) {
        logger.info("GET /users/status/{}", status);
        
        try {
            List<User> users = userService.getUsersByStatus(status);
            return ResponseEntity.ok(createSuccessResponse("用户查询成功", users));
        } catch (Exception e) {
            logger.error("查询用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("查询用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 搜索用户（支持用户名和姓名模糊搜索）
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName) {
        
        logger.info("GET /users/search - username: {}, fullName: {}", username, fullName);
        
        try {
            List<User> users;
            if (username != null && !username.trim().isEmpty()) {
                users = userService.searchUsersByUsername(username.trim());
            } else if (fullName != null && !fullName.trim().isEmpty()) {
                users = userService.searchUsersByFullName(fullName.trim());
            } else {
                users = userService.getAllUsers();
            }
            
            return ResponseEntity.ok(createSuccessResponse("搜索完成", users));
        } catch (Exception e) {
            logger.error("搜索用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("搜索用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody User user) {
        logger.info("POST /users - username: {}", user.getUsername());
        
        try {
            User savedUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("用户创建成功", savedUser));
        } catch (UserValidationException e) {
            logger.warn("用户验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("创建用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody User userDetails) {
        
        logger.info("PUT /users/{} - username: {}", id, userDetails.getUsername());
        
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(createSuccessResponse("用户更新成功", updatedUser));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (UserValidationException e) {
            logger.warn("用户验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("更新用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> statusRequest) {
        
        logger.info("PATCH /users/{}/status", id);
        
        try {
            String statusStr = statusRequest.get("status");
            if (statusStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("状态参数不能为空"));
            }
            
            User.UserStatus status = User.UserStatus.valueOf(statusStr.toUpperCase());
            User updatedUser = userService.updateUserStatus(id, status);
            
            return ResponseEntity.ok(createSuccessResponse("用户状态更新成功", updatedUser));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("无效的用户状态: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("无效的用户状态"));
        } catch (Exception e) {
            logger.error("更新用户状态失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("更新用户状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /users/{}", id);
        
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(createSuccessResponse("用户删除成功", null));
        } catch (UserNotFoundException e) {
            logger.warn("用户未找到: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除用户失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("删除用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查用户是否存在
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Object>> checkUserExists(@PathVariable Long id) {
        logger.info("GET /users/{}/exists", id);
        
        try {
            boolean exists = userService.existsById(id);
            Map<String, Object> response = createSuccessResponse("检查完成", null);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查用户存在性失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("检查用户存在性失败: " + e.getMessage()));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "user-service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
} 