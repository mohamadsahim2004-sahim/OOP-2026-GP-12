package com.facultyams.service;

import com.facultyams.model.profile.GpaClassifier;
import com.facultyams.util.Constants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the single in-memory source of truth for student result calculations.
 * The data access boundary can be replaced by a repository without changing views.
 */
public final class ResultService {
    public static final int DEGREE_CREDIT_REQUIREMENT = 135;

    public enum SemesterStatus { COMPLETED, IN_PROGRESS, UPCOMING }
    public enum ResultStatus { PASS, FAIL, PROVISIONAL, PENDING }

    /** Immutable result row. Overall semester is numbered from 1 through 8. */
    public record CourseResult(
            String code, String name, int credits, int overallSemester, String assessment,
            int score, int maxScore, String lecturer, ResultStatus status) {
        public BigDecimal gradePoint() {
            return status == ResultStatus.PENDING ? BigDecimal.ZERO : gradePointFor(score);
        }

        public String grade() {
            return status == ResultStatus.PROVISIONAL ? "—" : gradeFor(score);
        }
    }

    /** Immutable summary of a semester. */
    public record SemesterResult(int year, int semester, SemesterStatus status,
                                 List<CourseResult> courses, BigDecimal sgpa, int credits) {
        public SemesterResult {
            courses = List.copyOf(courses);
        }

        public int overallSemester() {
            return (year - 1) * 2 + semester;
        }
    }

    /** GPA and earned-credit values derived from completed course results. */
    public record Summary(BigDecimal cgpa, int earnedCredits, int plannedCredits,
                          int batchRank, int batchSize, String standing,
                          BigDecimal latestCompletedSgpa) { }

    private static final ResultService INSTANCE = new ResultService();
    private final List<SemesterResult> semesters;
    private final GpaClassifier classifier = new GpaClassifier();

    private ResultService() {
        semesters = createSampleSemesters();
    }

    /** Returns the application-wide result service instance. */
    public static ResultService getInstance() {
        return INSTANCE;
    }

    /** Returns the eight semesters in chronological order. */
    public List<SemesterResult> getSemesters() {
        return semesters;
    }

    /** Returns the semesters for a given academic year (1 to 4). */
    public List<SemesterResult> getSemestersForYear(int year) {
        return semesters.stream().filter(s -> s.year() == year).toList();
    }

    /** Returns the calculated overall academic summary. */
    public Summary getSummary() {
        BigDecimal qualityPoints = BigDecimal.ZERO;
        int attemptedCredits = 0;
        int earnedCredits = 0;
        BigDecimal latest = BigDecimal.ZERO;
        for (SemesterResult semester : semesters) {
            if (semester.status() != SemesterStatus.COMPLETED) continue;
            for (CourseResult course : semester.courses()) {
                if (course.status() == ResultStatus.PASS || course.status() == ResultStatus.FAIL) {
                    qualityPoints = qualityPoints.add(course.gradePoint()
                            .multiply(BigDecimal.valueOf(course.credits())));
                    attemptedCredits += course.credits();
                    if (course.status() == ResultStatus.PASS) earnedCredits += course.credits();
                }
            }
            latest = semester.sgpa();
        }
        BigDecimal cgpa = attemptedCredits == 0 ? BigDecimal.ZERO
                : qualityPoints.divide(BigDecimal.valueOf(attemptedCredits), 2, RoundingMode.HALF_UP);
        return new Summary(cgpa, earnedCredits, DEGREE_CREDIT_REQUIREMENT, 12, 120,
                classifier.classify(cgpa.doubleValue()), latest);
    }

    /** Computes a weighted semester GPA, optionally including provisional results. */
    public BigDecimal calculateGpa(List<CourseResult> courses, boolean includeProvisional) {
        BigDecimal qualityPoints = BigDecimal.ZERO;
        int credits = 0;
        for (CourseResult course : courses) {
            if (course.status() == ResultStatus.PENDING) continue;
            if (course.status() == ResultStatus.PROVISIONAL && !includeProvisional) continue;
            qualityPoints = qualityPoints.add(course.gradePoint()
                    .multiply(BigDecimal.valueOf(course.credits())));
            credits += course.credits();
        }
        return credits == 0 ? BigDecimal.ZERO
                : qualityPoints.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP);
    }

    /** Computes the official cumulative GPA through a semester, excluding provisional work by default. */
    public BigDecimal getCumulativeGpaThrough(int overallSemester, boolean includeProvisional) {
        BigDecimal points = BigDecimal.ZERO;
        int credits = 0;
        for (SemesterResult semester : semesters) {
            if (semester.overallSemester() > overallSemester) break;
            if (semester.status() == SemesterStatus.UPCOMING
                    || (semester.status() == SemesterStatus.IN_PROGRESS && !includeProvisional)) continue;
            for (CourseResult course : semester.courses()) {
                if (course.status() == ResultStatus.PENDING
                        || (course.status() == ResultStatus.PROVISIONAL && !includeProvisional)) continue;
                points = points.add(course.gradePoint().multiply(BigDecimal.valueOf(course.credits())));
                credits += course.credits();
            }
        }
        return credits == 0 ? BigDecimal.ZERO
                : points.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP);
    }

    /** Maps percentage marks to the project's configurable 4.0 grade scale. */
    public static BigDecimal gradePointFor(int marks) {
        return GradingScheme.fromMarks(marks).gradePoint();
    }

    /** Maps marks to a letter grade using the same boundaries as grade-point calculations. */
    public static String gradeFor(int marks) {
        return GradingScheme.fromMarks(marks).letter();
    }

    private List<SemesterResult> createSampleSemesters() {
        List<SemesterResult> result = new ArrayList<>();
        result.add(semester(1, 1, SemesterStatus.COMPLETED,
                course("CS101", "Introduction to Programming", 4, 94, "Final", "Dr. A. Fernando"),
                course("MA101", "Engineering Mathematics I", 4, 85, "Final", "Dr. S. Perera"),
                course("PH101", "Applied Physics", 3, 70, "Final", "Dr. N. Silva"),
                course("EN101", "Academic Communication", 3, 70, "Final", "Ms. T. Jayasinghe"),
                course("CS102", "Computer Systems", 4, 70, "Final", "Mr. R. Kumara")));
        result.add(semester(1, 2, SemesterStatus.COMPLETED,
                course("CS103", "Object-Oriented Programming", 4, 70, "Final", "Dr. A. Fernando"),
                course("MA102", "Engineering Mathematics II", 4, 65, "Final", "Dr. S. Perera"),
                course("PH102", "Electronics", 3, 70, "Final", "Dr. N. Silva"),
                course("CS104", "Data Representation", 3, 75, "Final", "Mr. R. Kumara"),
                course("EN102", "Professional Communication", 4, 89, "Final", "Ms. T. Jayasinghe")));
        result.add(semester(2, 1, SemesterStatus.COMPLETED,
                course("CS201", "Object Oriented Programming", 4, 88, "End Sem", "Dr. A. Fernando"),
                course("CS202", "Computer Architecture", 3, 81, "End Sem", "Prof. S. Perera"),
                course("MA201", "Discrete Mathematics", 3, 76, "End Sem", "Dr. N. Rajapakse"),
                course("CS203", "Data Structures", 4, 83, "End Sem", "Mr. R. Kumara"),
                course("IS201", "Information Systems", 3, 78, "End Sem", "Ms. T. Jayasinghe"),
                course("ST201", "Probability and Statistics", 3, 80, "End Sem", "Dr. M. Silva")));
        result.add(semester(2, 2, SemesterStatus.COMPLETED,
                course("CS204", "Database Systems", 4, 85, "End Sem", "Prof. S. Perera"),
                course("CS205", "Operating Systems", 4, 81, "End Sem", "Dr. N. Rajapakse"),
                course("CS206", "Software Engineering", 3, 78, "End Sem", "Mrs. T. Wickramasinghe"),
                course("CS207", "Computer Networks", 3, 74, "End Sem", "Dr. M. Silva"),
                course("MA202", "Numerical Methods", 3, 82, "End Sem", "Dr. S. Perera"),
                course("MG201", "Management Principles", 3, 77, "End Sem", "Dr. H. Mendis")));
        result.add(semester(3, 1, SemesterStatus.IN_PROGRESS,
                provisional("CS301", "Data Structures", 4, 86, "Mid Sem", "Dr. A. Fernando"),
                provisional("CS302", "Database Management Systems", 3, 91, "Mid Sem", "Prof. S. Perera"),
                provisional("CS303", "Operating Systems", 3, 78, "Mid Sem", "Dr. N. Rajapakse"),
                provisional("CS304", "Computer Networks", 3, 85, "Mid Sem", "Dr. M. Silva"),
                provisional("CS305", "Software Engineering", 3, 92, "Mid Sem", "Mrs. T. Wickramasinghe")));
        result.add(semester(3, 2, SemesterStatus.UPCOMING));
        result.add(semester(4, 1, SemesterStatus.UPCOMING));
        result.add(semester(4, 2, SemesterStatus.UPCOMING));
        return List.copyOf(result);
    }

    private SemesterResult semester(int year, int sem, SemesterStatus status, CourseResult... courses) {
        int overallSemester = (year - 1) * 2 + sem;
        List<CourseResult> courseList = java.util.Arrays.stream(courses)
                .map(course -> new CourseResult(course.code(), course.name(), course.credits(), overallSemester,
                        course.assessment(), course.score(), course.maxScore(), course.lecturer(), course.status()))
                .toList();
        BigDecimal sgpa = calculateGpa(courseList, status == SemesterStatus.IN_PROGRESS);
        int credits = courseList.stream()
                .filter(c -> c.status() == ResultStatus.PASS || c.status() == ResultStatus.FAIL
                        || (status == SemesterStatus.IN_PROGRESS && c.status() == ResultStatus.PROVISIONAL))
                .mapToInt(CourseResult::credits).sum();
        return new SemesterResult(year, sem, status, courseList, sgpa, credits);
    }

    private CourseResult course(String code, String name, int credits, int marks, String assessment, String lecturer) {
        return new CourseResult(code, name, credits, 0, assessment, marks, 100, lecturer,
                marks >= 45 ? ResultStatus.PASS : ResultStatus.FAIL);
    }

    private CourseResult provisional(String code, String name, int credits, int marks, String assessment, String lecturer) {
        return new CourseResult(code, name, credits, 0, assessment, marks, 100, lecturer, ResultStatus.PROVISIONAL);
    }
}
