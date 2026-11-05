package com.facultyams.model;

/**
 * A course unit (e.g. ICT2132). Belongs to exactly one department and may have
 * a lecturer in charge.
 */
public class Course {

    /** Whether the course has theory sessions, practical sessions or both. */
    public enum CourseType {
        THEORY("Theory"),
        PRACTICAL("Practical"),
        THEORY_AND_PRACTICAL("Theory + Practical");

        private final String displayName;

        CourseType(String displayName) {
            this.displayName = displayName;
        }

        public boolean hasTheory() {
            return this != PRACTICAL;
        }

        public boolean hasPractical() {
            return this != THEORY;
        }

        /**
         * Credits that count as practical. Practical = all credits, Theory = none, Theory + Practical = 1 credit
         * practical (when the course has 2 or more credits) and the rest theory.
         */
        public int practicalCredits(int credits) {
            return switch (this) {
                case THEORY -> 0;
                case PRACTICAL -> credits;
                case THEORY_AND_PRACTICAL -> credits >= 2 ? 1 : 0;
            };
        }

        /** Credits that count as theory (credits minus {@link #practicalCredits}). */
        public int theoryCredits(int credits) {
            return credits - practicalCredits(credits);
        }

        /** Short label for the table: "Theory", "Practical" or "Theory + Practical". */
        public String label() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** 1 theory credit = 1 contact hour per week. */
    public static final int THEORY_HOURS_PER_CREDIT = 1;
    /** 1 practical credit = 2 contact hours per week. */
    public static final int PRACTICAL_HOURS_PER_CREDIT = 2;

    /** Theory hours per week for a course of this type and size (credits x 1). */
    public static int theoryHoursPerWeek(CourseType type, int credits) {
        return type == null ? 0 : type.theoryCredits(credits) * THEORY_HOURS_PER_CREDIT;
    }

    /** Practical hours per week for a course of this type and size (credits x 2). */
    public static int practicalHoursPerWeek(CourseType type, int credits) {
        return type == null ? 0 : type.practicalCredits(credits) * PRACTICAL_HOURS_PER_CREDIT;
    }

    /** Total contact hours per week. Example: 3-credit theory = 3, 2-credit practical = 4. */
    public static int totalHoursPerWeek(CourseType type, int credits) {
        return theoryHoursPerWeek(type, credits) + practicalHoursPerWeek(type, credits);
    }

    /**
     * Human readable breakdown, e.g. "3 h/week" for a pure course or "3 h/week (1 h theory + 2 h practical)"
     * for a Theory + Practical course. Used by the course table and the live preview in the Add Course form.
     */
    public static String hoursSummary(CourseType type, int credits) {
        int total = totalHoursPerWeek(type, credits);
        if (type == CourseType.THEORY_AND_PRACTICAL) {
            return total + " h/week (" + theoryHoursPerWeek(type, credits) + " h theory + "
                    + practicalHoursPerWeek(type, credits) + " h practical)";
        }
        return total + " h/week";
    }

    /** Compact form for the table: "3 h", or "3 h (1T + 2P)" when a course has both theory and practical hours. */
    public static String hoursShort(CourseType type, int credits) {
        int total = totalHoursPerWeek(type, credits);
        if (type == CourseType.THEORY_AND_PRACTICAL) {
            return total + " h (" + theoryHoursPerWeek(type, credits) + "T + "
                    + practicalHoursPerWeek(type, credits) + "P)";
        }
        return total + " h";
    }

    /**
     * The calculation spelled out for the Add / Edit form, e.g. "3 credits x 1 h = 3 h per week",
     * "2 credits x 2 h = 4 h per week" or "1 credit x 2 h practical + 2 credits x 1 h theory = 4 h per week".
     */
    public static String hoursFormula(CourseType type, int credits) {
        if (type == null || credits <= 0) {
            return "";
        }
        int total = totalHoursPerWeek(type, credits);
        int theory = type.theoryCredits(credits);
        int practical = type.practicalCredits(credits);
        String result = " = " + total + " h per week";
        if (practical == 0) {
            return credits + (credits == 1 ? " credit" : " credits") + " x " + THEORY_HOURS_PER_CREDIT + " h theory" + result;
        }
        if (theory == 0) {
            return credits + (credits == 1 ? " credit" : " credits") + " x " + PRACTICAL_HOURS_PER_CREDIT
                    + " h practical" + result;
        }
        return practical + (practical == 1 ? " credit" : " credits") + " x " + PRACTICAL_HOURS_PER_CREDIT
                + " h practical + " + theory + (theory == 1 ? " credit" : " credits") + " x "
                + THEORY_HOURS_PER_CREDIT + " h theory" + result;
    }

    private int courseId;
    private String courseCode;
    private String courseName;
    private int credits;
    private int academicLevel;          // 1 - 4
    private int semester;               // 1 or 2
    private CourseType courseType = CourseType.THEORY_AND_PRACTICAL;
    private int departmentId;
    private String departmentName;      // read-only, filled by DAO joins for display
    private Integer lecturerId;         // null = no lecturer assigned
    private String lecturerName;        // read-only, filled by DAO joins for display

    public Course() {
    }

    public Course(int courseId, String courseCode, String courseName, int credits,
                  int academicLevel, int semester, CourseType courseType, int departmentId) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.academicLevel = academicLevel;
        this.semester = semester;
        this.courseType = courseType;
        this.departmentId = departmentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(int academicLevel) {
        this.academicLevel = academicLevel;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Integer lecturerId) {
        this.lecturerId = lecturerId;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    /** Theory contact hours per week, calculated from credits and course type (not stored). */
    public int getTheoryHoursPerWeek() {
        return theoryHoursPerWeek(courseType, credits);
    }

    /** Practical contact hours per week, calculated from credits and course type (not stored). */
    public int getPracticalHoursPerWeek() {
        return practicalHoursPerWeek(courseType, credits);
    }

    /** Total contact hours per week, calculated from credits and course type (not stored). */
    public int getHoursPerWeek() {
        return totalHoursPerWeek(courseType, credits);
    }

    /** e.g. "3 h/week" or "3 h/week (1 h theory + 2 h practical)". */
    public String getHoursSummary() {
        return hoursSummary(courseType, credits);
    }

    /** e.g. "3 h" or "3 h (1T + 2P)". */
    public String getHoursShort() {
        return hoursShort(courseType, credits);
    }

    /** e.g. "Level 2 Semester 1" */
    public String getSemesterLabel() {
        return "Level " + academicLevel + " Semester " + semester;
    }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}