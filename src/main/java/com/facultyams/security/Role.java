package com.facultyams.security;

/** Roles recognised by the Faculty Academic Management System. */
public enum Role {
    ADMIN,
    LECTURER,
    TECHNICAL_OFFICER,
    STUDENT;

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank.");
        }
        return Role.valueOf(value.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
    }

    /** Converts the value stored in the database into a role. */
    public static Role fromDatabaseValue(String value) {
        return fromString(value);
    }

    /** Human-readable label for UI messages. */
    public String getDisplayName() {
        return switch (this) {
            case ADMIN -> "Administrator";
            case LECTURER -> "Lecturer";
            case TECHNICAL_OFFICER -> "Technical Officer";
            case STUDENT -> "Student";
        };
    }

    public boolean isAdmin() { return this == ADMIN; }
    public boolean isLecturer() { return this == LECTURER; }
    public boolean isTechnicalOfficer() { return this == TECHNICAL_OFFICER; }
    public boolean isStudent() { return this == STUDENT; }
}
