package com.facultyams.model.student;

import java.time.LocalDateTime;
import java.util.List;

public record StudentSubmissionDetails(
        long id,
        long assignmentId,
        LocalDateTime submittedAt,
        String status,
        String comment,
        Double marks,
        String feedback,
        List<SubmittedFile> files
) {
    public StudentSubmissionDetails {
        comment = comment == null ? "" : comment;
        feedback = feedback == null ? "" : feedback;
        files = List.copyOf(files);
    }

    public record SubmittedFile(String name, String storedPath, long size) {}
}
