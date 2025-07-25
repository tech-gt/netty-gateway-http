package com.userservice.service;

import com.userservice.entity.User;
import com.userservice.exception.UserNotFoundException;
import com.userservice.exception.UserValidationException;
import com.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户业务服务类
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.info("查询所有用户");
        return userRepository.selectList(null);
    }
    
    /**
     * 分页获取用户
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(long current, long size) {
        logger.info("分页查询用户，页码：{}，大小：{}", current, size);
        Page<User> page = new Page<>(current, size);
        return userRepository.selectPage(page, null);
    }
    
    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        logger.info("根据ID查询用户：{}", id);
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new UserNotFoundException("用户不存在，ID: " + id);
        }
        return user;
    }
    
    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        logger.info("根据用户名查询用户：{}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("用户不存在，用户名: " + username);
        }
        return user;
    }
    
    /**
     * 根据邮箱获取用户
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        logger.info("根据邮箱查询用户：{}", email);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("用户不存在，邮箱: " + email);
        }
        return user;
    }
    
    /**
     * 根据状态获取用户列表
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        logger.info("根据状态查询用户：{}", status);
        return userRepository.findByStatus(status.name());
    }
    
    /**
     * 模糊查询用户名
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByUsername(String username) {
        logger.info("模糊查询用户名：{}", username);
        return userRepository.findByUsernameContaining(username);
    }
    
    /**
     * 模糊查询姓名
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByFullName(String fullName) {
        logger.info("模糊查询姓名：{}", fullName);
        return userRepository.findByFullNameContaining(fullName);
    }
    
    /**
     * 创建用户
     */
    public User createUser(User user) {
        logger.info("创建用户：{}", user.getUsername());
        
        // 验证用户名和邮箱是否已存在
        validateUserForCreate(user);
        
        userRepository.insert(user);
        logger.info("用户创建成功，ID：{}", user.getId());
        return user;
    }
    
    /**
     * 更新用户
     */
    public User updateUser(Long id, User userDetails) {
        logger.info("更新用户，ID：{}", id);
        
        User existingUser = getUserById(id);
        
        // 验证用户名和邮箱是否已被其他用户使用
        validateUserForUpdate(id, userDetails);
        
        // 更新用户信息
        updateUserFields(existingUser, userDetails);
        
        userRepository.updateById(existingUser);
        logger.info("用户更新成功，ID：{}", existingUser.getId());
        return existingUser;
    }
    
    /**
     * 更新用户状态
     */
    public User updateUserStatus(Long id, User.UserStatus status) {
        logger.info("更新用户状态，ID：{}，状态：{}", id, status);
        
        User user = getUserById(id);
        user.setStatus(status);
        
        userRepository.updateById(user);
        logger.info("用户状态更新成功，ID：{}", user.getId());
        return user;
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        logger.info("删除用户，ID：{}", id);
        
        // 先检查用户是否存在
        getUserById(id);
        userRepository.deleteById(id);
        
        logger.info("用户删除成功，ID：{}", id);
    }
    
    /**
     * 检查用户是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return userRepository.selectById(id) != null;
    }
    
    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username) != null;
    }
    
    /**
     * 检查邮箱是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email) != null;
    }
    
    /**
     * 验证新建用户
     */
    private void validateUserForCreate(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new UserValidationException("用户名已存在: " + user.getUsername());
        }
        
        if (existsByEmail(user.getEmail())) {
            throw new UserValidationException("邮箱已存在: " + user.getEmail());
        }
    }
    
    /**
     * 验证更新用户
     */
    private void validateUserForUpdate(Long id, User userDetails) {
        if (userRepository.existsByUsernameAndIdNot(userDetails.getUsername(), id)) {
            throw new UserValidationException("用户名已被其他用户使用: " + userDetails.getUsername());
        }
        
        if (userRepository.existsByEmailAndIdNot(userDetails.getEmail(), id)) {
            throw new UserValidationException("邮箱已被其他用户使用: " + userDetails.getEmail());
        }
    }
    
    /**
     * 更新用户字段
     */
    private void updateUserFields(User existingUser, User userDetails) {
        existingUser.setUsername(userDetails.getUsername());
        existingUser.setFullName(userDetails.getFullName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setAddress(userDetails.getAddress());
        
        if (userDetails.getStatus() != null) {
            existingUser.setStatus(userDetails.getStatus());
        }
    }
} 