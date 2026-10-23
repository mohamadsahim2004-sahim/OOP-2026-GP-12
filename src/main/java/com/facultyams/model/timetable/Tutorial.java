package com.facultyams.model.timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A tutorial session. */
public final class Tutorial extends ClassSession {
    public Tutorial(String id, DayOfWeek day, LocalTime start, LocalTime end, Course course, Lecturer lecturer, Venue venue) {
        super(id, day, start, end, course, lecturer, venue);
    }
    @Override public ClassType getType() { return ClassType.TUTORIAL; }
}
