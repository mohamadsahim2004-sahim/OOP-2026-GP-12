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

public class SidebarPanel extends JPanel {
    private final Consumer<String> navigationCallback;
    private final List<NavItem> navItems = new ArrayList<>();
    private String activeView = "Dashboard";

    public SidebarPanel(Consumer<String> navigationCallback) {
        this.navigationCallback = navigationCallback;
        setPreferredSize(new Dimension(240, 0));
        setBackground(Theme.SIDEBAR_BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.CARD_BORDER));

        // Top Logo Banner
        add(createLogoHeader(), BorderLayout.NORTH);

        // Navigation Items
        add(createNavMenu(), BorderLayout.CENTER);

        // Bottom Logout
        add(createLogoutSection(), BorderLayout.SOUTH);
    }

    private JPanel createLogoHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 96, 242), getWidth(), getHeight(), new Color(20, 70, 200));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(240, 150));
        header.setLayout(new GridBagLayout());

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // White graduation cap
        JLabel iconLbl = new JLabel(IconHelper.getIcon("graduation-cap", 42, Color.WHITE));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("SRMS");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Student Result");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(224, 235, 255));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl2 = new JLabel("Management System");
        subLbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl2.setForeground(new Color(224, 235, 255));
        subLbl2.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(iconLbl);
        box.add(Box.createVerticalStrut(4));
        box.add(titleLbl);
        box.add(Box.createVerticalStrut(2));
        box.add(subLbl);
        box.add(subLbl2);

        header.add(box);
        return header;
    }

    private JPanel createNavMenu() {
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(Theme.createEmptyPadding(14, 12, 14, 12));

        // Define nav items matching screenshot and user requirements
        addNavItem(menu, "Dashboard", "dashboard");
        addNavItem(menu, "Profile", "profile");
        addNavItem(menu, "Course Enrollment", "courses");
        addNavItem(menu, "Course Materials", "downloads");
        addNavItem(menu, "Results", "results");
        addNavItem(menu, "Attendance", "attendance");
        addNavItem(menu, "Medical Records", "medical");
        addNavItem(menu, "Timetable", "timetable");
        addNavItem(menu, "Notice Board", "loudspeaker");

        return menu;
    }

    private void addNavItem(JPanel menu, String title, String iconName) {
        NavItem item = new NavItem(title, iconName);
        navItems.add(item);
        menu.add(item);
        menu.add(Box.createVerticalStrut(4));
    }

    private JPanel createLogoutSection() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(Theme.createEmptyPadding(12, 16, 20, 16));

        JPanel logoutBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
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
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (isHovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setAntiAliasing(g2);
                    g2.setColor(Theme.ACCENT_ROSE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        logoutBtn.setOpaque(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(IconHelper.getIcon("logout", 18, Theme.ACCENT_ROSE));
        JLabel lbl = new JLabel("Logout");
        lbl.setFont(Theme.FONT_BODY_BOLD);
        lbl.setForeground(Theme.ACCENT_ROSE);

        logoutBtn.add(icon);
        logoutBtn.add(lbl);
        bottom.add(logoutBtn, BorderLayout.CENTER);

        return bottom;
    }

    public void setActiveView(String viewName) {
        this.activeView = viewName;
        for (NavItem item : navItems) {
            item.setActive(item.title.equalsIgnoreCase(viewName));
        }
        repaint();
    }

    private class NavItem extends JPanel {
        private final String title;
        private final String iconName;
        private boolean isActive = false;
        private boolean isHovered = false;
        private final JLabel iconLbl;
        private final JLabel textLbl;

        public NavItem(String title, String iconName) {
            this.title = title;
            this.iconName = iconName;
            this.isActive = title.equalsIgnoreCase(activeView);

            setLayout(new BorderLayout(12, 0));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(Theme.createEmptyPadding(8, 14, 8, 14));
            setMaximumSize(new Dimension(220, 42));
            setPreferredSize(new Dimension(216, 40));

            iconLbl = new JLabel();
            textLbl = new JLabel(title);
            textLbl.setFont(Theme.FONT_BODY_BOLD);

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
                        navigationCallback.accept(title);
                    }
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            updateVisuals();
            repaint();
        }

        private void updateVisuals() {
            Color color;
            if (isActive) {
                color = Theme.SIDEBAR_ACTIVE_TEXT;
            } else if (isHovered) {
                color = Theme.TEXT_MAIN;
            } else {
                color = Theme.SIDEBAR_TEXT;
            }
            iconLbl.setIcon(IconHelper.getIcon(iconName, 18, color));
            textLbl.setForeground(color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);

            if (isActive) {
                g2.setColor(Theme.SIDEBAR_ACTIVE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } else if (isHovered) {
                g2.setColor(Theme.SIDEBAR_HOVER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
