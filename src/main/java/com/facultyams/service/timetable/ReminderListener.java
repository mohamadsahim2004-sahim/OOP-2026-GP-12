package com.facultyams.service.timetable;

import com.facultyams.model.timetable.Reminder;

/** Receives reminder notifications from the scheduler. */
@FunctionalInterface
public interface ReminderListener {
    void onReminder(Reminder reminder);
}
