package com.facultyams.model;

/** Catalogue entry used by enrollment, materials and medical forms (code, credits, type, schedule, enrolled flag). */
public class CourseOffering {
    private String code;
    private String title;
    private int credits;
    private String department;
    private String semester;
    private String lecturerName;
    private String courseType; // "Core", "Elective"
    private boolean isEnrolled;
    private String scheduleTime;

    public CourseOffering() {
    }

    public CourseOffering(String code, String title, int credits, String department, String semester, 
                  String lecturerName, String courseType, boolean isEnrolled, String scheduleTime) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.department = department;
        this.semester = semester;
        this.lecturerName = lecturerName;
        this.courseType = courseType;
        this.isEnrolled = isEnrolled;
        this.scheduleTime = scheduleTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public boolean isEnrolled() {
        return isEnrolled;
    }

    public void setEnrolled(boolean enrolled) {
        isEnrolled = enrolled;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }
}
