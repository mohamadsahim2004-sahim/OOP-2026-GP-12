package com.facultyams.exception;

/**
 * Thrown when the logged-in user is not allowed to perform an action.
 */
public class AuthorizationException extends Exception {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}