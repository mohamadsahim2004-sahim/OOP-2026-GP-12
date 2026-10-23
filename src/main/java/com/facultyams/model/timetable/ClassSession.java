package com.facultyams.model.timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/** Base immutable model for academic classes and personal calendar events. */
public abstract class ClassSession {
    private final String id;
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;
    private final Course course;
    private final Lecturer lecturer;
    private final Venue venue;

    protected ClassSession(String id, DayOfWeek day, LocalTime start, LocalTime end,
                           Course course, Lecturer lecturer, Venue venue) {
        this.id = Objects.requireNonNull(id, "id");
        this.day = Objects.requireNonNull(day, "day");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        this.course = Objects.requireNonNull(course, "course");
        this.lecturer = Objects.requireNonNull(lecturer, "lecturer");
        this.venue = Objects.requireNonNull(venue, "venue");
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be after start time.");
    }

    public abstract ClassType getType();
    public String getId() { return id; }
    public DayOfWeek getDay() { return day; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public Course getCourse() { return course; }
    public Lecturer getLecturer() { return lecturer; }
    public Venue getVenue() { return venue; }
    public String getDisplayName() { return course.getName(); }
}
