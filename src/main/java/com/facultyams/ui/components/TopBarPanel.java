package com.facultyams.ui.components;

import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

public class TopBarPanel extends JPanel {
    private final JLabel titleLabel;
    private final Consumer<String> searchCallback;
    private final Consumer<String> navigationCallback;

    public TopBarPanel(String initialTitle, Runnable hamburgerToggle,
                       Consumer<String> searchCallback, Consumer<String> navigationCallback) {
        this.searchCallback = searchCallback;
        this.navigationCallback = navigationCallback;

        setPreferredSize(new Dimension(0, 68));
        setBackground(Color.WHITE);
        setLayout(new BorderLayout(16, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                Theme.createEmptyPadding(12, 24, 12, 24)
        ));

        // Left Section: Hamburger + Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        // Hamburger button
        JLabel hamburger = new JLabel("☰");
        hamburger.setFont(new Font("Segoe UI", Font.BOLD, 18));
        hamburger.setForeground(Theme.TEXT_MAIN);
        hamburger.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hamburger.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hamburgerToggle != null) hamburgerToggle.run();
            }
        });

        titleLabel = new JLabel(initialTitle);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_MAIN);

        left.add(hamburger);
        left.add(titleLabel);
        add(left, BorderLayout.WEST);

        // Right Section: Search Bar + Bell + Profile
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);

        // 1. Search Bar Pill
        right.add(createSearchBar());

        // 2. Notification Bell
        right.add(createNotificationBell());

        // 3. Profile Pill
        right.add(createProfilePill());

        add(right, BorderLayout.EAST);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    private JPanel createSearchBar() {
        JPanel pill = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(Theme.CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(240, 38));
        pill.setBorder(Theme.createEmptyPadding(4, 12, 4, 12));

        JLabel searchIcon = new JLabel(IconHelper.getIcon("search", 16, Theme.TEXT_MUTED));
        JTextField searchField = new JTextField();
        searchField.setFont(Theme.FONT_BODY);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setForeground(Theme.TEXT_MAIN);

        // Placeholder hint
        searchField.setText("Search anything...");
        searchField.setForeground(Theme.TEXT_MUTED);

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search anything...")) {
                    searchField.setText("");
                    searchField.setForeground(Theme.TEXT_MAIN);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setText("Search anything...");
                    searchField.setForeground(Theme.TEXT_MUTED);
                }
            }
        });

        searchField.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty() && !query.equals("Search anything...") && searchCallback != null) {
                searchCallback.accept(query);
            }
        });

        pill.add(searchIcon, BorderLayout.WEST);
        pill.add(searchField, BorderLayout.CENTER);
        return pill;
    }

    private JPanel createNotificationBell() {
        JPanel bellContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                // Badge circle with count 3
                g2.setColor(new Color(37, 99, 235));
                g2.fillOval(getWidth() - 14, 2, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.drawString("3", getWidth() - 10, 11);
                g2.dispose();
            }
        };
        bellContainer.setOpaque(false);
        bellContainer.setPreferredSize(new Dimension(36, 36));
        bellContainer.setLayout(new GridBagLayout());
        bellContainer.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel bell = new JLabel(IconHelper.getIcon("bell", 20, Theme.TEXT_MUTED));
        bellContainer.add(bell);

        bellContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showNotificationsPopup(bellContainer);
            }
        });

        return bellContainer;
    }

    private void showNotificationsPopup(Component source) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1));
        popup.setBackground(Color.WHITE);

        JMenuItem header = new JMenuItem("Notifications (3 Unread)");
        header.setFont(Theme.FONT_BODY_BOLD);
        header.setEnabled(false);
        popup.add(header);
        popup.addSeparator();

        JMenuItem n1 = new JMenuItem("• End Semester Exam Schedule Released (May 15)");
        n1.setFont(Theme.FONT_BODY);
        n1.addActionListener(e -> {
            if (navigationCallback != null) navigationCallback.accept("Notice Board");
        });

        JMenuItem n2 = new JMenuItem("• Mid Semester Results Declared - CGPA: 8.45");
        n2.setFont(Theme.FONT_BODY);
        n2.addActionListener(e -> {
            if (navigationCallback != null) navigationCallback.accept("Results");
        });

        JMenuItem n3 = new JMenuItem("• Mini Project Phase 1 Submission Deadline May 30");
        n3.setFont(Theme.FONT_BODY);
        n3.addActionListener(e -> {
            if (navigationCallback != null) navigationCallback.accept("Notice Board");
        });

        popup.add(n1);
        popup.add(n2);
        popup.add(n3);

        popup.show(source, 0, source.getHeight());
    }

    private JPanel createProfilePill() {
        Student s = DataService.getInstance().getCurrentStudent();

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pill.setOpaque(false);
        pill.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Circular Avatar
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, new Color(59, 130, 246), getWidth(), getHeight(), new Color(37, 99, 235));
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String in = "AV";
                int tx = (getWidth() - fm.stringWidth(in)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(in, tx, ty);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(36, 36));

        // Text
        JPanel textP = new JPanel();
        textP.setOpaque(false);
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel("Hello, " + s.getFullName());
        nameLbl.setFont(Theme.FONT_BODY_BOLD);
        nameLbl.setForeground(Theme.TEXT_MAIN);

        JLabel roleLbl = new JLabel(s.getDegreeProgram());
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roleLbl.setForeground(Theme.TEXT_MUTED);

        textP.add(nameLbl);
        textP.add(roleLbl);

        JLabel chevron = new JLabel("\u2304"); // subtle down chevron
        chevron.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chevron.setForeground(Theme.TEXT_MUTED);

        pill.add(avatar);
        pill.add(textP);
        pill.add(chevron);

        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (navigationCallback != null) {
                    navigationCallback.accept("Profile");
                }
            }
        });

        return pill;
    }
}
