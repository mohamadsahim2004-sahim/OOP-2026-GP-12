package com.facultyams.exception;

<<<<<<< HEAD
/**
 * Thrown when a database operation fails (connection problem, SQL error, constraint violation).
 */
public class DatabaseException extends Exception {
=======
public class DatabaseException extends RuntimeException {
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}