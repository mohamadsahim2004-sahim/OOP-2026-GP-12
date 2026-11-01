package com.facultyams.model;

public class AttendanceSummary {
    private int userId;
    private double percentage;
    private boolean eligible;

    public AttendanceSummary(int userId, double percentage, boolean eligible) {
        this.userId=userId;
        this.percentage=percentage;
        this.eligible=eligible;
    }

    public int getUserId() { return userId; }
    public double getPercentage() { return percentage; }
    public boolean isEligible() { return eligible; }
}