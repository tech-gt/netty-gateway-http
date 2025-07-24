package com.userservice.service;

import com.userservice.entity.User;
import com.userservice.exception.UserNotFoundException;
import com.userservice.exception.UserValidationException;
import com.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return userRepository.findAll();
    }
    
    /**
     * 分页获取用户
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        logger.info("分页查询用户，页码：{}，大小：{}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }
    
    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        logger.info("根据ID查询用户：{}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + id));
    }
    
    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        logger.info("根据用户名查询用户：{}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，用户名: " + username));
    }
    
    /**
     * 根据邮箱获取用户
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        logger.info("根据邮箱查询用户：{}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，邮箱: " + email));
    }
    
    /**
     * 根据状态获取用户列表
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        logger.info("根据状态查询用户：{}", status);
        return userRepository.findByStatus(status);
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
        
        User savedUser = userRepository.save(user);
        logger.info("用户创建成功，ID：{}", savedUser.getId());
        return savedUser;
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
        
        User updatedUser = userRepository.save(existingUser);
        logger.info("用户更新成功，ID：{}", updatedUser.getId());
        return updatedUser;
    }
    
    /**
     * 更新用户状态
     */
    public User updateUserStatus(Long id, User.UserStatus status) {
        logger.info("更新用户状态，ID：{}，状态：{}", id, status);
        
        User user = getUserById(id);
        user.setStatus(status);
        
        User updatedUser = userRepository.save(user);
        logger.info("用户状态更新成功，ID：{}", updatedUser.getId());
        return updatedUser;
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        logger.info("删除用户，ID：{}", id);
        
        User user = getUserById(id);
        userRepository.delete(user);
        
        logger.info("用户删除成功，ID：{}", id);
    }
    
    /**
     * 检查用户是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
    
    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    /**
     * 检查邮箱是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
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