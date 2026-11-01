package com.facultyams.model;

import java.time.LocalDate;

/** An absent session of a student that has no medical yet, so one may be submitted for it. */
public record SubmittableSession(int attendanceId, int sessionNo, LocalDate date,
                                 String courseCode, String courseName, String type) {
    @Override
    public String toString() {
        return date + "  |  " + courseCode + " " + type + "  |  session " + sessionNo;
    }
}