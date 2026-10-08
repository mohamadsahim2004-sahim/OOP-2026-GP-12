package com.facultyams.exception;

/**
 * Thrown when user input or business rules are invalid (empty fields, bad email, duplicate username, ...).
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}