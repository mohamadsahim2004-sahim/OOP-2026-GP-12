package com.facultyams.model;

import java.time.LocalDate;

/** One saved attendance record joined with student and course details (history view). */
public record AttendanceRow(int attendanceId, int userId, String fullName, String registrationNo, String batch,
                            String courseCode, String courseName, String type, int sessionNo,
                            LocalDate date, String status, String medicalStatus) {
}