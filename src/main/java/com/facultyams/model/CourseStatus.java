package com.facultyams.model;

/** Progress status filters for the course overview. */
public enum CourseStatus {
    ALL("All courses"),
    IN_PROGRESS("In progress"),
    PAST("Past"),
    FUTURE("Future");

    private final String label;

    CourseStatus(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
}
