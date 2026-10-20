package com.facultyams.service;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.dao.MedicalDAO;

public class AttendanceService {

    public static final int TOTAL_SESSIONS = 15;

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final MedicalDAO medicalDAO = new MedicalDAO();

    public double calculatePercentage(int present, int approvedMedical, int totalSessions) {
        if (totalSessions <= 0) {
            throw new IllegalArgumentException("Total sessions must be greater than 0");
        }
        return 100.0 * (present + approvedMedical) / totalSessions;
    }

    public boolean isEligible(double percentage) {
        return percentage >= 80;
    }

    public double getStudentPercentage(int userId, int componentId) {
        int present = attendanceDAO.countPresent(userId, componentId);
        int approved = medicalDAO.countApproved(userId, componentId);
        return calculatePercentage(present, approved, TOTAL_SESSIONS);
    }

    public boolean isStudentEligible(int userId, int componentId) {
        return isEligible(getStudentPercentage(userId, componentId));
    }
}