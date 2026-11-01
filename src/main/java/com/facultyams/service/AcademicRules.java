package com.facultyams.service;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.EvaluationComponent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The academic rules of the Lecturer module in ONE place, with no database or UI access, so they
 * can be checked on their own (see LecturerChecks).
 *
 * <p>Every number below was already in the project before this class existed (the old
 * MarksService / AttendanceService, labelled "SRS" in the comments). They are collected here
 * unchanged. They have NOT been confirmed against the official university regulations - there
 * is no settings table in the database - so confirm them and change them here only.
 */
public final class AcademicRules {

    /** Minimum CA percentage (0-100) to sit the final examination. */
    public static final double CA_MIN_PERCENT = 40.0;

    /** Minimum attendance percentage (0-100); approved medical absences count as attended. */
    public static final double ATTENDANCE_MIN_PERCENT = 80.0;

    /** Sessions expected per attendance component (theory / practical); same as AttendanceService. */
    public static final int SESSIONS_PER_COMPONENT = AttendanceService.TOTAL_SESSIONS;

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** The existing attendance rules (15 sessions per component, 80 %) are reused, not copied. */
    private static final AttendanceService ATTENDANCE = new AttendanceService();

    /** One row of the grading scale: overall mark at or above {@code minMark} earns the grade. */
    public record Band(double minMark, String grade, double points) {
    }

    private static final List<Band> BANDS = List.of(
            new Band(85, "A+", 4.0),
            new Band(75, "A", 4.0),
            new Band(70, "A-", 3.7),
            new Band(65, "B+", 3.3),
            new Band(60, "B", 3.0),
            new Band(55, "B-", 2.7),
            new Band(50, "C+", 2.3),
            new Band(45, "C", 2.0),
            new Band(40, "C-", 1.7),
            new Band(35, "D+", 1.3),
            new Band(30, "D", 1.0),
            new Band(0, "E", 0.0));

    private AcademicRules() {
    }

    // ------------------------------------------------------------------ grading scale

    public static List<Band> gradeBands() {
        return BANDS;
    }

    /** The grading-scale row for an overall mark (0-100, already rounded to 2 decimals). */
    public static Band bandFor(double overall) {
        if (Double.isNaN(overall) || overall < 0 || overall > 100) {
            throw new IllegalArgumentException("Overall mark must be between 0 and 100.");
        }
        for (Band band : BANDS) {
            if (overall >= band.minMark()) {
                return band;
            }
        }
        return BANDS.get(BANDS.size() - 1);
    }

    public static String gradeFor(double overall) {
        return bandFor(overall).grade();
    }

    public static double gradePointsFor(double overall) {
        return bandFor(overall).points();
    }

    // ------------------------------------------------------------------ assessment scheme

    /** Outcome of checking a course's evaluation components. */
    public record SchemeCheck(double totalWeight, boolean complete, String problem) {
    }

    public static SchemeCheck checkScheme(List<EvaluationComponent> components) {
        BigDecimal total = BigDecimal.ZERO;
        boolean hasCa = false;
        boolean hasFinal = false;
        for (EvaluationComponent c : components) {
            total = total.add(BigDecimal.valueOf(c.getWeightage()));
            hasCa |= c.isContinuousAssessment();
            hasFinal |= c.isFinalExam();
        }
        double totalWeight = total.setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (components.isEmpty()) {
            return new SchemeCheck(0, false, "No evaluation components are defined for this course.");
        }
        if (total.compareTo(HUNDRED) != 0) {
            return new SchemeCheck(totalWeight, false,
                    "Weightages add up to " + fmt(totalWeight) + "% - they must total exactly 100%.");
        }
        if (!hasCa) {
            return new SchemeCheck(totalWeight, false,
                    "Add at least one CA, assignment or practical component.");
        }
        if (!hasFinal) {
            return new SchemeCheck(totalWeight, false, "Add a final examination component.");
        }
        return new SchemeCheck(totalWeight, true, null);
    }

    /** Rejects NaN / infinite / negative marks and marks above the component's maximum. */
    public static double requireValidMark(EvaluationComponent component, double mark)
            throws ValidationException {
        if (Double.isNaN(mark) || Double.isInfinite(mark)) {
            throw new ValidationException(component.getComponentName() + ": enter a valid number.");
        }
        if (mark < 0) {
            throw new ValidationException(component.getComponentName() + ": marks cannot be negative.");
        }
        if (mark > component.getMaxMarks()) {
            throw new ValidationException(component.getComponentName() + ": " + fmt(mark)
                    + " is above the maximum of " + fmt(component.getMaxMarks()) + ".");
        }
        return BigDecimal.valueOf(mark).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ------------------------------------------------------------------ marks -> percentages

    /**
     * Percentage (0-100, 2 decimals) achieved over the given components, weighted by their
     * weightage: sum(mark / max * weight) / sum(weight) * 100.
     * Returns {@code null} when there are no components or any mark is missing.
     */
    public static Double percentOf(List<EvaluationComponent> components, Map<Integer, Double> marks) {
        if (components.isEmpty()) {
            return null;
        }
        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (EvaluationComponent c : components) {
            Double mark = marks.get(c.getEvalComponentId());
            if (mark == null) {
                return null;
            }
            BigDecimal weight = BigDecimal.valueOf(c.getWeightage());
            earned = earned.add(BigDecimal.valueOf(mark)
                    .divide(BigDecimal.valueOf(c.getMaxMarks()), MC).multiply(weight));
            weights = weights.add(weight);
        }
        return earned.divide(weights, MC).multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static List<EvaluationComponent> caComponents(List<EvaluationComponent> all) {
        List<EvaluationComponent> list = new ArrayList<>();
        for (EvaluationComponent c : all) {
            if (c.isContinuousAssessment()) {
                list.add(c);
            }
        }
        return list;
    }

    public static List<EvaluationComponent> finalComponents(List<EvaluationComponent> all) {
        List<EvaluationComponent> list = new ArrayList<>();
        for (EvaluationComponent c : all) {
            if (c.isFinalExam()) {
                list.add(c);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ attendance + eligibility

    /**
     * Attendance of one student in one course, counted over every attendance component the
     * course has (theory and/or practical). {@code totalSessions} is the number of sessions the
     * course is expected to have, so a student with no rows is "not recorded", never "0 %".
     */
    public record AttendanceInfo(int present, int absent, int approvedMedical, int totalSessions) {

        public static AttendanceInfo none() {
            return new AttendanceInfo(0, 0, 0, 0);
        }

        public boolean recorded() {
            return totalSessions > 0 && present + absent > 0;
        }

        /** Unrounded percentage (capped at 100); 0 when nothing is recorded. */
        public double rawPercent() {
            if (!recorded()) {
                return 0;
            }
            return Math.min(100.0, ATTENDANCE.calculatePercentage(present, approvedMedical, totalSessions));
        }

        public double percent() {
            return BigDecimal.valueOf(rawPercent()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        public boolean meetsMinimum() {
            return recorded() && ATTENDANCE.isEligible(rawPercent());
        }
    }

    public enum Status {
        ELIGIBLE("Eligible"),
        NOT_ELIGIBLE("Not eligible"),
        /** Cannot be decided yet: CA marks incomplete or attendance not recorded. */
        PENDING("Pending");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Eligibility(Status status, Double caPercent, AttendanceInfo attendance, String reason) {
        public boolean eligible() {
            return status == Status.ELIGIBLE;
        }
    }

    /**
     * Eligible for the final examination = CA percentage >= {@link #CA_MIN_PERCENT} AND
     * attendance >= {@link #ATTENDANCE_MIN_PERCENT}. A failed requirement is final
     * (NOT_ELIGIBLE); a missing one (CA incomplete, attendance not recorded) is PENDING.
     */
    public static Eligibility eligibility(Double caPercent, AttendanceInfo attendance) {
        AttendanceInfo att = attendance == null ? AttendanceInfo.none() : attendance;
        List<String> failed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (caPercent == null) {
            missing.add("CA marks incomplete");
        } else if (caPercent < CA_MIN_PERCENT) {
            failed.add("CA " + fmt(caPercent) + "% is below " + fmt(CA_MIN_PERCENT) + "%");
        }

        if (!att.recorded()) {
            missing.add("attendance not recorded");
        } else if (!att.meetsMinimum()) {
            failed.add("attendance " + fmt(att.percent()) + "% is below " + fmt(ATTENDANCE_MIN_PERCENT) + "%");
        }

        if (!failed.isEmpty()) {
            return new Eligibility(Status.NOT_ELIGIBLE, caPercent, att, capitalise(String.join("; ", failed)));
        }
        if (!missing.isEmpty()) {
            return new Eligibility(Status.PENDING, caPercent, att, capitalise(String.join("; ", missing)));
        }
        return new Eligibility(Status.ELIGIBLE, caPercent, att, "CA and attendance requirements met");
    }

    // ------------------------------------------------------------------ final result

    /** A fully calculated course result. */
    public record Result(double caPercent, double finalPercent, double overall, String grade,
                         double gradePoints, Eligibility eligibility) {
    }

    /**
     * Calculates the final result, or returns {@code null} while any component mark is missing.
     * overall = sum(mark / max * weight) over all components (weights total 100).
     */
    public static Result computeResult(List<EvaluationComponent> components,
                                       Map<Integer, Double> marks, AttendanceInfo attendance) {
        Double ca = percentOf(caComponents(components), marks);
        Double fin = percentOf(finalComponents(components), marks);
        Double overall = percentOf(components, marks);
        if (ca == null || fin == null || overall == null) {
            return null;
        }
        Band band = bandFor(overall);
        return new Result(ca, fin, overall, band.grade(), band.points(), eligibility(ca, attendance));
    }

    // ------------------------------------------------------------------ GPA

    public record CreditResult(int credits, double gradePoints) {
    }

    /**
     * Credit-weighted grade point average: sum(gradePoints * credits) / sum(credits), rounded to
     * 2 decimals (half up). {@code null} when there are no credits to average.
     */
    public static Double gpa(Collection<CreditResult> results) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (CreditResult r : results) {
            if (r.credits() <= 0) {
                continue;
            }
            weighted = weighted.add(BigDecimal.valueOf(r.gradePoints()).multiply(BigDecimal.valueOf(r.credits())));
            credits = credits.add(BigDecimal.valueOf(r.credits()));
        }
        if (credits.signum() == 0) {
            return null;
        }
        return weighted.divide(credits, MC).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ------------------------------------------------------------------ formatting

    public static String fmt(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().scale() <= 0
                ? BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString()
                : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
