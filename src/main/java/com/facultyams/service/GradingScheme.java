package com.facultyams.service;

import com.facultyams.util.Constants;
import java.math.BigDecimal;
import java.util.List;

/** Central, data-driven marks-to-grade mapping for the SRMS 4.0 scale. */
public final class GradingScheme {
    public record GradeBand(String letter, int minimumMark, int maximumMark,
                            BigDecimal gradePoint, boolean passing) { }

    /** Same table MarksService uses when lecturers save marks (overall mark = 40% CA + 60% final). */
    private static final List<GradeBand> BANDS = List.of(
            new GradeBand("A+", 85, 100, new BigDecimal("4.0"), true),
            new GradeBand("A", 75, 84, new BigDecimal("4.0"), true),
            new GradeBand("A-", 70, 74, new BigDecimal("3.7"), true),
            new GradeBand("B+", 65, 69, new BigDecimal("3.3"), true),
            new GradeBand("B", 60, 64, new BigDecimal("3.0"), true),
            new GradeBand("B-", 55, 59, new BigDecimal("2.7"), true),
            new GradeBand("C+", 50, 54, new BigDecimal("2.3"), true),
            new GradeBand("C", 45, 49, new BigDecimal("2.0"), true),
            new GradeBand("C-", 40, 44, new BigDecimal("1.7"), true),
            new GradeBand("D+", 35, 39, new BigDecimal("1.3"), true),
            new GradeBand("D", 30, 34, new BigDecimal("1.0"), true),
            new GradeBand("E", 0, 29, BigDecimal.ZERO, false));

    private GradingScheme() { }

    /** Returns grade band for marks within the inclusive 0 to 100 range. */
    public static GradeBand fromMarks(int marks) {
        if (marks < 0 || marks > 100) throw new IllegalArgumentException("Marks must be between 0 and 100.");
        return BANDS.stream().filter(band -> marks >= band.minimumMark() && marks <= band.maximumMark())
                .findFirst().orElseThrow(() -> new IllegalStateException("No grade configured for marks: " + marks));
    }

    /** Returns the grade point associated with a letter grade. */
    public static BigDecimal gradePoint(String letter) {
        return BANDS.stream().filter(band -> band.letter().equals(letter)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown grade: " + letter)).gradePoint();
    }

    /** Returns the configured grade bands in descending grade order. */
    public static List<GradeBand> bands() {
        return BANDS;
    }

    /** Returns the maximum configured grade-point value. */
    public static BigDecimal maximumGpa() {
        return BigDecimal.valueOf(Constants.MAX_GPA);
    }
}
