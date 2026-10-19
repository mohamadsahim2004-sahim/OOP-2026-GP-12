package com.facultyams.ui.admin;

import com.facultyams.model.User;
import com.facultyams.security.Session;

import javax.swing.JFrame;
import java.awt.Dimension;

/**
 * The Administrator window opened by DashboardRouter: a plain frame around {@link AdminShell}, the shared
 * {@code AppShell} (sidebar, header, logout, role check) with all Admin pages - including Notices and
 * Timetables. All layout and navigation live in the shell; the shell also reloads each page's data every
 * time it is shown, which a bare tabbed window does not.
 *
 * <p>Both constructors are kept so DashboardRouter can call either form. The shell refuses to open
 * (IllegalStateException, handled by the router) unless the session belongs to a logged-in Admin.
 */
public class AdminDashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Opens the window for the user in the current session. */
    public AdminDashboardFrame() {
        this(Session.getCurrentUser());
    }

    public AdminDashboardFrame(User user) {
        super("FoT-AMS - Administrator Dashboard");
        AdminShell shell = new AdminShell(this, user);   // throws IllegalStateException if not an Admin session
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(shell);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }
}