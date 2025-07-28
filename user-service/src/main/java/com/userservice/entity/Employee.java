package com.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 员工实体类 - 对应employees表
 */
@TableName("employees")
public class Employee {
    
    @TableId(value = "emp_no", type = IdType.INPUT)
    private Integer empNo;
    
    @TableField("birth_date")
    @NotNull(message = "出生日期不能为空")
    @Past(message = "出生日期必须是过去的日期")
    private LocalDate birthDate;
    
    @TableField("first_name")
    @NotBlank(message = "名字不能为空")
    @Size(max = 14, message = "名字长度不能超过14个字符")
    private String firstName;
    
    @TableField("last_name")
    @NotBlank(message = "姓氏不能为空")
    @Size(max = 16, message = "姓氏长度不能超过16个字符")
    private String lastName;
    
    @NotNull(message = "性别不能为空")
    private Gender gender;
    
    @TableField("hire_date")
    @NotNull(message = "入职日期不能为空")
    private LocalDate hireDate;
    
    // 性别枚举
    public enum Gender {
        M, F
    }
    
    // 构造函数
    public Employee() {}
    
    public Employee(Integer empNo, String firstName, String lastName, Gender gender, LocalDate birthDate, LocalDate hireDate) {
        this.empNo = empNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
    }
    
    // Getters and Setters
    public Integer getEmpNo() {
        return empNo;
    }
    
    public void setEmpNo(Integer empNo) {
        this.empNo = empNo;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public Gender getGender() {
        return gender;
    }
    
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    
    public LocalDate getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
    
    /**
     * 获取全名
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * 计算工作年限
     */
    public int getYearsOfService() {
        return LocalDate.now().getYear() - hireDate.getYear();
    }
    
    /**
     * 计算年龄
     */
    public int getAge() {
        return LocalDate.now().getYear() - birthDate.getYear();
    }
    
    @Override
    public String toString() {
        return "Employee{" +
                "empNo=" + empNo +
                ", birthDate=" + birthDate +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", gender=" + gender +
                ", hireDate=" + hireDate +
                '}';
    }
} 