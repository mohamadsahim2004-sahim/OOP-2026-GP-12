package com.facultyams.ui;

import com.facultyams.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Temporary dashboard shown to Lecturer / Technical Officer / Student after login
 * until the responsible member plugs in their real dashboard (see DashboardRouter).
 */
public class PlaceholderDashboardFrame extends JFrame {

    public PlaceholderDashboardFrame(User user, String moduleName) {
        super("FoT-AMS - " + user.getRole().getDisplayName() + " Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel welcome = new JLabel("Welcome, " + user.getFullName(), SwingConstants.CENTER);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 16f));
        JLabel role = new JLabel("Logged in as: " + user.getRole().getDisplayName()
                + " (" + user.getUsername() + ")", SwingConstants.CENTER);
        JLabel note = new JLabel(moduleName + " is under development.", SwingConstants.CENTER);

        JPanel center = new JPanel(new GridLayout(3, 1, 5, 5));
        center.add(welcome);
        center.add(role);
        center.add(note);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> DashboardRouter.logout(this));
        JPanel south = new JPanel();
        south.add(logoutButton);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        content.add(center, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(450, 220);
        setLocationRelativeTo(null);
    }
}