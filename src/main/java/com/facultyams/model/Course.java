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

        @Override
        public String toString() {
            return displayName;
        }
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

    /** e.g. "Level 2 Semester 1" */
    public String getSemesterLabel() {
        return "Level " + academicLevel + " Semester " + semester;
    }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}