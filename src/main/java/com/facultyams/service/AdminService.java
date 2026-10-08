package com.facultyams.service;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.util.LinkedHashMap;
import java.util.Map;

/** Admin dashboard figures. */
public class AdminService {

    private final UserDAO userDAO;
    private final DepartmentDAO departmentDAO;
    private final CourseDAO courseDAO;

    public AdminService() {
        this(new UserDAO(), new DepartmentDAO(), new CourseDAO());
    }

    public AdminService(UserDAO userDAO, DepartmentDAO departmentDAO, CourseDAO courseDAO) {
        this.userDAO = userDAO;
        this.departmentDAO = departmentDAO;
        this.courseDAO = courseDAO;
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
}