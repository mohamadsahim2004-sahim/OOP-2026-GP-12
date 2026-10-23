package com.facultyams.ui.components;

import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Slim modern sidebar matching the exact SRMS undergraduate portal design.
 */
public class SidebarPanel extends JPanel {
    private final Consumer<String> navigationCallback;
    private final List<NavItem> navItems = new ArrayList<>();
    private String activeView = "Dashboard";

    private static final Color COLOR_PRIMARY = Theme.PRIMARY;
    private static final Color COLOR_PRIMARY_LIGHT = Theme.PRIMARY_LIGHT;
    private static final Color COLOR_TEXT_PRIMARY = Theme.TEXT_MAIN;
    private static final Color COLOR_TEXT_MUTED = Theme.TEXT_MUTED;
    private static final Color COLOR_BORDER = Theme.CONTROL_BORDER;
    private static final Color COLOR_HOVER_BG = Theme.SIDEBAR_HOVER_BG;

    public SidebarPanel(Consumer<String> navigationCallback) {
        this.navigationCallback = navigationCallback;
        setPreferredSize(new Dimension(220, 0));
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        // 1. Top Brand Header
        add(createBrandHeader(), BorderLayout.NORTH);

        // 2. Navigation Items
        add(createNavMenu(), BorderLayout.CENTER);

        // 3. Bottom Logout
        add(createLogoutSection(), BorderLayout.SOUTH);
    }

    private JPanel createBrandHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(22, 24, 16, 24));

        JLabel titleLbl = new JLabel("SRMS");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_PRIMARY);

        JLabel subLbl = new JLabel("Undergraduate Portal");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(COLOR_TEXT_MUTED);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(2));
        header.add(subLbl);

        return header;
    }

    private JPanel createNavMenu() {
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // Essential menu items matching the image exactly
        addNavItem(menu, "Dashboard", "dashboard", "Dashboard");
        addNavItem(menu, "Results", "results", "Results");
        addNavItem(menu, "Courses", "courses", "Course Enrollment");
        addNavItem(menu, "Attendance", "attendance", "Attendance");
        addNavItem(menu, "Timetable", "timetable", "Timetable");
        addNavItem(menu, "Notices", "loudspeaker", "Notice Board");
        addNavItem(menu, "Profile", "profile", "Profile");

        menu.add(Box.createVerticalGlue());
        return menu;
    }

    private void addNavItem(JPanel menu, String title, String iconName, String targetView) {
        NavItem item = new NavItem(title, iconName, targetView);
        navItems.add(item);
        menu.add(item);
        menu.add(Box.createVerticalStrut(4));
    }

    private JPanel createLogoutSection() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));

        JLabel logoutLbl = new JLabel("Logout");
        logoutLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutLbl.setForeground(COLOR_TEXT_MUTED);
        logoutLbl.setCursor(new Cursor(Cursor.HAND_CURSOR));

        logoutLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                logoutLbl.setForeground(new Color(239, 68, 68));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                logoutLbl.setForeground(COLOR_TEXT_MUTED);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                int res = JOptionPane.showConfirmDialog(SidebarPanel.this,
                        "Are you sure you want to log out of SRMS?",
                        "Confirm Logout", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(SidebarPanel.this,
                            "You have been logged out securely.", "Logged Out", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        bottom.add(logoutLbl, BorderLayout.WEST);
        return bottom;
    }

    public void setActiveView(String viewName) {
        this.activeView = viewName;
        for (NavItem item : navItems) {
            item.updateState(viewName);
        }
        repaint();
    }

    private class NavItem extends JPanel {
        private final String title;
        private final String iconName;
        private final String targetView;
        private boolean isActive = false;
        private boolean isHovered = false;
        private final JLabel iconLbl;
        private final JLabel textLbl;

        public NavItem(String title, String iconName, String targetView) {
            this.title = title;
            this.iconName = iconName;
            this.targetView = targetView;
            this.isActive = title.equalsIgnoreCase(activeView);

            setLayout(new BorderLayout(12, 0));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
            setPreferredSize(new Dimension(220, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            iconLbl = new JLabel();
            textLbl = new JLabel(title);
            textLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

            add(iconLbl, BorderLayout.WEST);
            add(textLbl, BorderLayout.CENTER);

            updateVisuals();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    updateVisuals();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    updateVisuals();
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    setActiveView(title);
                    if (navigationCallback != null) {
                        navigationCallback.accept(targetView);
                    }
                }
            });
        }

        public void updateState(String currentView) {
            if (currentView == null) currentView = "Dashboard";
            String cv = currentView.toLowerCase();
            if (title.equalsIgnoreCase("Courses") && (cv.contains("enroll") || cv.contains("course") || cv.contains("material"))) {
                this.isActive = true;
            } else if (title.equalsIgnoreCase("Notices") && (cv.contains("notice"))) {
                this.isActive = true;
            } else {
                this.isActive = title.equalsIgnoreCase(currentView) || targetView.equalsIgnoreCase(currentView);
            }
            updateVisuals();
            repaint();
        }

        private void updateVisuals() {
            Color color;
            if (isActive) {
                color = COLOR_PRIMARY;
            } else if (isHovered) {
                color = COLOR_TEXT_PRIMARY;
            } else {
                color = COLOR_TEXT_MUTED;
            }
            iconLbl.setIcon(IconHelper.getIcon(iconName, 18, color));
            textLbl.setForeground(color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                // Background tint
                g2.setColor(COLOR_PRIMARY_LIGHT);
                g2.fillRoundRect(8, 2, w - 16, h - 4, 8, 8);

                // 3px blue vertical accent bar on left
                g2.setColor(COLOR_PRIMARY);
                g2.fillRoundRect(2, 6, 3, h - 12, 3, 3);
            } else if (isHovered) {
                g2.setColor(COLOR_HOVER_BG);
                g2.fillRoundRect(8, 2, w - 16, h - 4, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
