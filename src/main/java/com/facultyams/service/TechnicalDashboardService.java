package com.facultyams.service;

import com.facultyams.dao.MedicalDAO;
import com.facultyams.dao.TechnicalDashboardDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.DashboardData;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.MedicalRow;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.util.List;
import java.util.Map;

/** Reads the figures shown on the Technical Officer dashboard (live from the database). */
public class TechnicalDashboardService {

    private static final int RECENT = 8;

    private final TechnicalDashboardDAO dao = new TechnicalDashboardDAO();
    private final MedicalDAO medicalDAO = new MedicalDAO();

    public DashboardData load() throws DatabaseException {
        Session.requireRole(Role.TECHNICAL_OFFICER);
        Map<String, Integer> medicals = medicalDAO.countByStatus();
        List<MedicalRow> pending = medicalDAO.findMedicals(MedicalFilter.none().withStatus(MedicalService.PENDING), RECENT);
        return new DashboardData(dao.countStudents(), dao.countAttendanceRecords(),
                dao.countOnDate(java.time.LocalDate.now(), AttendanceService.PRESENT),
                dao.countOnDate(java.time.LocalDate.now(), AttendanceService.ABSENT),
                dao.countOnDate(java.time.LocalDate.now(), AttendanceService.LATE),
                medicals.get(MedicalService.PENDING), medicals.get(MedicalService.APPROVED),
                medicals.get(MedicalService.REJECTED), dao.recentSessions(RECENT), pending);
    }
}