package com.facultyams.model.student;

import java.time.LocalDateTime;
import java.util.List;

public record StudentAssignment(
        long id,
        int courseId,
        String courseCode,
        String courseName,
        String title,
        String description,
        LocalDateTime dueAt,
        LocalDateTime cutoffAt,
        boolean allowLate,
        Double maxMarks,
        List<String> allowedTypes,
        int maxFileMb,
        int maxFiles,
        String status,
        LocalDateTime submittedAt,
        Double marks,
        String feedback,
        String comment
) {
    public StudentAssignment {
        allowedTypes = List.copyOf(allowedTypes);
        feedback = feedback == null ? "" : feedback;
        comment = comment == null ? "" : comment;
    }

    public boolean hasSubmission() {
        return status != null;
    }
}
