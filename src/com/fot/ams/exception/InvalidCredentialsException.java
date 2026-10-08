package com.fot.ams.exception;

/**
 * Thrown when user credentials fail verification.
 * Implements FR-AUTH-04: Display appropriate error message for invalid credentials
 * without revealing which specific field is incorrect.
 */
public class InvalidCredentialsException extends AuthenticationException {
    public static final String DEFAULT_MESSAGE = "Invalid username or password. Please try again.";

    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
