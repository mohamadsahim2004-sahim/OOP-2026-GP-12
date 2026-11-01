package com.facultyams.service.timetable;

/** Background service that monitors enabled timetable reminders. */
public interface ReminderService extends AutoCloseable {
    void addListener(ReminderListener listener);
    void snooze(String sessionId);
    @Override void close();
}
