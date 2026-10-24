package com.facultyams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable medical excuse request record.
 */
public record MedicalRequest(
        String id,
        String referenceNo,
        String studentId,
        LocalDate from,
        LocalDate to,
        List<String> sessionIds,
        String reason,
        String doctor,
        String certificateNo,
        String notes,
        List<String> files,
        LocalDateTime submittedAt,
        MedicalStatus status,
        String reviewerRemarks
) {
    public boolean isPending() {
        return status == MedicalStatus.PENDING;
    }
}
