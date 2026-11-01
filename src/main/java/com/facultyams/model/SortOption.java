package com.facultyams.model;

import java.util.Comparator;
import java.util.Locale;

/** Supported sorting options for course overview cards. */
public enum SortOption {
    SHORT_NAME("Sort by short name", Comparator.comparing(CourseSummary::code, String.CASE_INSENSITIVE_ORDER)),
    FULL_NAME("Sort by title", Comparator.comparing(CourseSummary::title, String.CASE_INSENSITIVE_ORDER)),
    PROGRESS("Sort by progress", Comparator.comparingInt(CourseSummary::progressPercent).reversed()),
    SEMESTER("Sort by semester", Comparator.comparing(CourseSummary::semester, SortOption::compareSemester));

    private final String label;
    private final Comparator<CourseSummary> comparator;

    SortOption(String label, Comparator<CourseSummary> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() { return label; }
    public Comparator<CourseSummary> comparator() { return comparator; }
    @Override public String toString() { return label; }

    private static int compareSemester(String left, String right) {
        return left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
    }
}
