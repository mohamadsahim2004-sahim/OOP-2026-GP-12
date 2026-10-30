package com.facultyams.model;

/** Raw per-student, per-component attendance counts straight from the database (no percentage yet). */
public record SummaryCounts(int userId, String fullName, String registrationNo, String batch,
                            int componentId, String courseCode, String courseName, String type,
                            int present, int late, int absent, int approvedMedical, int recorded) {
}