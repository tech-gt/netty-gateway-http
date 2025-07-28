package com.userservice.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.userservice.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户数据访问层
 */
@Repository
@Mapper
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据用户状态查找用户列表
     */
    @Select("SELECT * FROM users WHERE status = #{status}")
    List<User> findByStatus(@Param("status") String status);
    
    /**
     * 根据用户名模糊查询
     */
    @Select("SELECT * FROM users WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> findByUsernameContaining(@Param("username") String username);
    
    /**
     * 根据姓名模糊查询
     */
    @Select("SELECT * FROM users WHERE full_name LIKE CONCAT('%', #{fullName}, '%')")
    List<User> findByFullNameContaining(@Param("fullName") String fullName);
    
    /**
     * 检查用户名是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND id != #{id}")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("id") Long id);
    
    /**
     * 检查邮箱是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND id != #{id}")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
} 