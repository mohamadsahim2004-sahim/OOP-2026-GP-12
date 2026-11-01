package com.facultyams.model;

import java.time.LocalDate;

/** One line of the attendance sheet for a session: a student and what is already saved for them (if anything). */
public record SheetRow(int userId, String fullName, String registrationNo, String batch,
                       Integer attendanceId, String status, LocalDate date, boolean hasMedical) {
    public boolean isRecorded() {
        return attendanceId != null;
    }
}