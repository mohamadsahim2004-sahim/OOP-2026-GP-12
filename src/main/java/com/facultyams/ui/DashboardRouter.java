package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.ui.admin.AdminDashboardFrame;

public final class DashboardRouter {

    private DashboardRouter() {
    }

    public static void openDashboard(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        Role role = user.getRole();

        if (role == Role.ADMIN) {

            new AdminDashboardFrame()
                    .setVisible(true);

        } else {

            new PlaceholderDashboardFrame(
                    role
            ).setVisible(true);
        }
    }
}