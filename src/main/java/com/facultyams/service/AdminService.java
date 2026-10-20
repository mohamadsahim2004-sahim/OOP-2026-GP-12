package com.facultyams.service;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.NoticeDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin dashboard figures. Every figure is counted by the DAO that owns the table (no SQL here), and
 * every method is Admin-only. The four {@code count...} methods are separate on purpose: the dashboard
 * calls them one by one so that a problem with one table (for example a migration that was not run)
 * does not blank the figures that can still be read.
 */
public class AdminService {

    private final UserDAO userDAO;
    private final DepartmentDAO departmentDAO;
    private final CourseDAO courseDAO;
    private final NoticeDAO noticeDAO;

    public AdminService() {
        this(new UserDAO(), new DepartmentDAO(), new CourseDAO(), new NoticeDAO());
    }

    public AdminService(UserDAO userDAO, DepartmentDAO departmentDAO, CourseDAO courseDAO) {
        this(userDAO, departmentDAO, courseDAO, new NoticeDAO());
    }

    public AdminService(UserDAO userDAO, DepartmentDAO departmentDAO, CourseDAO courseDAO, NoticeDAO noticeDAO) {
        this.userDAO = userDAO;
        this.departmentDAO = departmentDAO;
        this.courseDAO = courseDAO;
        this.noticeDAO = noticeDAO;
    }

    /**
     * Summary counts in display order, e.g. "Students" -> 20.
     */
    public Map<String, Integer> getDashboardStatistics() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        Map<Role, Integer> byRole = userDAO.countByRole();
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("Admins", byRole.get(Role.ADMIN));
        stats.put("Lecturers", byRole.get(Role.LECTURER));
        stats.put("Technical Officers", byRole.get(Role.TECHNICAL_OFFICER));
        stats.put("Students", byRole.get(Role.STUDENT));
        stats.put("Departments", departmentDAO.countAll());
        stats.put("Courses", courseDAO.countAll());
        return stats;
    }

    /** Every user account of every role, active or not. */
    public int countUsers() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return userDAO.countByRole().values().stream().mapToInt(Integer::intValue).sum();
    }

    public int countDepartments() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return departmentDAO.countAll();
    }

    public int countCourses() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return courseDAO.countAll();
    }

    public int countNotices() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return noticeDAO.countAll();
    }
}
