package com.userservice.repository;

import com.userservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 员工数据访问层
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    
    /**
     * 根据名字查找员工
     */
    List<Employee> findByFirstName(String firstName);
    
    /**
     * 根据姓氏查找员工
     */
    List<Employee> findByLastName(String lastName);
    
    /**
     * 根据性别查找员工
     */
    List<Employee> findByGender(Employee.Gender gender);
    
    /**
     * 根据入职日期范围查找员工
     */
    List<Employee> findByHireDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据出生日期范围查找员工
     */
    List<Employee> findByBirthDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据名字模糊查询
     */
    @Query("SELECT e FROM Employee e WHERE e.firstName LIKE %:firstName%")
    List<Employee> findByFirstNameContaining(@Param("firstName") String firstName);
    
    /**
     * 根据姓氏模糊查询
     */
    @Query("SELECT e FROM Employee e WHERE e.lastName LIKE %:lastName%")
    List<Employee> findByLastNameContaining(@Param("lastName") String lastName);
    
    /**
     * 根据全名模糊查询
     */
    @Query("SELECT e FROM Employee e WHERE CONCAT(e.firstName, ' ', e.lastName) LIKE %:fullName%")
    List<Employee> findByFullNameContaining(@Param("fullName") String fullName);
    
    /**
     * 查找在特定年份入职的员工
     */
    @Query("SELECT e FROM Employee e WHERE YEAR(e.hireDate) = :year")
    List<Employee> findByHireYear(@Param("year") int year);
    
    /**
     * 查找在特定年份出生的员工
     */
    @Query("SELECT e FROM Employee e WHERE YEAR(e.birthDate) = :year")
    List<Employee> findByBirthYear(@Param("year") int year);
    
    /**
     * 查找工作年限超过指定年数的员工
     */
    @Query("SELECT e FROM Employee e WHERE YEAR(CURRENT_DATE) - YEAR(e.hireDate) >= :years")
    List<Employee> findByYearsOfServiceGreaterThanEqual(@Param("years") int years);
    
    /**
     * 根据员工编号范围查询
     */
    List<Employee> findByEmpNoBetween(Integer startEmpNo, Integer endEmpNo);
    
    /**
     * 查找最新入职的员工
     */
    @Query("SELECT e FROM Employee e ORDER BY e.hireDate DESC")
    List<Employee> findLatestHired();
    
    /**
     * 查找最早入职的员工
     */
    @Query("SELECT e FROM Employee e ORDER BY e.hireDate ASC")
    List<Employee> findEarliestHired();
    
    /**
     * 统计不同性别的员工数量
     */
    @Query("SELECT e.gender, COUNT(e) FROM Employee e GROUP BY e.gender")
    List<Object[]> countByGender();
    
    /**
     * 统计每年入职的员工数量
     */
    @Query("SELECT YEAR(e.hireDate), COUNT(e) FROM Employee e GROUP BY YEAR(e.hireDate) ORDER BY YEAR(e.hireDate)")
    List<Object[]> countByHireYear();
} 