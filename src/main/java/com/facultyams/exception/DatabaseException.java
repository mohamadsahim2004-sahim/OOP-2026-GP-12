package com.facultyams.exception;

/**
 * Thrown when a database operation fails (connection problem, SQL error, constraint violation).
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}