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
        String comment,
        Integer weekNo,
        LocalDateTime openedAt
) {
    public StudentAssignment(long id, int courseId, String courseCode, String courseName, String title,
                             String description, LocalDateTime dueAt, LocalDateTime cutoffAt,
                             boolean allowLate, Double maxMarks, List<String> allowedTypes,
                             int maxFileMb, int maxFiles, String status, LocalDateTime submittedAt,
                             Double marks, String feedback, String comment) {
        this(id, courseId, courseCode, courseName, title, description, dueAt, cutoffAt, allowLate,
                maxMarks, allowedTypes, maxFileMb, maxFiles, status, submittedAt, marks, feedback,
                comment, null, null);
    }

    public StudentAssignment {
        allowedTypes = List.copyOf(allowedTypes);
        feedback = feedback == null ? "" : feedback;
        comment = comment == null ? "" : comment;
    }

    public boolean hasSubmission() {
        return status != null;
    }
}
