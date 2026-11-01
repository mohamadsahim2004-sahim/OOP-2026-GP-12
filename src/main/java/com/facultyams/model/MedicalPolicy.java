package com.facultyams.model;

/**
 * Medical submission policies.
 */
public final class MedicalPolicy {
    private MedicalPolicy() {}

    public static final int WINDOW_DAYS = 14;     // Only dates within last 14 days
    public static final int MAX_RANGE_DAYS = 7;   // Max 7 consecutive days
    public static final int MAX_FILES = 3;
    public static final int MAX_FILE_MB = 5;
}
