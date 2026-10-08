package com.facultyams.security;

public enum Role {

    ADMIN,
    LECTURER,
    TECHNICAL_OFFICER,
    STUDENT;

    public static Role fromString(String value) {

        if (value == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        return Role.valueOf(
                value.trim()
                        .toUpperCase()
                        .replace(" ", "_")
        );
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isLecturer() {
        return this == LECTURER;
    }

    public boolean isTechnicalOfficer() {
        return this == TECHNICAL_OFFICER;
    }

    public boolean isStudent() {
        return this == STUDENT;
    }
}