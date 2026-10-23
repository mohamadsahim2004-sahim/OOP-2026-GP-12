package com.srms.model;

import java.util.Comparator;
import java.util.Locale;

/** Supported sorting options for course overview cards. */
public enum SortOption {
    SHORT_NAME("Sort by short name", Comparator.comparing(Course::code, String.CASE_INSENSITIVE_ORDER)),
    FULL_NAME("Sort by title", Comparator.comparing(Course::title, String.CASE_INSENSITIVE_ORDER)),
    PROGRESS("Sort by progress", Comparator.comparingInt(Course::progressPercent).reversed()),
    SEMESTER("Sort by semester", Comparator.comparing(Course::semester, SortOption::compareSemester));

    private final String label;
    private final Comparator<Course> comparator;

    SortOption(String label, Comparator<Course> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() { return label; }
    public Comparator<Course> comparator() { return comparator; }
    @Override public String toString() { return label; }

    private static int compareSemester(String left, String right) {
        return left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
    }
}
