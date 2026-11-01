package com.facultyams.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * One lecture slot of the timetable (table {@code timetables}): a course on a weekday between two
 * times in a venue. The slot's department and semester are not stored on the slot itself - they are
 * those of its course, and are filled (read-only) by the DAO's joins for display and filtering.
 */
public class TimeTable {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private int timetableId;
    private int courseId;
    private DayOfWeek dayOfWeek;        // MONDAY..SATURDAY (the table has no Sunday)
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;

    // read-only, filled by DAO joins with courses / departments
    private String courseCode;
    private String courseName;
    private int academicLevel;
    private int semester;
    private int departmentId;
    private String departmentName;

    public TimeTable() {
    }

    public TimeTable(int timetableId, int courseId, DayOfWeek dayOfWeek, LocalTime startTime,
                     LocalTime endTime, String venue) {
        this.timetableId = timetableId;
        this.courseId = courseId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
    }

    public int getTimetableId() {
        return timetableId;
    }

    public void setTimetableId(int timetableId) {
        this.timetableId = timetableId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
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

    /** e.g. "Monday". */
    public String getDayName() {
        return dayOfWeek == null ? "" : dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /** e.g. "08:00 - 10:00". */
    public String getTimeRange() {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(HOUR_MINUTE) + " - " + endTime.format(HOUR_MINUTE);
    }

    /** e.g. "Monday 08:00 - 10:00 ICT2132 (LT-01)" - used in confirmation and clash messages. */
    @Override
    public String toString() {
        String course = courseCode == null ? "course #" + courseId : courseCode;
        return getDayName() + " " + getTimeRange() + " " + course + " (" + venue + ")";
    }
}