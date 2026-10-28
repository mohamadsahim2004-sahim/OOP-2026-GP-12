package com.facultyams.ui;

import com.facultyams.model.Student;
import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.util.UIUtil;

import javax.swing.JFrame;

/**
 * Single place that decides which dashboard opens after login, and handles logout.
 *
 * INTEGRATION POINT: this branch only contains the Undergraduate module, so only the STUDENT case
 * opens a real dashboard. When merging with the other modules, keep the team's router and replace its
 * STUDENT line with {@code new UndergraduateDashboardFrame((Student) user)}.
 */
public final class DashboardRouter {
    private DashboardRouter() {
    }

    /** Opens the window for the user's role; on failure the session is cleared and login is shown again. */
    public static void openDashboard(User user) {
        JFrame frame;
        try {
            if (user == null) {
                throw new IllegalStateException("Please log in to continue.");
            }
            switch (user.getRole()) {
                case STUDENT:
                    frame = new UndergraduateDashboardFrame((Student) user);
                    break;
                default:
                    throw new IllegalStateException("The " + user.getRole().getDisplayName()
                            + " dashboard is delivered by another module and is not part of this build.");
            }
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
