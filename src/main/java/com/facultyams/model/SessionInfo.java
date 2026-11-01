package com.facultyams.model;

import java.time.LocalDate;

/** What the medical rules need to know about one saved attendance record. */
public record SessionInfo(int attendanceId, int userId, String status, LocalDate date, boolean hasMedical) {
}