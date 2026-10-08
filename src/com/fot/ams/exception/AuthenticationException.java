package com.fot.ams.exception;

/**
 * Base custom exception for authentication related errors.
 * Demonstrates: Error / Exception Handling mapping per SRS Section 9.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
