package com.facultyams.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable submission record.
 */
public record Submission(
        String id,
        String assignmentId,
        String studentId,
        List<String> files,
        LocalDateTime submittedAt,
        SubmissionStatus status,
        String grade,
        String feedback
) {}
