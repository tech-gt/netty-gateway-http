package com.userservice.exception;

/**
 * 用户验证异常
 */
public class UserValidationException extends RuntimeException {
    
    public UserValidationException(String message) {
        super(message);
    }
    
    public UserValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 