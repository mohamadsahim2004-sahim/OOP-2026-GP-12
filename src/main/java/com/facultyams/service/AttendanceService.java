package com.facultyams.service;

import java.util.ArrayList;
import java.util.List;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.dao.MedicalDAO;
import com.facultyams.model.AttendanceSummary;

public class AttendanceService {

    public static final int TOTAL_SESSIONS = 15;

    private AttendanceDAO attendanceDAO = new AttendanceDAO();
    private MedicalDAO medicalDAO = new MedicalDAO();

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

    public double getCombinedPercentage(int userId, int theoryId, int practicalId) {
        int present = attendanceDAO.countPresent(userId, theoryId)
                    + attendanceDAO.countPresent(userId, practicalId);
        int approved = medicalDAO.countApproved(userId, theoryId)
                     + medicalDAO.countApproved(userId, practicalId);
        return calculatePercentage(present, approved, TOTAL_SESSIONS * 2);
    }

    public List<AttendanceSummary> getBatchSummary(int componentId) {
        List<AttendanceSummary> list = new ArrayList<>();
        List<Integer> ids = attendanceDAO.getStudentIdsByComponent(componentId);

        for (int id : ids) {
            double percentage = getStudentPercentage(id, componentId);
            boolean eligible = isEligible(percentage);
            list.add(new AttendanceSummary(id, percentage, eligible));
        }
        return list;
    }
    //  how many sessions this student attended (used by the summary page)
    public int getPresentCount(int userId, int componentId) {
        return attendanceDAO.countPresent(userId, componentId);
    }

    // how many absent sessions were covered by an approved medical
    public int getApprovedCount(int userId, int componentId) {
        return medicalDAO.countApproved(userId, componentId);
    }
}