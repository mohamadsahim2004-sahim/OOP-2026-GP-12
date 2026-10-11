package com.facultyams.model;

public class Attendance {
    private String courseCode;
    private String courseName;
    private int totalSessions;
    private int attendedSessions;
    private double percentage;
    private double requiredPercentage;
    private String lastAttendedDate;

    public Attendance() {
        this.requiredPercentage = 80.0;
    }

    public Attendance(String courseCode, String courseName, int totalSessions, 
                      int attendedSessions, String lastAttendedDate) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.totalSessions = totalSessions;
        this.attendedSessions = attendedSessions;
        this.percentage = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100.0 : 0.0;
        this.requiredPercentage = 80.0;
        this.lastAttendedDate = lastAttendedDate;
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

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
        calculatePercentage();
    }

    public int getAttendedSessions() {
        return attendedSessions;
    }

    public void setAttendedSessions(int attendedSessions) {
        this.attendedSessions = attendedSessions;
        calculatePercentage();
    }

    public int getAbsentSessions() {
        return Math.max(0, totalSessions - attendedSessions);
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getRequiredPercentage() {
        return requiredPercentage;
    }

    public void setRequiredPercentage(double requiredPercentage) {
        this.requiredPercentage = requiredPercentage;
    }

    public String getLastAttendedDate() {
        return lastAttendedDate;
    }

    public void setLastAttendedDate(String lastAttendedDate) {
        this.lastAttendedDate = lastAttendedDate;
    }

    public boolean isEligible() {
        return percentage >= requiredPercentage;
    }

    private void calculatePercentage() {
        if (totalSessions > 0) {
            this.percentage = Math.round(((double) attendedSessions / totalSessions * 100.0) * 10.0) / 10.0;
        } else {
            this.percentage = 0.0;
        }
    }
}
