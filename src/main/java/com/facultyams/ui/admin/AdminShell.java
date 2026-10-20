package com.facultyams.ui.admin;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.ui.AppShell;

import javax.swing.JFrame;

/**
 * The Admin window's content: the shared {@link AppShell} (sidebar, header, logout, role check)
 * plus the nine administrator pages.
 */
public final class AdminShell extends AppShell {

    private static final long serialVersionUID = 1L;

    /** Page keys, shared with the dashboard's quick actions. */
    public static final String PAGE_DASHBOARD = "dashboard";
    public static final String PAGE_USERS = "users";
    public static final String PAGE_DEPARTMENTS = "departments";
    public static final String PAGE_COURSES = "courses";
    public static final String PAGE_NOTICES = "notices";
    public static final String PAGE_TIMETABLES = "timetables";
    public static final String PAGE_REPORTS = "reports";
    public static final String PAGE_PROFILE = "profile";
    public static final String PAGE_SETTINGS = "settings";

    public AdminShell(JFrame owner, User user) {
        super(owner, user, Role.ADMIN);
        init();
    }

    @Override
    protected void registerPages() {
        addPage(PAGE_DASHBOARD, "Dashboard", "Admin Dashboard", new DashboardPanel(this::showPage));
        addPage(PAGE_USERS, "User Management", "User Management", new UserManagementPanel());
        addPage(PAGE_DEPARTMENTS, "Department Management", "Department Management", new DepartmentManagementPanel());
        addPage(PAGE_COURSES, "Course Management", "Course Management", new CourseManagementPanel());
        addPage(PAGE_NOTICES, "Notices", "Notice Management", new NoticeManagementPanel());
        addPage(PAGE_TIMETABLES, "Timetables", "Timetable Management", new TimeTableManagementPanel());
        addPage(PAGE_REPORTS, "Reports", "Reports", new ReportsPanel());
        addPage(PAGE_PROFILE, "Profile", "My Profile", new ProfilePanel(this::refreshUserLabels, this::requestLogout));
        addPage(PAGE_SETTINGS, "System Settings", "System Settings", new SettingsPanel());
    }
}