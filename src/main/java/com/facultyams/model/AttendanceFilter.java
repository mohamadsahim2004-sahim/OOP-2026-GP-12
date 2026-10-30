package com.facultyams.model;

import java.time.LocalDate;

/** Optional criteria for attendance queries; a null (or blank) field means "no restriction". */
public record AttendanceFilter(String student, Integer studentId, Integer courseId, Integer componentId,
                               String batch, LocalDate from, LocalDate to, String status) {
    public static AttendanceFilter none() {
        return new AttendanceFilter(null, null, null, null, null, null, null, null);
    }

    public boolean hasDateRange() {
        return from != null || to != null;
    }
}