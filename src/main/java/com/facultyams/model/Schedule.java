package com.facultyams.model;

import java.time.LocalTime;
import java.util.Locale;

/** A weekly course meeting interval. */
public record Schedule(String day, LocalTime start, LocalTime end) {
    public Schedule {
        day = day == null ? "" : day.trim().toLowerCase(Locale.ROOT);
        if (day.length() > 3) day = day.substring(0, 3);
    }

    public boolean overlaps(Schedule other) {
        return other != null
                && day.equalsIgnoreCase(other.day)
                && start.isBefore(other.end)
                && other.start.isBefore(end);
    }

    public static Schedule parse(String value) {
        if (value == null) return null;
        String[] parts = value.trim().split("\\s+", 2);
        if (parts.length != 2) return null;
        String[] times = parts[1].split("\\s*-\\s*");
        if (times.length != 2) return null;
        try {
            return new Schedule(parts[0],
                    LocalTime.parse(times[0]), LocalTime.parse(times[1]));
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
