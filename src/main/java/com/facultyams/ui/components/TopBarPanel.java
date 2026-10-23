package com.facultyams.ui.components;

import com.facultyams.model.Student;
import com.facultyams.service.DataService;
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
        this.notificationAction = () -> {
            if (navigationCallback != null) navigationCallback.accept("Notice Board");
        };

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
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(COLOR_PRIMARY);
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
        avatar.setPreferredSize(new Dimension(34, 34));

        // Text: Abhishek Verma + B.Tech CSE • 3rd Year
        JPanel textP = new JPanel();
        textP.setOpaque(false);
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(s.getFullName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(COLOR_TEXT_PRIMARY);

        String degree = s.getDegreeProgram();
        if (degree.contains("-")) {
            degree = degree.replace("-", "•");
        }
        JLabel roleLbl = new JLabel(degree);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roleLbl.setForeground(COLOR_TEXT_SECONDARY);

        textP.add(nameLbl);
        textP.add(roleLbl);

        pill.add(avatar);
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
}
