package com.facultyams.ui.admin;

import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.ui.DashboardRouter;
import com.facultyams.util.UIUtil;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Content of the Admin window: navy sidebar on the left, application header on top
 * and the selected screen in the centre. Each screen implements Refreshable and
 * reloads its data from the database every time it is opened.
 */
public class AdminShell extends JPanel {

    private static final String LOGOUT = "logout";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> screens = new LinkedHashMap<>();
    private final JLabel welcomeLabel = new JLabel();
    private final JLabel sidebarName = new JLabel();

    private final JFrame owner;

    public AdminShell(JFrame owner, User user) {
        super(new BorderLayout());
        if (user == null || !Session.isAdmin()) {
            throw new IllegalStateException("Administrator access required.");
        }
        this.owner = owner;

        add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.add(buildHeader(), BorderLayout.NORTH);
        content.setBackground(Color.WHITE);
        main.add(content, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        addScreen("dashboard", "Admin Dashboard", new DashboardPanel(this::showPage));
        addScreen("users", "User Management", new UserManagementPanel());
        addScreen("departments", "Department Management", new DepartmentManagementPanel());
        addScreen("courses", "Course Management", new CourseManagementPanel());
        addScreen("notices", "Notice Management", new NoticeManagementPanel());
        addScreen("timetables", "Timetable Management", new TimeTableManagementPanel());
        addScreen("reports", "Reports", new ReportsPanel());
        addScreen("profile", "My Profile", new ProfilePanel(this::updateUserLabels));
        addScreen("settings", "System Settings", new SettingsPanel());

        updateUserLabels();
        showPage("dashboard");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
        sidebar.setBackground(AdminTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(230, 100));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

        JPanel profile = new JPanel(new BorderLayout(12, 0));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 10));
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        profile.setAlignmentX(0f);
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(AdminTheme.NAVY);
                g2.fillOval(getWidth() / 2 - 7, 9, 14, 14);
                g2.fillArc(getWidth() / 2 - 13, 26, 26, 26, 0, 180);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(46, 46));
        JLabel role = new JLabel("Admin");
        role.setForeground(Color.WHITE);
        role.setFont(AdminTheme.FONT_BOLD);
        sidebarName.setForeground(new Color(0xC9D6F2));
        sidebarName.setFont(AdminTheme.FONT);
        JPanel names = new JPanel(new java.awt.GridLayout(2, 1));
        names.setOpaque(false);
        names.add(role);
        names.add(sidebarName);
        profile.add(avatar, BorderLayout.WEST);
        profile.add(names, BorderLayout.CENTER);
        sidebar.add(profile);

        addNav(sidebar, "dashboard", "Dashboard");
        addNav(sidebar, "users", "User Management");
        addNav(sidebar, "departments", "Department Management");
        addNav(sidebar, "courses", "Course Management");
        addNav(sidebar, "notices", "Notices");
        addNav(sidebar, "timetables", "Timetables");
        addNav(sidebar, "reports", "Reports");
        addNav(sidebar, "profile", "Profile");
        addNav(sidebar, "settings", "System Settings");
        sidebar.add(Box.createVerticalGlue());
        addNav(sidebar, LOGOUT, "Logout");
        return sidebar;
    }

    private void addNav(JPanel sidebar, String key, String text) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(AdminTheme.FONT);
        b.setForeground(Color.WHITE);
        b.setBackground(AdminTheme.NAVY);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(0f);
        b.addActionListener(e -> {
            if (LOGOUT.equals(key)) {
                logout();
            } else {
                showPage(key);
            }
        });
        navButtons.put(key, b);
        sidebar.add(b);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AdminTheme.HEADER_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JLabel app = new JLabel("Faculty Academic Management System");
        app.setForeground(Color.WHITE);
        app.setFont(new Font("SansSerif", Font.BOLD, 16));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(AdminTheme.FONT);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(welcomeLabel);
        header.add(app, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void addScreen(String key, String title, JComponent screen) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        page.add(AdminTheme.title(title), BorderLayout.NORTH);
        page.add(screen, BorderLayout.CENTER);
        AdminTheme.apply(screen);
        content.add(page, key);
        screens.put(key, (Refreshable) screen);
    }

    public void showPage(String key) {
        if (!screens.containsKey(key)) {
            return;
        }
        cards.show(content, key);
        navButtons.forEach((k, b) -> b.setBackground(k.equals(key) ? AdminTheme.NAVY_ACTIVE : AdminTheme.NAVY));
        screens.get(key).refresh();
    }

    private void updateUserLabels() {
        User me = Session.getCurrentUser();
        if (me != null) {
            welcomeLabel.setText("Welcome, " + me.getFullName());
            sidebarName.setText(me.getFullName());
        }
    }

    private void logout() {
        if (UIUtil.confirm(this, "Are you sure you want to log out?")) {
            DashboardRouter.logout(owner);
        }
    }
}