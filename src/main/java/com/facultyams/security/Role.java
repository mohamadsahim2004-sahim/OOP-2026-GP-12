package com.facultyams.security;

/**
 * The four user roles of the system. The enum constant names match the
 * values stored in the {@code users.role} column.
 */
public enum Role {
    ADMIN("Admin"),
    LECTURER("Lecturer"),
    TECHNICAL_OFFICER("Technical Officer"),
    STUDENT("Student");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a database value (e.g. "TECHNICAL_OFFICER") to a Role.
     *
     * @throws IllegalArgumentException if the value is not a known role
     */
    public static Role fromDatabaseValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value is null");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }

    @Override
    public String toString() {
        return displayName;
    }
}