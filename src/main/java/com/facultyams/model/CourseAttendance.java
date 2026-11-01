package com.facultyams.model;

/**
 * Attendance summary for a course, using the project's rule (see AttendanceService):
 * percentage = 100 x (present + approved medical) / (15 sessions x components with records).
 * {@code totalSessions} is that denominator; {@code recorded} is how many sessions have been marked so far.
 */
public record CourseAttendance(
        String courseCode,
        String courseTitle,
        int totalSessions,
        int attended,
        int medicalApproved,
        int recorded
) {
    public double percent() {
        if (totalSessions <= 0) return 0.0;
        return Math.min(100.0, (attended + medicalApproved) * 100.0 / totalSessions);
    }

    public boolean hasRecords() {
        return recorded > 0;
    }

    public int absent() {
        return Math.max(0, recorded - attended - medicalApproved);
    }

    public boolean isEligible() {
        return percent() >= AttendancePolicy.MIN_PERCENT;
    }
}
