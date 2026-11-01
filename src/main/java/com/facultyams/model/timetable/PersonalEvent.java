package com.facultyams.model.timetable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Student-created event that participates in schedule and clash checks. */
public final class PersonalEvent extends ClassSession {
    private final String note;
    private final boolean reminderEnabled;
    private final LocalDate date;
    private final boolean repeatWeekly;

    public PersonalEvent(String id, String title, DayOfWeek day, LocalTime start, LocalTime end,
                         String venue, String note, boolean reminderEnabled) {
        this(id, title, LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(day)),
                start, end, venue, note, reminderEnabled, true);
    }

    public PersonalEvent(String id, String title, LocalDate date, LocalTime start, LocalTime end,
                         String venue, String note, boolean reminderEnabled, boolean repeatWeekly) {
        super(id, Objects.requireNonNull(date, "date").getDayOfWeek(), start, end, new Course("PERSONAL", title),
                new Lecturer("Personal event"), new Venue(venue));
        this.date = date;
        this.repeatWeekly = repeatWeekly;
        this.note = note == null ? "" : note.trim();
        this.reminderEnabled = reminderEnabled;
    }

    private PersonalEvent(String id, String title, DayOfWeek day, LocalTime start, LocalTime end,
                          String venue, String note, boolean reminderEnabled, boolean legacy) {
        super(id, day, start, end, new Course("PERSONAL", title),
                new Lecturer("Personal event"), new Venue(venue));
        this.note = note == null ? "" : note.trim();
        this.reminderEnabled = reminderEnabled;
        this.date = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(day));
        this.repeatWeekly = true;
    }

    @Override public ClassType getType() { return ClassType.PERSONAL; }
    public String getNote() { return note; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public LocalDate getDate() { return date; }
    public boolean isRepeatWeekly() { return repeatWeekly; }
}
