package com.facultyams.model;

/**
 * Attendance summary of one student in one course component.
 * {@code basis} is the number of sessions the percentage is measured against.
 */
public record StudentSummaryRow(int userId, String fullName, String registrationNo, String batch,
                                int componentId, String courseCode, String courseName, String type,
                                int present, int late, int absent, int approvedMedical, int recorded,
                                int basis, double percentage, boolean eligible) {
}