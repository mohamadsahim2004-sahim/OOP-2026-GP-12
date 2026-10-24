package com.facultyams.model;

/**
 * Course announcement activity.
 */
public record Announcement(
        String id,
        String title
) implements Activity {}
