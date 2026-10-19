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

    private static final Color COLOR_TEXT_PRIMARY   = new Color(15, 23, 42);    // #0F172A
    private static final Color COLOR_TEXT_SECONDARY = new Color(100, 116, 139);  // #64748B
    private static final Color COLOR_TEXT_MUTED     = new Color(148, 163, 184); // #94A3B8
    private static final Color COLOR_PRIMARY        = new Color(37, 99, 235);   // #2563EB
    private static final Color COLOR_DANGER         = new Color(220, 38, 38);   // #DC2626
    private static final Color COLOR_BORDER         = new Color(229, 231, 235); // #E5E7EB
    private static final Color COLOR_BG_FIELD       = new Color(248, 250, 252); // #F8FAFC

    public TopBarPanel(String initialTitle, Runnable hamburgerToggle,
                       Consumer<String> searchCallback, Consumer<String> navigationCallback) {
        this.navigationCallback = navigationCallback;

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
        pill.setPreferredSize(new Dimension(240, 36));
        pill.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JTextField tf = new JTextField("Search results, courses...");
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(COLOR_TEXT_MUTED);
        tf.setBorder(BorderFactory.createEmptyBorder());
        tf.setOpaque(false);

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals("Search results, courses...")) {
                    tf.setText("");
                    tf.setForeground(COLOR_TEXT_PRIMARY);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (tf.getText().trim().isEmpty()) {
                    tf.setText("Search results, courses...");
                    tf.setForeground(COLOR_TEXT_MUTED);
                }
            }
        });

        pill.add(tf, BorderLayout.CENTER);
        return pill;
    }

    private JPanel createNotificationBell() {
        JPanel bellContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);

                // Red notification badge matching screenshot
                g2.setColor(COLOR_DANGER);
                g2.fillOval(getWidth() - 13, 1, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.drawString("3", getWidth() - 9, 10);
                g2.dispose();
            }
        };
        bellContainer.setOpaque(false);
        bellContainer.setPreferredSize(new Dimension(34, 34));
        bellContainer.setLayout(new GridBagLayout());
        bellContainer.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel bell = new JLabel(IconHelper.getIcon("bell", 18, COLOR_TEXT_SECONDARY));
        bellContainer.add(bell);

        bellContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (navigationCallback != null) navigationCallback.accept("Notice Board");
            }
        });

        return bellContainer;
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
