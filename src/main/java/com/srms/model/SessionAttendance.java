package com.srms.model;

/**
 * Pairs a class session with its attendance status.
 */
public record SessionAttendance(
        ClassSession session,
        SessionStatus status
) {}
