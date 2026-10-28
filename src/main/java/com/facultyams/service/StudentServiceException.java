package com.facultyams.service;

/** Unchecked, user-presentable failure raised by the student services (for example "database unavailable"). */
public class StudentServiceException extends RuntimeException {
    public StudentServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public StudentServiceException(String message) {
        super(message);
    }
}
