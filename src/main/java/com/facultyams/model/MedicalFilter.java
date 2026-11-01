package com.facultyams.model;

import java.time.LocalDate;

/** Optional criteria for medical queries; dates apply to the submission date. */
public record MedicalFilter(String student, Integer studentId, Integer courseId, String batch,
                            String status, LocalDate from, LocalDate to) {
    public static MedicalFilter none() {
        return new MedicalFilter(null, null, null, null, null, null, null);
    }

    public MedicalFilter withStatus(String newStatus) {
        return new MedicalFilter(student, studentId, courseId, batch, newStatus, from, to);
    }
}