package com.facultyams.exception;

/**
 * Thrown when login fails (wrong username/password or inactive account).
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}