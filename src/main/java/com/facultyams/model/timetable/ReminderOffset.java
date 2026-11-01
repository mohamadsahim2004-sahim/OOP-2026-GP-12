package com.facultyams.model.timetable;

/** Supported reminder lead times. */
public enum ReminderOffset {
    FIVE(5), TEN(10), FIFTEEN(15), THIRTY(30);

    private final int minutes;

    ReminderOffset(int minutes) { this.minutes = minutes; }

    public int getMinutes() { return minutes; }

    @Override
    public String toString() { return minutes + " minutes"; }
}
