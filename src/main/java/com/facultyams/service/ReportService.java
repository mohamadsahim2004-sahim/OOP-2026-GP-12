package com.facultyams.service;

import com.facultyams.dao.ReportDAO;
import com.facultyams.dao.ReportDAO.DaySummary;
import com.facultyams.dao.ReportDAO.DepartmentSummary;
import com.facultyams.dao.ReportDAO.RoleSummary;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.util.List;

/** Admin-only reports. Every method checks the role first. */
public class ReportService {

    private final ReportDAO reportDAO;

    public ReportService() {
        this(new ReportDAO());
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public List<RoleSummary> getUsersByRole() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return reportDAO.usersByRole();
    }

    public List<DepartmentSummary> getDepartmentOverview() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return reportDAO.departmentOverview();
    }

    public List<DaySummary> getTimetableLoad() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return reportDAO.timetableLoad();
    }

    /** All three reports as one CSV text (sections separated by a blank line). */
    public String exportCsv() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        StringBuilder csv = new StringBuilder("Users by role\nRole,Total,Active,Inactive\n");
        for (RoleSummary r : reportDAO.usersByRole()) {
            csv.append(r.role()).append(',').append(r.total()).append(',').append(r.active()).append(',')
                    .append(r.total() - r.active()).append('\n');
        }
        csv.append("\nDepartments\nCode,Name,Courses,Users\n");
        for (DepartmentSummary d : reportDAO.departmentOverview()) {
            csv.append(escape(d.code())).append(',').append(escape(d.name())).append(',')
                    .append(d.courses()).append(',').append(d.users()).append('\n');
        }
        csv.append("\nTimetable load\nDay,Slots\n");
        for (DaySummary d : reportDAO.timetableLoad()) {
            csv.append(d.day()).append(',').append(d.slots()).append('\n');
        }
        return csv.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}