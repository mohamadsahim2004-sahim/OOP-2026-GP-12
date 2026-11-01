package com.facultyams.ui;

import javax.swing.JFrame;
import java.awt.Dimension;

/** Application login window. Uses the shared styled login panel and database-backed authentication. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginFrame() {
        super("Faculty Academic Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new LoginPanel(user -> {
            dispose();
            DashboardRouter.openDashboard(user);
        }));
        setSize(new Dimension(900, 640));
        setMinimumSize(new Dimension(460, 590));
        setLocationRelativeTo(null);
    }
}