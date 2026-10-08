package com.facultyams.ui.admin;

import com.facultyams.security.Session;
import com.facultyams.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class AdminDashboardFrame extends JFrame {

    private final JTabbedPane tabs;

    public AdminDashboardFrame() {

        Session.requireAdmin();

        setTitle(
                "Faculty Academic Management System - Admin"
        );

        setSize(1200, 750);
        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 15, 10, 15
                )
        );

        JLabel title =
                new JLabel(
                        "ADMIN DASHBOARD"
                );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        header.add(
                title,
                BorderLayout.WEST
        );

        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        JLabel userLabel =
                new JLabel(
                        "Logged in as: "
                                + Session
                                .getCurrentUser()
                                .getUsername()
                );

        JButton logoutButton =
                new JButton("Logout");

        right.add(userLabel);
        right.add(logoutButton);

        header.add(
                right,
                BorderLayout.EAST
        );

        tabs = new JTabbedPane();

        tabs.addTab(
                "Overview",
                createOverviewPanel()
        );

        tabs.addTab(
                "Users",
                new UserManagementPanel()
        );

        tabs.addTab(
                "Departments",
                new DepartmentManagementPanel()
        );

        tabs.addTab(
                "Courses",
                new CourseManagementPanel()
        );

        tabs.addTab(
                "Notices",
                new NoticeManagementPanel()
        );

        tabs.addTab(
                "Timetable",
                new TimeTableManagementPanel()
        );

        logoutButton.addActionListener(
                e -> logout()
        );

        setLayout(new BorderLayout());

        add(
                header,
                BorderLayout.NORTH
        );

        add(
                tabs,
                BorderLayout.CENTER
        );
    }

    private JPanel createOverviewPanel() {

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                2,
                                3,
                                20,
                                20
                        )
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        30, 30, 30, 30
                )
        );

        panel.add(
                createCard(
                        "Users",
                        "Manage system users"
                )
        );

        panel.add(
                createCard(
                        "Departments",
                        "Manage departments"
                )
        );

        panel.add(
                createCard(
                        "Courses",
                        "Manage course units"
                )
        );

        panel.add(
                createCard(
                        "Notices",
                        "Publish announcements"
                )
        );

        panel.add(
                createCard(
                        "Timetable",
                        "Manage schedules"
                )
        );

        panel.add(
                createCard(
                        "Security",
                        "Role-based access"
                )
        );

        return panel;
    }

    private JPanel createCard(
            String title,
            String description) {

        JPanel card =
                new JPanel(
                        new BorderLayout()
                );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Color.GRAY
                        ),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        18
                )
        );

        JLabel descriptionLabel =
                new JLabel(
                        "<html>"
                                + description
                                + "</html>"
                );

        card.add(
                titleLabel,
                BorderLayout.NORTH
        );

        card.add(
                descriptionLabel,
                BorderLayout.CENTER
        );

        return card;
    }

    private void logout() {

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to logout?",
                        "Logout",
                        JOptionPane.YES_NO_OPTION
                );

        if (result
                != JOptionPane.YES_OPTION) {
            return;
        }

        new AuthService().logout();

        dispose();

        new com.facultyams.ui.LoginFrame()
                .setVisible(true);
    }
}