package com.facultyams.ui;

import com.facultyams.model.User;

import javax.swing.JFrame;
import java.awt.Dimension;

/**
 * Temporary window shown to Lecturer / Technical Officer / Undergraduate after login until the
 * responsible member plugs in their real dashboard (see {@link DashboardRouter}). All layout is in
 * {@link PlaceholderShell}.
 */
public class PlaceholderDashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public PlaceholderDashboardFrame(User user, String moduleName) {
        super("FoT-AMS - " + user.getRole().getDisplayName() + " Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new PlaceholderShell(this, user, moduleName));
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }
}