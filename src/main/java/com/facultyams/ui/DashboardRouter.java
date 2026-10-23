package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.AdminDashboardFrame;
import com.facultyams.util.UIUtil;
import com.facultyams.ui.technical.TechnicalOfficerDashboardFrame;

import javax.swing.JFrame;

/**
 * Single place that decides which dashboard opens after login, and handles logout.
 *
 * INTEGRATION POINT FOR TEAM MEMBERS:
 * replace the PlaceholderDashboardFrame line for your role with your own frame, e.g.
 *     case LECTURER -> new LecturerDashboardFrame(user);
 * Build that frame's content from a subclass of {@link AppShell}; its sidebar Logout button
 * already calls DashboardRouter.logout(...).
 */
public final class DashboardRouter {

    private DashboardRouter() {
    }

    /**
     * Opens the window for the user's role. Each role gets its own {@link AppShell} (same sidebar,
     * header and logout); the shell itself re-checks the session role. If the check fails the session
     * is cleared and the login window is shown again with the reason.
     */
    public static void openDashboard(User user) {
        JFrame frame;
        try {
            if (user == null) {
                throw new IllegalStateException("Please log in to continue.");
            }
            frame = switch (user.getRole()) {
                case ADMIN -> new AdminDashboardFrame();
                case LECTURER -> new PlaceholderDashboardFrame(user, "Lecturer module (Member 2)");
                case TECHNICAL_OFFICER -> new TechnicalOfficerDashboardFrame(user);                case STUDENT -> new PlaceholderDashboardFrame(user, "Undergraduate module (Member 4)");
            };
        } catch (IllegalStateException e) {
            Session.end();
            UIUtil.showError(null, e.getMessage());
            new LoginFrame().setVisible(true);
            return;
        }
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