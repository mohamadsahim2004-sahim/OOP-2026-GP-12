package com.srms.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Immutable class session record.
 */
public record ClassSession(
        String id,
        String courseCode,
        String courseTitle,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        String type, // "Lecture" / "Practical"
        String lecturer
) {}
