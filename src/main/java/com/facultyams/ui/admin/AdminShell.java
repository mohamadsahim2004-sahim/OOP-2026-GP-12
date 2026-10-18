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

    public AdminShell(JFrame owner, User user) {
        super(owner, user, Role.ADMIN);
        init();
    }

    @Override
    protected void registerPages() {
        addPage("dashboard", "Dashboard", "Admin Dashboard", new DashboardPanel(this::showPage));
        addPage("users", "User Management", "User Management", new UserManagementPanel());
        addPage("departments", "Department Management", "Department Management", new DepartmentManagementPanel());
        addPage("courses", "Course Management", "Course Management", new CourseManagementPanel());
        addPage("notices", "Notices", "Notice Management", new NoticeManagementPanel());
        addPage("timetables", "Timetables", "Timetable Management", new TimeTableManagementPanel());
        addPage("reports", "Reports", "Reports", new ReportsPanel());
        addPage("profile", "Profile", "My Profile", new ProfilePanel(this::refreshUserLabels));
        addPage("settings", "System Settings", "System Settings", new SettingsPanel());
    }
}