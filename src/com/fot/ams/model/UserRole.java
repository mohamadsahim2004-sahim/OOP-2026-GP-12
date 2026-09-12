package com.fot.ams.model;

/**
 * UserRole represents the distinct user roles defined in the SRS
 * for the Faculty of Technology Academic Management System (University of Ruhuna).
 */
public enum UserRole {
    ADMIN("Administrator", "Full system privileges & user management"),
    LECTURER("Lecturer", "Course materials & marks management"),
    TECHNICAL_OFFICER("Technical Officer", "Attendance & medical record management"),
    UNDERGRADUATE("Undergraduate", "View grades, attendance, and enrolled courses");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
