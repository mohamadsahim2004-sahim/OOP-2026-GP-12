package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.User;
import com.facultyams.service.AdminService;
import com.facultyams.ui.DashboardRouter;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;

/**
 * Admin dashboard: overview figures plus tabs for user, department and course management.
 * Members who add admin screens (notices, timetables) can add another tab in buildTabs().
 */
public class AdminDashboardFrame extends JFrame {

    private final AdminService adminService = new AdminService();
    private final JPanel statsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
    private final JTabbedPane tabs = new JTabbedPane();

    public AdminDashboardFrame(User admin) {
        super("FoT-AMS - Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.add(buildHeader(admin), BorderLayout.NORTH);
        content.add(buildTabs(), BorderLayout.CENTER);
        setContentPane(content);

        setSize(1150, 720);
        setLocationRelativeTo(null);
        refreshStatistics();
    }

    private JPanel buildHeader(User admin) {
        JLabel welcome = new JLabel("Welcome, " + admin.getFullName() + " (Admin)");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 15f));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            if (UIUtil.confirm(this, "Do you want to log out?")) {
                DashboardRouter.logout(this);
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        header.add(welcome, BorderLayout.WEST);
        header.add(logoutButton, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        tabs.addTab("Overview", buildOverview());
        tabs.addTab("Users", new UserManagementPanel());
        tabs.addTab("Departments", new DepartmentManagementPanel());
        tabs.addTab("Courses", new CourseManagementPanel());

        // Reload a tab's data every time it is opened (polymorphic refresh()).
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected instanceof AbstractManagementPanel<?>) {
                ((AbstractManagementPanel<?>) selected).refresh();
            } else {
                refreshStatistics();
            }
        });
        return tabs;
    }

    private JPanel buildOverview() {
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshStatistics());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refreshButton);

        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel overview = new JPanel(new BorderLayout());
        overview.add(statsPanel, BorderLayout.CENTER);
        overview.add(south, BorderLayout.SOUTH);
        return overview;
    }

    private void refreshStatistics() {
        statsPanel.removeAll();
        try {
            Map<String, Integer> stats = adminService.getDashboardStatistics();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                statsPanel.add(statCard(entry.getKey(), entry.getValue()));
            }
        } catch (DatabaseException | AuthorizationException e) {
            statsPanel.add(new JLabel("Statistics unavailable: " + e.getMessage()));
        }
        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private static JPanel statCard(String title, int value) {
        JLabel number = new JLabel(String.valueOf(value), SwingConstants.CENTER);
        number.setFont(number.getFont().deriveFont(Font.BOLD, 28f));
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createTitledBorder(title));
        card.add(number, BorderLayout.CENTER);
        return card;
    }
}