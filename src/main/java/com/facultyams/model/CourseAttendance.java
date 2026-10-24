package com.facultyams.model;

/**
 * Summary of attendance for a course.
 * Formula: attended / (totalSessions - medicalApproved) * 100.0
 */
public record CourseAttendance(
        String courseCode,
        String courseTitle,
        int totalSessions,
        int attended,
        int medicalApproved
) {
    public double percent() {
        int effectiveTotal = totalSessions - medicalApproved;
        if (effectiveTotal <= 0) return 100.0;
        return (attended * 100.0) / effectiveTotal;
    }

    public int absent() {
        return Math.max(0, totalSessions - attended - medicalApproved);
    }

    public boolean isEligible() {
        return percent() >= AttendancePolicy.MIN_PERCENT;
    }
}
