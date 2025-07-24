package com.userservice.exception;

/**
 * 员工验证异常
 */
public class EmployeeValidationException extends RuntimeException {
    
    public EmployeeValidationException(String message) {
        super(message);
    }
    
    public EmployeeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 