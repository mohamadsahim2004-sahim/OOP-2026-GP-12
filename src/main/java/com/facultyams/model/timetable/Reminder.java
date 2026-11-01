package com.facultyams.model.timetable;

import java.time.LocalDateTime;

/** A reminder notification and its read state. */
public final class Reminder {
    private final String sessionId;
    private final String message;
    private final LocalDateTime createdAt;
    private boolean read;

    public Reminder(String message, LocalDateTime createdAt, boolean read) {
        this("", message, createdAt, read);
    }
    public Reminder(String sessionId, String message, LocalDateTime createdAt, boolean read) {
        this.sessionId = sessionId;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }
    public String getSessionId() { return sessionId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void markRead() { read = true; }
}
