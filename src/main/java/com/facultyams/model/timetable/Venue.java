package com.facultyams.model.timetable;

import java.util.Objects;

/** Physical venue for a timetable item. */
public final class Venue {
    private final String name;

    public Venue(String name) {
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isEmpty()) throw new IllegalArgumentException("Venue is required.");
    }

    public String getName() { return name; }
}
