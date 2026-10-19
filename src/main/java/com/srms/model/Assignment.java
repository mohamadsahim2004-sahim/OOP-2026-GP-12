package com.srms.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Course assignment activity with due dates and submission criteria.
 */
public record Assignment(
        String id,
        String title,
        String description,
        LocalDateTime opensAt,
        LocalDateTime dueAt,
        List<String> allowedTypes,
        int maxFileMb,
        int maxFiles,
        boolean allowLate
) implements Activity {}
