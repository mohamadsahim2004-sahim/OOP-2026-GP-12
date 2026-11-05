package com.facultyams;

import com.facultyams.model.Course;
import com.facultyams.model.Course.CourseType;

/**
 * Checks the weekly-hours rule used by the admin Course screens:
 * 1 theory credit = 1 hour per week, 1 practical credit = 2 hours per week.
 *
 * <pre>
 *   java -cp target/classes:target/test-classes com.facultyams.CourseHoursChecks
 * </pre>
 * Needs no database.
 */
public final class CourseHoursChecks {

    private static int passed;
    private static int failed;

    private CourseHoursChecks() {
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    public static void main(String[] args) {
        // Theory: credits x 1
        for (int c = 1; c <= 6; c++) {
            check(c + " credit(s) theory = " + c + " h", Course.totalHoursPerWeek(CourseType.THEORY, c) == c);
            check(c + " credit(s) practical = " + (2 * c) + " h", Course.totalHoursPerWeek(CourseType.PRACTICAL, c) == 2 * c);
        }
        check("1 credit theory shows 1 hour", "1 h/week".equals(Course.hoursSummary(CourseType.THEORY, 1)));
        check("1 credit practical shows 2 hours", "2 h/week".equals(Course.hoursSummary(CourseType.PRACTICAL, 1)));
        check("theory has no practical hours", Course.practicalHoursPerWeek(CourseType.THEORY, 3) == 0);
        check("practical has no theory hours", Course.theoryHoursPerWeek(CourseType.PRACTICAL, 3) == 0);

        // Theory + Practical: 1 practical credit, the rest theory (2 credits and above)
        check("3 credit theory+practical = 2 theory + 2 practical = 4 h",
                Course.theoryHoursPerWeek(CourseType.THEORY_AND_PRACTICAL, 3) == 2
                        && Course.practicalHoursPerWeek(CourseType.THEORY_AND_PRACTICAL, 3) == 2
                        && Course.totalHoursPerWeek(CourseType.THEORY_AND_PRACTICAL, 3) == 4);
        check("2 credit theory+practical = 1 + 2 = 3 h", Course.totalHoursPerWeek(CourseType.THEORY_AND_PRACTICAL, 2) == 3);
        check("1 credit theory+practical cannot be split, counts as theory = 1 h",
                Course.totalHoursPerWeek(CourseType.THEORY_AND_PRACTICAL, 1) == 1);
        check("summary of mixed course",
                "4 h/week (2 h theory + 2 h practical)".equals(Course.hoursSummary(CourseType.THEORY_AND_PRACTICAL, 3)));
        check("short form of mixed course", "4 h (2T + 2P)".equals(Course.hoursShort(CourseType.THEORY_AND_PRACTICAL, 3)));
        check("short form of theory course", "3 h".equals(Course.hoursShort(CourseType.THEORY, 3)));

        // Formula text shown in the form
        check("formula theory", "3 credits x 1 h theory = 3 h per week".equals(Course.hoursFormula(CourseType.THEORY, 3)));
        check("formula practical", "1 credit x 2 h practical = 2 h per week".equals(Course.hoursFormula(CourseType.PRACTICAL, 1)));

        // Instance getters follow credits / type when they are edited
        Course course = new Course(1, "ICT2132", "OOP", 2, 2, 1, CourseType.THEORY, 1);
        check("instance: 2 credit theory = 2 h", course.getHoursPerWeek() == 2 && "2 h".equals(course.getHoursShort()));
        course.setCourseType(CourseType.PRACTICAL);
        check("instance: changing type to practical = 4 h", course.getHoursPerWeek() == 4);
        course.setCredits(3);
        check("instance: changing credits to 3 practical = 6 h", course.getHoursPerWeek() == 6);
        check("null type is safe", Course.totalHoursPerWeek(null, 3) == 0);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}