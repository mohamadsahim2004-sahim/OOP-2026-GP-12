package com.facultyams.model;

/**
 * Weekly or module section in a course.
 */
public record Section(
        String id,
        String courseId,
        String title,
        int orderIndex
) {}
