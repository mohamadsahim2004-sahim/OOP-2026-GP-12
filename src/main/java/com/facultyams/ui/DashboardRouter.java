package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.AdminDashboardFrame;

import javax.swing.JFrame;

/**
 * Single place that decides which dashboard opens after login, and handles logout.
 *
 * INTEGRATION POINT FOR TEAM MEMBERS:
 * replace the PlaceholderDashboardFrame line for your role with your own frame, e.g.
 *     case LECTURER -> new LecturerDashboardFrame(user);
 * Your frame's logout button should call DashboardRouter.logout(this).
 */
public final class DashboardRouter {

    private DashboardRouter() {
    }

    public static void openDashboard(User user) {
        JFrame frame = switch (user.getRole()) {
            case ADMIN -> new AdminDashboardFrame(user);
            case LECTURER -> new PlaceholderDashboardFrame(user, "Lecturer module (Member 2)");
            case TECHNICAL_OFFICER -> new PlaceholderDashboardFrame(user, "Technical Officer module (Member 3)");
            case STUDENT -> new PlaceholderDashboardFrame(user, "Undergraduate module (Member 4)");
        };
        frame.setVisible(true);
    }

    /** Ends the session, closes the current window and shows the login screen again. */
    public static void logout(JFrame currentFrame) {
        Session.end();
        if (currentFrame != null) {
            currentFrame.dispose();
        }
        new LoginFrame().setVisible(true);
    }
}