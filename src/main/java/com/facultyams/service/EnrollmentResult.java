package com.facultyams.service;

public record EnrollmentResult(boolean success, String message) {
    public static EnrollmentResult success(String message) { return new EnrollmentResult(true, message); }
    public static EnrollmentResult failure(String message) { return new EnrollmentResult(false, message); }
}
