package com.facultyams.model.timetable;

import java.util.Objects;

/** Lecturer associated with an academic class session. */
public final class Lecturer {
    private final String name;

    public Lecturer(String name) {
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isEmpty()) throw new IllegalArgumentException("Lecturer name is required.");
    }

    public String getName() { return name; }
}
