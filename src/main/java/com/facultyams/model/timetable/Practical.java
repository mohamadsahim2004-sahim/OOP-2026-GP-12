package com.facultyams.model.timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A practical or laboratory session. */
public final class Practical extends ClassSession {
    public Practical(String id, DayOfWeek day, LocalTime start, LocalTime end, Course course, Lecturer lecturer, Venue venue) {
        super(id, day, start, end, course, lecturer, venue);
    }
    @Override public ClassType getType() { return ClassType.PRACTICAL; }
}
