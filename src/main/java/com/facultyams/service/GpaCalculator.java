package com.facultyams.service;

import com.facultyams.model.results.LetterGrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Validates GPA inputs and performs weighted GPA calculations. */
public final class GpaCalculator {
    public record CourseInput(String name, int credits, LetterGrade grade) { }
    public record Calculation(BigDecimal semesterGpa, BigDecimal projectedCgpa, int semesterCredits,
                              int totalCredits, BigDecimal change, BigDecimal qualityPoints) { }

    /** Calculates semester and projected GPA, rejecting invalid values explicitly. */
    public Calculation calculate(List<CourseInput> courses, BigDecimal currentCgpa, int creditsCompleted) {
        if (currentCgpa == null || currentCgpa.compareTo(BigDecimal.ZERO) < 0
                || currentCgpa.compareTo(BigDecimal.valueOf(4)) > 0) {
            throw new IllegalArgumentException("Current CGPA must be between 0.00 and 4.00.");
        }
        if (creditsCompleted < 0) {
            throw new IllegalArgumentException("Credits completed cannot be negative.");
        }
        BigDecimal qualityPoints = BigDecimal.ZERO;
        int semesterCredits = 0;
        for (int i = 0; i < courses.size(); i++) {
            CourseInput course = courses.get(i);
            if (course.name() == null || course.name().isBlank()) {
                throw new IllegalArgumentException("Course " + (i + 1) + ": enter a course name.");
            }
            if (course.credits() <= 0) {
                throw new IllegalArgumentException("Course " + (i + 1) + ": credits must be greater than zero.");
            }
            if (course.grade() == null) {
                throw new IllegalArgumentException("Course " + (i + 1) + ": select a grade.");
            }
            semesterCredits += course.credits();
            qualityPoints = qualityPoints.add(course.grade().points()
                    .multiply(BigDecimal.valueOf(course.credits())));
        }
        BigDecimal semesterGpa = semesterCredits == 0 ? BigDecimal.ZERO
                : qualityPoints.divide(BigDecimal.valueOf(semesterCredits), 2, RoundingMode.HALF_UP);
        int totalCredits = creditsCompleted + semesterCredits;
        BigDecimal projected = totalCredits == 0 ? currentCgpa
                : currentCgpa.multiply(BigDecimal.valueOf(creditsCompleted)).add(qualityPoints)
                .divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
        return new Calculation(semesterGpa, projected, semesterCredits, totalCredits,
                projected.subtract(currentCgpa).setScale(2, RoundingMode.HALF_UP), qualityPoints);
    }

    /** Calculates the SGPA for entered courses only. */
    public BigDecimal semesterGpa(List<CourseInput> courses) {
        return calculate(courses, BigDecimal.ZERO, 0).semesterGpa();
    }

    /** Calculates the average GPA required across remaining credits to reach a target. */
    public BigDecimal requiredAverage(BigDecimal currentCgpa, int creditsCompleted,
                                      BigDecimal targetCgpa, int remainingCredits) {
        if (currentCgpa == null || targetCgpa == null
                || currentCgpa.compareTo(BigDecimal.ZERO) < 0 || currentCgpa.compareTo(BigDecimal.valueOf(4)) > 0
                || targetCgpa.compareTo(BigDecimal.ZERO) < 0 || targetCgpa.compareTo(BigDecimal.valueOf(4)) > 0) {
            throw new IllegalArgumentException("GPA values must be between 0.00 and 4.00.");
        }
        if (creditsCompleted < 0 || remainingCredits <= 0) {
            throw new IllegalArgumentException("Enter completed credits and a positive number of remaining credits.");
        }
        BigDecimal targetPoints = targetCgpa.multiply(BigDecimal.valueOf(creditsCompleted + remainingCredits));
        BigDecimal currentPoints = currentCgpa.multiply(BigDecimal.valueOf(creditsCompleted));
        return targetPoints.subtract(currentPoints)
                .divide(BigDecimal.valueOf(remainingCredits), 2, RoundingMode.HALF_UP);
    }
}
