package com.userservice.service;

import com.userservice.entity.Employee;
import com.userservice.exception.EmployeeNotFoundException;
import com.userservice.exception.EmployeeValidationException;
import com.userservice.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工业务服务类
 */
@Service
@Transactional
public class EmployeeService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    /**
     * Get all employees
     */
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        logger.info("Fetching all employees.");
        return employeeRepository.findAll();
    }
    
    /**
     * Get employees with pagination
     */
    @Transactional(readOnly = true)
    public Page<Employee> getAllEmployees(Pageable pageable) {
        logger.info("Fetching employees with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return employeeRepository.findAll(pageable);
    }
    
    /**
     * Get employee by employee number
     */
    @Transactional(readOnly = true)
    public Employee getEmployeeByEmpNo(Integer empNo) {
        logger.info("Fetching employee by empNo: {}", empNo);
        return employeeRepository.findById(empNo)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with empNo: " + empNo));
    }
    
    /**
     * Find employees by first name
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByFirstName(String firstName) {
        logger.info("Fetching employees by first name: {}", firstName);
        return employeeRepository.findByFirstName(firstName);
    }
    
    /**
     * Find employees by last name
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByLastName(String lastName) {
        logger.info("Fetching employees by last name: {}", lastName);
        return employeeRepository.findByLastName(lastName);
    }
    
    /**
     * Find employees by gender
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByGender(Employee.Gender gender) {
        logger.info("Fetching employees by gender: {}", gender);
        return employeeRepository.findByGender(gender);
    }
    
    /**
     * Find employees by hire date range
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByHireDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching employees by hire date range: {} to {}", startDate, endDate);
        return employeeRepository.findByHireDateBetween(startDate, endDate);
    }
    
    /**
     * Find employees by birth date range
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByBirthDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching employees by birth date range: {} to {}", startDate, endDate);
        return employeeRepository.findByBirthDateBetween(startDate, endDate);
    }
    
    /**
     * Search employees by first name (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<Employee> searchEmployeesByFirstName(String firstName) {
        logger.info("Searching employees by first name (containing): {}", firstName);
        return employeeRepository.findByFirstNameContaining(firstName);
    }
    
    /**
     * Search employees by last name (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<Employee> searchEmployeesByLastName(String lastName) {
        logger.info("Searching employees by last name (containing): {}", lastName);
        return employeeRepository.findByLastNameContaining(lastName);
    }
    
    /**
     * Search employees by full name (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<Employee> searchEmployeesByFullName(String fullName) {
        logger.info("Searching employees by full name (containing): {}", fullName);
        return employeeRepository.findByFullNameContaining(fullName);
    }
    
    /**
     * Find employees by hire year
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByHireYear(int year) {
        logger.info("Fetching employees by hire year: {}", year);
        return employeeRepository.findByHireYear(year);
    }
    
    /**
     * Find employees by birth year
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByBirthYear(int year) {
        logger.info("Fetching employees by birth year: {}", year);
        return employeeRepository.findByBirthYear(year);
    }
    
    /**
     * Find employees with a minimum number of years of service
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByMinYearsOfService(int years) {
        logger.info("Fetching employees with at least {} years of service.", years);
        return employeeRepository.findByYearsOfServiceGreaterThanEqual(years);
    }
    
    /**
     * Find employees by employee number range
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByEmpNoRange(Integer startEmpNo, Integer endEmpNo) {
        logger.info("Fetching employees by empNo range: {} to {}", startEmpNo, endEmpNo);
        return employeeRepository.findByEmpNoBetween(startEmpNo, endEmpNo);
    }
    
    /**
     * Get the most recently hired employees
     */
    @Transactional(readOnly = true)
    public List<Employee> getLatestHiredEmployees() {
        logger.info("Fetching latest hired employees.");
        return employeeRepository.findLatestHired();
    }
    
    /**
     * Get the earliest hired employees
     */
    @Transactional(readOnly = true)
    public List<Employee> getEarliestHiredEmployees() {
        logger.info("Fetching earliest hired employees.");
        return employeeRepository.findEarliestHired();
    }
    
    /**
     * Create a new employee
     */
    public Employee createEmployee(Employee employee) {
        logger.info("Creating a new employee with empNo: {}", employee.getEmpNo());
        
        // Validate if employee number already exists
        if (employeeRepository.existsById(employee.getEmpNo())) {
            throw new EmployeeValidationException("Employee with empNo already exists: " + employee.getEmpNo());
        }
        
        Employee savedEmployee = employeeRepository.save(employee);
        logger.info("Employee created successfully with empNo: {}", savedEmployee.getEmpNo());
        return savedEmployee;
    }
    
    /**
     * Update an existing employee's information
     */
    public Employee updateEmployee(Integer empNo, Employee employeeDetails) {
        logger.info("Updating employee with empNo: {}", empNo);
        
        Employee existingEmployee = getEmployeeByEmpNo(empNo);
        
        // Update employee fields
        updateEmployeeFields(existingEmployee, employeeDetails);
        
        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        logger.info("Employee updated successfully with empNo: {}", updatedEmployee.getEmpNo());
        return updatedEmployee;
    }
    
    /**
     * Delete an employee
     */
    public void deleteEmployee(Integer empNo) {
        logger.info("Deleting employee with empNo: {}", empNo);
        
        Employee employee = getEmployeeByEmpNo(empNo);
        employeeRepository.delete(employee);
        
        logger.info("Employee deleted successfully with empNo: {}", empNo);
    }
    
    /**
     * Check if an employee exists by employee number
     */
    @Transactional(readOnly = true)
    public boolean existsByEmpNo(Integer empNo) {
        return employeeRepository.existsById(empNo);
    }
    
    /**
     * Get gender statistics
     */
    @Transactional(readOnly = true)
    public List<Object[]> getGenderStatistics() {
        logger.info("Fetching gender statistics.");
        return employeeRepository.countByGender();
    }
    
    /**
     * Get hire year statistics
     */
    @Transactional(readOnly = true)
    public List<Object[]> getHireYearStatistics() {
        logger.info("Fetching hire year statistics.");
        return employeeRepository.countByHireYear();
    }
    
    /**
     * Update an employee's fields from provided details
     */
    private void updateEmployeeFields(Employee existingEmployee, Employee employeeDetails) {
        existingEmployee.setFirstName(employeeDetails.getFirstName());
        existingEmployee.setLastName(employeeDetails.getLastName());
        existingEmployee.setBirthDate(employeeDetails.getBirthDate());
        existingEmployee.setGender(employeeDetails.getGender());
        existingEmployee.setHireDate(employeeDetails.getHireDate());
    }
} 