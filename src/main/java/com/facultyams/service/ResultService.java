package com.facultyams.service;

import com.facultyams.dao.StudentResultDAO;
import com.facultyams.dao.StudentResultDAO.ResultRow;
import com.facultyams.exception.DatabaseException;
import com.facultyams.security.StudentAccess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Results of the LOGGED-IN student, read from the {@code marks} table.
 * <p>
 * Grades, grade points and overall marks are the values the lecturer module stored (MarksService:
 * overall = 40% CA + 60% final, grade table in {@link GradingScheme}). SGPA / CGPA use the project formula
 * sum(gradePoints x credits) / sum(credits) over courses that have marks; the latest marks row per course counts.
 * A course counts as passed when its grade points are above zero and the student was CA-eligible (CA >= 40).
 */
public final class ResultService {

    public enum SemesterStatus { COMPLETED, IN_PROGRESS, UPCOMING }
    public enum ResultStatus { PASS, FAIL, PENDING }

    /** One course line. {@code overall} is null while the result is pending. */
    public record CourseResult(String code, String name, int credits, int overallSemester, String assessment,
                               Double overall, int maxScore, String lecturer, ResultStatus status,
                               String grade, BigDecimal gradePoint, Double caMarks, Double finalMarks,
                               boolean eligible) {
        public int score() {
            return overall == null ? 0 : (int) Math.round(overall);
        }

        public String scoreText() {
            return overall == null ? "—" : String.format(java.util.Locale.ROOT, "%.1f", overall);
        }

        public boolean hasMarks() {
            return overall != null;
        }
    }

    public record SemesterResult(int year, int semester, SemesterStatus status,
                                 List<CourseResult> courses, BigDecimal sgpa, int credits) {
        public SemesterResult {
            courses = List.copyOf(courses);
        }

        public int overallSemester() {
            return (year - 1) * 2 + semester;
        }
    }

    /**
     * {@code plannedCredits} = credits of every course the student has registered for or been graded in;
     * {@code gradedCredits} = credits of the graded courses (the CGPA denominator); {@code earnedCredits} = passed only.
     */
    public record Summary(BigDecimal cgpa, int earnedCredits, int plannedCredits, int gradedCourses,
                          BigDecimal latestSgpa, int gradedCredits) {
        public boolean hasResults() {
            return gradedCourses > 0;
        }
    }

    private static ResultService instance;
    private static int instanceOwner = -1;

    private final int studentId;
    private final StudentResultDAO dao = new StudentResultDAO();

    private ResultService(int studentId) {
        this.studentId = studentId;
    }

    /** The service for the logged-in student (a new one is created when a different student logs in). */
    public static synchronized ResultService getInstance() {
        int me = StudentAccess.currentStudentId();
        if (instance == null || instanceOwner != me) {
            instance = new ResultService(me);
            instanceOwner = me;
        }
        return instance;
    }

    /** Semesters (academic level + semester number) in chronological order, loaded fresh from the database. */
    public List<SemesterResult> getSemesters() {
        StudentAccess.requireOwn(String.valueOf(studentId));
        List<ResultRow> rows;
        try {
            rows = dao.findResults(studentId);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Results could not be loaded. Please try again.", e);
        }
        Map<Integer, List<ResultRow>> byTerm = new TreeMap<>();
        for (ResultRow r : rows) {
            byTerm.computeIfAbsent(r.level() * 10 + r.semester(), k -> new ArrayList<>()).add(r);
        }
        List<SemesterResult> result = new ArrayList<>();
        for (Map.Entry<Integer, List<ResultRow>> e : byTerm.entrySet()) {
            int year = e.getKey() / 10;
            int sem = e.getKey() % 10;
            int overallSemester = (year - 1) * 2 + sem;
            List<CourseResult> courses = new ArrayList<>();
            for (ResultRow r : e.getValue()) {
                courses.add(toCourseResult(r, overallSemester));
            }
            boolean allGraded = courses.stream().allMatch(CourseResult::hasMarks);
            result.add(new SemesterResult(year, sem,
                    allGraded ? SemesterStatus.COMPLETED : SemesterStatus.IN_PROGRESS,
                    courses, calculateGpa(courses), gradedCredits(courses)));
        }
        return result;
    }

    public List<SemesterResult> getSemestersForYear(int year) {
        return getSemesters().stream().filter(s -> s.year() == year).toList();
    }

    /** The most recent graded courses (highest level/semester first), for the dashboard. */
    public List<CourseResult> getRecentResults(int limit) {
        List<CourseResult> graded = new ArrayList<>();
        List<SemesterResult> semesters = getSemesters();
        for (int i = semesters.size() - 1; i >= 0; i--) {
            for (CourseResult c : semesters.get(i).courses()) {
                if (c.hasMarks()) graded.add(c);
            }
        }
        return graded.size() > limit ? graded.subList(0, limit) : graded;
    }

    public Summary getSummary() {
        return summarize(getSemesters());
    }

    /** Summary computed from an already loaded list of semesters (no extra database round trip). */
    public Summary summarize(List<SemesterResult> semesters) {
        BigDecimal points = BigDecimal.ZERO;
        int attempted = 0;
        int earned = 0;
        int planned = 0;
        int graded = 0;
        BigDecimal latest = BigDecimal.ZERO;
        for (SemesterResult s : semesters) {
            boolean anyGraded = false;
            for (CourseResult c : s.courses()) {
                planned += c.credits();
                if (!c.hasMarks()) continue;
                anyGraded = true;
                graded++;
                points = points.add(c.gradePoint().multiply(BigDecimal.valueOf(c.credits())));
                attempted += c.credits();
                if (c.status() == ResultStatus.PASS) earned += c.credits();
            }
            if (anyGraded) latest = s.sgpa();
        }
        BigDecimal cgpa = attempted == 0 ? BigDecimal.ZERO
                : points.divide(BigDecimal.valueOf(attempted), 2, RoundingMode.HALF_UP);
        return new Summary(cgpa, earned, planned, graded, latest, attempted);
    }

    /** sum(gradePoints x credits) / sum(credits) over graded courses; zero when nothing is graded. */
    public BigDecimal calculateGpa(List<CourseResult> courses) {
        BigDecimal points = BigDecimal.ZERO;
        int credits = 0;
        for (CourseResult c : courses) {
            if (!c.hasMarks()) continue;
            points = points.add(c.gradePoint().multiply(BigDecimal.valueOf(c.credits())));
            credits += c.credits();
        }
        return credits == 0 ? BigDecimal.ZERO
                : points.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP);
    }

    /** CGPA through a given overall semester number (1 = level 1 sem 1 ... 8 = level 4 sem 2). */
    public BigDecimal getCumulativeGpaThrough(int overallSemester) {
        List<CourseResult> upTo = new ArrayList<>();
        for (SemesterResult s : getSemesters()) {
            if (s.overallSemester() <= overallSemester) upTo.addAll(s.courses());
        }
        return calculateGpa(upTo);
    }

    private static int gradedCredits(List<CourseResult> courses) {
        return courses.stream().filter(CourseResult::hasMarks).mapToInt(CourseResult::credits).sum();
    }

    private static CourseResult toCourseResult(ResultRow r, int overallSemester) {
        if (!r.hasMarks()) {
            return new CourseResult(r.code(), r.name(), r.credits(), overallSemester, "Awaiting results", null, 100,
                    nameOrDash(r.lecturer()), ResultStatus.PENDING, "—", BigDecimal.ZERO, null, null, true);
        }
        boolean eligible = r.eligible() == null || r.eligible();
        BigDecimal gp = BigDecimal.valueOf(r.gradePoints() == null ? 0.0 : r.gradePoints());
        boolean passed = eligible && gp.signum() > 0;
        String assessment = String.format(java.util.Locale.ROOT, "CA %.0f%% · Final %.0f%%",
                r.caMarks() == null ? 0.0 : r.caMarks(), r.finalMarks() == null ? 0.0 : r.finalMarks());
        return new CourseResult(r.code(), r.name(), r.credits(), overallSemester, assessment, r.overall(), 100,
                nameOrDash(r.lecturer()), passed ? ResultStatus.PASS : ResultStatus.FAIL, r.grade(), gp,
                r.caMarks(), r.finalMarks(), eligible);
    }

    private static String nameOrDash(String s) {
        return s == null || s.isBlank() ? "Not assigned" : s;
    }
}
