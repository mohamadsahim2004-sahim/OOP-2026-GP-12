package com.facultyams.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.facultyams.model.CourseSummary;
import com.facultyams.model.CourseStatus;
import com.facultyams.model.SortOption;

/** Immutable combined status, text, and ordering filter for course results. */
public final class CourseFilter {
    private final String searchText;
    private final CourseStatus status;
    private final SortOption sortOption;
    private final String currentSemester;

    public CourseFilter(String searchText, CourseStatus status, SortOption sortOption, String currentSemester) {
        this.searchText = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        this.status = Objects.requireNonNullElse(status, CourseStatus.ALL);
        this.sortOption = Objects.requireNonNullElse(sortOption, SortOption.SHORT_NAME);
        this.currentSemester = currentSemester == null ? "" : currentSemester.trim();
    }

    /** Returns a newly filtered and sorted course list. */
    public List<CourseSummary> apply(List<CourseSummary> courses) {
        List<CourseSummary> result = new ArrayList<>();
        for (CourseSummary course : courses) {
            if (matchesSearch(course) && matchesStatus(course)) result.add(course);
        }
        result.sort(sortOption.comparator());
        return result;
    }

    private boolean matchesSearch(CourseSummary course) {
        if (searchText.isEmpty()) return true;
        return contains(course.code()) || contains(course.title()) || contains(course.lecturerName());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchText);
    }

    private boolean matchesStatus(CourseSummary course) {
        boolean currentSemester = course.semester().equalsIgnoreCase(this.currentSemester);
        return switch (status) {
            case ALL -> true;
            case IN_PROGRESS -> currentSemester && course.progressPercent() > 0 && course.progressPercent() < 100;
            case PAST -> course.progressPercent() >= 100 || isBefore(course.semester(), this.currentSemester);
            case FUTURE -> course.progressPercent() <= 0 || isAfter(course.semester(), this.currentSemester);
        };
    }

    private boolean isBefore(String semester, String current) {
        return compareSemester(semester, current) < 0;
    }

    private boolean isAfter(String semester, String current) {
        return compareSemester(semester, current) > 0;
    }

    private int compareSemester(String first, String second) {
        if (second == null || second.isBlank()) return 0;
        String[] left = first.toLowerCase(Locale.ROOT).split("\\s+");
        String[] right = second.toLowerCase(Locale.ROOT).split("\\s+");
        int yearCompare = left[0].compareTo(right[0]);
        if (yearCompare != 0) return yearCompare;
        return semesterNumber(first).compareTo(semesterNumber(second));
    }

    private Integer semesterNumber(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("semester ii") || normalized.contains("semester 2")) return 2;
        if (normalized.contains("semester i") || normalized.contains("semester 1")) return 1;
        return 0;
    }
}
