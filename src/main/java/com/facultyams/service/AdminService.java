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

import com.facultyams.model.Notice;
import com.facultyams.model.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

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
        /** One line of the dashboard's "Recent Activities" list. */
    public record Activity(String text, LocalDateTime when) {
    }

    /** Users per role (every role present, 0 if none) for the dashboard's overview chart. */
    public Map<Role, Integer> countUsersByRole() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            counts.put(role, 0);
        }
        counts.putAll(userDAO.countByRole());
        return counts;
    }

    /**
     * The newest events the database can tell us about, newest first: accounts that were created (users.created_at)
     * and notices that were posted (notices.notice_date). Nothing is invented; the schema keeps no other history.
     */
    public List<Activity> getRecentActivity(int limit) throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        List<Activity> events = new ArrayList<>();
        for (User u : userDAO.findAll()) {
            if (u.getCreatedAt() != null) {
                events.add(new Activity("New user registered - " + u.getFullName() + " ("
                        + u.getRole().getDisplayName() + ")", u.getCreatedAt()));
            }
        }
        for (Notice n : noticeDAO.findAll()) {
            events.add(new Activity("Notice posted - " + n.getTitle(), n.getNoticeDate().atStartOfDay()));
        }
        events.sort(Comparator.comparing(Activity::when).reversed());
        return events.size() > limit ? new ArrayList<>(events.subList(0, limit)) : events;
    }
}
