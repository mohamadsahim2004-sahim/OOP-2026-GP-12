package com.facultyams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One medical submission joined with the student, course and session it covers. */
public record MedicalRow(int medicalId, int attendanceId, int studentId, String studentName, String registrationNo,
                        String batch, String courseCode, String courseName, String type, int sessionNo,
                        LocalDate sessionDate, String sessionStatus, LocalDate submittedDate, String reason,
                        String evidenceName, boolean hasEvidence, String status, String reviewerName,
                        LocalDateTime decidedAt, String rejectionReason) {
}