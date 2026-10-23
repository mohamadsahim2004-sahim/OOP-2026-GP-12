package com.facultyams.model;

public enum EnrollmentStatus {
    ENROLLED("Enrolled"), PENDING("Pending");

    private final String label;
    EnrollmentStatus(String label) { this.label = label; }
    public String label() { return label; }
}
