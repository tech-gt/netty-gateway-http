package com.userservice.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.userservice.entity.Employee;
import com.userservice.exception.EmployeeNotFoundException;
import com.userservice.exception.EmployeeValidationException;
import com.userservice.service.EmployeeService;

/**
 * 员工REST控制器
 */
@RestController
@RequestMapping("/employees")
@Validated
@CrossOrigin(origins = "*")
public class EmployeeController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    
    @Autowired
    private EmployeeService employeeService;
    
    /**
     * Get all employees with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmployees(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.info("Request to get all employees with pagination: page={}, size={}", page, size);
        
        try {
            Page<Employee> employeePage = employeeService.getAllEmployees(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("employees", employeePage.getRecords());
            response.put("currentPage", employeePage.getCurrent());
            response.put("totalItems", employeePage.getTotal());
            response.put("totalPages", employeePage.getPages());
            response.put("hasNext", employeePage.hasNext());
            response.put("hasPrevious", employeePage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get employee list.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employee list: " + e.getMessage()));
        }
    }
    
    /**
     * Get employee by employee number
     */
    @GetMapping("/{empNo}")
    public ResponseEntity<Map<String, Object>> getEmployeeByEmpNo(@PathVariable Integer empNo) {
        logger.info("Request to get employee by empNo: {}", empNo);
        
        try {
            Employee employee = employeeService.getEmployeeByEmpNo(empNo);
            return ResponseEntity.ok(createSuccessResponse("Employee found successfully.", employee));
        } catch (EmployeeNotFoundException e) {
            logger.warn("Employee not found for empNo: {}", empNo, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to get employee for empNo: {}", empNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employee: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by first name
     */
    @GetMapping("/firstName/{firstName}")
    public ResponseEntity<Map<String, Object>> getEmployeesByFirstName(@PathVariable String firstName) {
        logger.info("Request to get employees by first name: {}", firstName);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByFirstName(firstName);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by first name: {}", firstName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by last name
     */
    @GetMapping("/lastName/{lastName}")
    public ResponseEntity<Map<String, Object>> getEmployeesByLastName(@PathVariable String lastName) {
        logger.info("Request to get employees by last name: {}", lastName);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByLastName(lastName);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by last name: {}", lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by gender
     */
    @GetMapping("/gender/{gender}")
    public ResponseEntity<Map<String, Object>> getEmployeesByGender(@PathVariable Employee.Gender gender) {
        logger.info("Request to get employees by gender: {}", gender);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByGender(gender);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by gender: {}", gender, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by hire year
     */
    @GetMapping("/hireYear/{year}")
    public ResponseEntity<Map<String, Object>> getEmployeesByHireYear(@PathVariable int year) {
        logger.info("Request to get employees by hire year: {}", year);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByHireYear(year);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by hire year: {}", year, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by hire date range
     */
    @GetMapping("/hireDate")
    public ResponseEntity<Map<String, Object>> getEmployeesByHireDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Request to get employees by hire date range: startDate={}, endDate={}", startDate, endDate);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByHireDateRange(startDate, endDate);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by hire date range.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Search employees (by first name, last name, or full name)
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchEmployees(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String fullName) {
        
        logger.info("Request to search employees: firstName={}, lastName={}, fullName={}", firstName, lastName, fullName);
        
        try {
            List<Employee> employees;
            if (firstName != null && !firstName.trim().isEmpty()) {
                employees = employeeService.searchEmployeesByFirstName(firstName.trim());
            } else if (lastName != null && !lastName.trim().isEmpty()) {
                employees = employeeService.searchEmployeesByLastName(lastName.trim());
            } else if (fullName != null && !fullName.trim().isEmpty()) {
                employees = employeeService.searchEmployeesByFullName(fullName.trim());
            } else {
                employees = employeeService.getAllEmployees();
            }
            
            return ResponseEntity.ok(createSuccessResponse("Search completed successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to search employees.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to search employees: " + e.getMessage()));
        }
    }
    
    /**
     * Get employees by minimum years of service
     */
    @GetMapping("/yearsOfService/{years}")
    public ResponseEntity<Map<String, Object>> getEmployeesByMinYearsOfService(@PathVariable int years) {
        logger.info("Request to get employees by min years of service: {}", years);
        
        try {
            List<Employee> employees = employeeService.getEmployeesByMinYearsOfService(years);
            return ResponseEntity.ok(createSuccessResponse("Employees found successfully.", employees));
        } catch (Exception e) {
            logger.error("Failed to get employees by min years of service: {}", years, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get employees: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new employee
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createEmployee(@Valid @RequestBody Employee employee) {
        logger.info("Request to create employee: empNo={}", employee.getEmpNo());
        
        try {
            Employee savedEmployee = employeeService.createEmployee(employee);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("Employee created successfully.", savedEmployee));
        } catch (EmployeeValidationException e) {
            logger.warn("Employee validation failed on creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create employee.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create employee: " + e.getMessage()));
        }
    }
    
    /**
     * Update an existing employee
     */
    @PutMapping("/{empNo}")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Integer empNo, 
            @Valid @RequestBody Employee employeeDetails) {
        
        logger.info("Request to update employee: empNo={}, firstName={}, lastName={}", empNo, employeeDetails.getFirstName(), employeeDetails.getLastName());
        
        try {
            Employee updatedEmployee = employeeService.updateEmployee(empNo, employeeDetails);
            return ResponseEntity.ok(createSuccessResponse("Employee updated successfully.", updatedEmployee));
        } catch (EmployeeNotFoundException e) {
            logger.warn("Employee not found for update: empNo={}", empNo, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (EmployeeValidationException e) {
            logger.warn("Employee validation failed on update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update employee: empNo={}", empNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update employee: " + e.getMessage()));
        }
    }
    
    /**
     * Delete an employee
     */
    @DeleteMapping("/{empNo}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable Integer empNo) {
        logger.info("Request to delete employee: empNo={}", empNo);
        
        try {
            employeeService.deleteEmployee(empNo);
            return ResponseEntity.ok(createSuccessResponse("Employee deleted successfully.", null));
        } catch (EmployeeNotFoundException e) {
            logger.warn("Employee not found for deletion: empNo={}", empNo, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete employee: empNo={}", empNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete employee: " + e.getMessage()));
        }
    }
    
    /**
     * Check if an employee exists
     */
    @GetMapping("/{empNo}/exists")
    public ResponseEntity<Map<String, Object>> checkEmployeeExists(@PathVariable Integer empNo) {
        logger.info("Request to check if employee exists: empNo={}", empNo);
        
        try {
            boolean exists = employeeService.existsByEmpNo(empNo);
            Map<String, Object> response = createSuccessResponse("Check completed successfully.", null);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to check employee existence: empNo={}", empNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check employee existence: " + e.getMessage()));
        }
    }
    
    /**
     * Get gender statistics
     */
    @GetMapping("/statistics/gender")
    public ResponseEntity<Map<String, Object>> getGenderStatistics() {
        logger.info("Request to get gender statistics.");
        
        try {
            List<Object[]> statistics = employeeService.getGenderStatistics();
            Map<String, Object> result = new HashMap<>();
            for (Object[] stat : statistics) {
                result.put(stat[0].toString(), stat[1]);
            }
            return ResponseEntity.ok(createSuccessResponse("Gender statistics retrieved successfully.", result));
        } catch (Exception e) {
            logger.error("Failed to get gender statistics.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get gender statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get hire year statistics
     */
    @GetMapping("/statistics/hireYear")
    public ResponseEntity<Map<String, Object>> getHireYearStatistics() {
        logger.info("Request to get hire year statistics.");
        
        try {
            List<Object[]> statistics = employeeService.getHireYearStatistics();
            Map<String, Object> result = new HashMap<>();
            for (Object[] stat : statistics) {
                result.put(stat[0].toString(), stat[1]);
            }
            return ResponseEntity.ok(createSuccessResponse("Hire year statistics retrieved successfully.", result));
        } catch (Exception e) {
            logger.error("Failed to get hire year statistics.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get hire year statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("GET /employees/health");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "employee-service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a success response map
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
     * Create an error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
} 