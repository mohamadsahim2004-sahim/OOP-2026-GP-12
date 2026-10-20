package com.facultyams.service;

public class AttendanceService {

    public double calculatePercentage(int present, int approvedMedical, int totalSessions) {
        if (totalSessions <= 0) {
            throw new IllegalArgumentException("Total sessions must be greater than 0");
        }
        double percentage = 100.0 * (present + approvedMedical) / totalSessions;
        return percentage;

    }

    public boolean isEligible(double percentage){
        return percentage >=80;
    }
}