package com.userservice.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.userservice.entity.Employee;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工数据访问层
 */
@Repository
@Mapper
public interface EmployeeRepository extends BaseMapper<Employee> {
    
    /**
     * 根据名字查找员工
     */
    @Select("SELECT * FROM employees WHERE first_name = #{firstName}")
    List<Employee> findByFirstName(@Param("firstName") String firstName);
    
    /**
     * 根据姓氏查找员工
     */
    @Select("SELECT * FROM employees WHERE last_name = #{lastName}")
    List<Employee> findByLastName(@Param("lastName") String lastName);
    
    /**
     * 根据性别查找员工
     */
    @Select("SELECT * FROM employees WHERE gender = #{gender}")
    List<Employee> findByGender(@Param("gender") String gender);
    
    /**
     * 根据入职日期范围查找员工
     */
    @Select("SELECT * FROM employees WHERE hire_date BETWEEN #{startDate} AND #{endDate}")
    List<Employee> findByHireDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 根据出生日期范围查找员工
     */
    @Select("SELECT * FROM employees WHERE birth_date BETWEEN #{startDate} AND #{endDate}")
    List<Employee> findByBirthDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 根据名字模糊查询
     */
    @Select("SELECT * FROM employees WHERE first_name LIKE CONCAT('%', #{firstName}, '%')")
    List<Employee> findByFirstNameContaining(@Param("firstName") String firstName);
    
    /**
     * 根据姓氏模糊查询
     */
    @Select("SELECT * FROM employees WHERE last_name LIKE CONCAT('%', #{lastName}, '%')")
    List<Employee> findByLastNameContaining(@Param("lastName") String lastName);
    
    /**
     * 根据全名模糊查询
     */
    @Select("SELECT * FROM employees WHERE CONCAT(first_name, ' ', last_name) LIKE CONCAT('%', #{fullName}, '%')")
    List<Employee> findByFullNameContaining(@Param("fullName") String fullName);
    
    /**
     * 查找在特定年份入职的员工
     */
    @Select("SELECT * FROM employees WHERE YEAR(hire_date) = #{year}")
    List<Employee> findByHireYear(@Param("year") int year);
    
    /**
     * 查找在特定年份出生的员工
     */
    @Select("SELECT * FROM employees WHERE YEAR(birth_date) = #{year}")
    List<Employee> findByBirthYear(@Param("year") int year);
    
    /**
     * 查找工作年限超过指定年数的员工
     */
    @Select("SELECT * FROM employees WHERE YEAR(CURRENT_DATE) - YEAR(hire_date) >= #{years}")
    List<Employee> findByYearsOfServiceGreaterThanEqual(@Param("years") int years);
    
    /**
     * 根据员工编号范围查询
     */
    @Select("SELECT * FROM employees WHERE emp_no BETWEEN #{startEmpNo} AND #{endEmpNo}")
    List<Employee> findByEmpNoBetween(@Param("startEmpNo") Integer startEmpNo, @Param("endEmpNo") Integer endEmpNo);
    
    /**
     * 查找最新入职的员工
     */
    @Select("SELECT * FROM employees ORDER BY hire_date DESC")
    List<Employee> findLatestHired();
    
    /**
     * 查找最早入职的员工
     */
    @Select("SELECT * FROM employees ORDER BY hire_date ASC")
    List<Employee> findEarliestHired();
    
    /**
     * 统计不同性别的员工数量
     */
    @Select("SELECT gender, COUNT(*) FROM employees GROUP BY gender")
    List<Object[]> countByGender();
    
    /**
     * 统计每年入职的员工数量
     */
    @Select("SELECT YEAR(hire_date), COUNT(*) FROM employees GROUP BY YEAR(hire_date) ORDER BY YEAR(hire_date)")
    List<Object[]> countByHireYear();
} 