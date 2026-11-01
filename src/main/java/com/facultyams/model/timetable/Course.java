package com.facultyams.model.timetable;

import java.util.Objects;

/** Immutable course identity displayed in a timetable item. */
public final class Course {
    private final String code;
    private final String name;

    public Course(String code, String name) {
        this.code = Objects.requireNonNull(code, "code").trim();
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.code.isEmpty() || this.name.isEmpty()) throw new IllegalArgumentException("Course code and name are required.");
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}
