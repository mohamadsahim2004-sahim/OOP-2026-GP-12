package com.facultyams.model;

import java.time.LocalDate;
import java.util.Objects;

public record Deadline(
        String id,
        String courseCode,
        String courseTitle,
        String title,
        DeadlineType type,
        LocalDate dueDate,
        DeadlineTarget navigationTarget
) {
    public Deadline {
        Objects.requireNonNull(id);
        Objects.requireNonNull(courseCode);
        Objects.requireNonNull(courseTitle);
        Objects.requireNonNull(title);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dueDate);
        Objects.requireNonNull(navigationTarget);
    }
}
