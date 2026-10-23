package com.facultyams.ui.components;

import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.service.ProfileChangeListener;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * TopBarPanel matching the exact screenshot design.
 */
public class TopBarPanel extends JPanel {
    private final JLabel titleLabel;
    private final Consumer<String> navigationCallback;
    private final Consumer<String> searchCallback;
    private Runnable notificationAction;
    private int notificationCount;
    private JPanel notificationBell;
    private JTextField searchField;
    private AvatarLabel profileAvatar;
    private JLabel profileName;
    private JLabel profileDegree;
    private final ProfileChangeListener profileChangeListener;

    private static final Color COLOR_TEXT_PRIMARY = Theme.TEXT_MAIN;
    private static final Color COLOR_TEXT_SECONDARY = Theme.TEXT_MUTED;
    private static final Color COLOR_TEXT_MUTED = Theme.TEXT_SUBTLE;
    private static final Color COLOR_PRIMARY = Theme.PRIMARY;
    private static final Color COLOR_DANGER = Theme.NOTIFICATION_RED;
    private static final Color COLOR_BORDER = Theme.CONTROL_BORDER;
    private static final Color COLOR_BG_FIELD = Theme.TOPBAR_FIELD_BG;

    public TopBarPanel(String initialTitle, Runnable hamburgerToggle,
                       Consumer<String> searchCallback, Consumer<String> navigationCallback) {
        this.navigationCallback = navigationCallback;
        this.searchCallback = searchCallback;
        this.profileChangeListener = student -> SwingUtilities.invokeLater(() -> updateProfileChip(student));
        this.notificationAction = () -> {
            if (navigationCallback != null) navigationCallback.accept("Notice Board");
        };
        DataService.getInstance().addProfileChangeListener(profileChangeListener);

        setPreferredSize(new Dimension(0, 64));
        setBackground(Color.WHITE);
        setLayout(new BorderLayout(16, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        // Left: Page Title
        titleLabel = new JLabel(initialTitle);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);
        add(titleLabel, BorderLayout.WEST);

        // Right Section: Search + Bell + Avatar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        // 1. Search Field
        right.add(createSearchField());

        // 2. Notification Bell
        right.add(createNotificationBell());

        // 3. Avatar Label (AV + Abhishek Verma + B.Tech CSE • 3rd Year)
        right.add(createProfileLabel());

        add(right, BorderLayout.EAST);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setNotificationAction(Runnable action) {
        notificationAction = action;
    }

    public void setNotificationCount(int count) {
        notificationCount = Math.max(0, count);
        if (notificationBell != null) notificationBell.repaint();
    }

    public void setSearchText(String text) {
        if (searchField != null && !searchField.getText().equals(text)) {
            searchField.setText(text);
            searchField.setForeground(COLOR_TEXT_PRIMARY);
            searchField.repaint();
        }
    }

    public void clearSearch() {
        if (searchField != null) {
            searchField.setText("");
            searchField.setForeground(COLOR_TEXT_MUTED);
            searchField.repaint();
        }
    }

    /** Keeps the shared search field visible but suppresses its caret on non-search profile views. */
    public void setSearchInputActive(boolean active) {
        if (searchField == null) return;
        searchField.setEditable(active);
        searchField.setFocusable(active);
        if (!active) {
            searchField.setCaretPosition(0);
            searchField.getCaret().setVisible(false);
            searchField.setForeground(COLOR_TEXT_MUTED);
        } else {
            searchField.setForeground(searchField.getText().isEmpty()
                    ? COLOR_TEXT_MUTED : COLOR_TEXT_PRIMARY);
        }
    }

    private JPanel createSearchField() {
        JPanel pill = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(COLOR_BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(300, 36));
        pill.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getText().isEmpty()) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    Theme.setAntiAliasing(g);
                    g.setColor(COLOR_TEXT_MUTED);
                    g.setFont(getFont());
                    g.drawString("Search courses, lecturers, venues...", 0,
                            getHeight() / 2 + g.getFontMetrics().getAscent() / 2 - 1);
                    g.dispose();
                }
            }
        };
        searchField = tf;
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(COLOR_TEXT_MUTED);
        tf.setBorder(BorderFactory.createEmptyBorder());
        tf.setOpaque(false);

        tf.addActionListener(e -> {
            if (searchCallback != null) {
                searchCallback.accept(tf.getText().trim());
            }
        });

        JLabel searchIcon = new JLabel(new TimetableIcon(TimetableIcon.Shape.SEARCH, COLOR_TEXT_MUTED, 16));
        pill.add(searchIcon, BorderLayout.WEST);
        pill.add(tf, BorderLayout.CENTER);
        return pill;
    }

    private JPanel createNotificationBell() {
        notificationBell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);

                if (notificationCount > 0) {
                    String count = notificationCount > 9 ? "9+" : Integer.toString(notificationCount);
                    int badgeWidth = count.length() > 1 ? 18 : 13;
                    g2.setColor(COLOR_DANGER);
                    g2.fillRoundRect(getWidth() - badgeWidth, 0, badgeWidth, 14, 12, 12);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.drawString(count, getWidth() - badgeWidth + 3, 10);
                }
                g2.dispose();
            }
        };
        notificationBell.setOpaque(false);
        notificationBell.setPreferredSize(new Dimension(34, 34));
        notificationBell.setLayout(new GridBagLayout());
        notificationBell.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel bell = new JLabel(IconHelper.getIcon("bell", 18, COLOR_TEXT_SECONDARY));
        notificationBell.add(bell);

        notificationBell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (notificationAction != null) notificationAction.run();
            }
        });

        return notificationBell;
    }

    private JPanel createProfileLabel() {
        Student s = DataService.getInstance().getCurrentStudent();

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pill.setOpaque(false);
        pill.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Circular Avatar "AV"
        profileAvatar = new AvatarLabel(s.getProfileImageUrl(), initials(s.getFullName()), 34);

        // Text: Abhishek Verma + B.Tech CSE • 3rd Year
        JPanel textP = new JPanel();
        textP.setOpaque(false);
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));

        profileName = new JLabel(s.getFullName());
        profileName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        profileName.setForeground(COLOR_TEXT_PRIMARY);

        String degree = s.getDegreeProgram();
        if (degree.contains("-")) {
            degree = degree.replace("-", "•");
        }
        profileDegree = new JLabel(degree);
        profileDegree.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        profileDegree.setForeground(COLOR_TEXT_SECONDARY);

        textP.add(profileName);
        textP.add(profileDegree);

        pill.add(profileAvatar);
        pill.add(textP);

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

    private void updateProfileChip(Student student) {
        if (profileAvatar == null) return;
        profileAvatar.setImagePath(student.getProfileImageUrl());
        if (profileName != null) profileName.setText(student.getFullName());
        if (profileDegree != null) {
            profileDegree.setText(student.getDegreeProgram().replace("-", "\u2022"));
        }
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "ST";
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, 1).toUpperCase()
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
