package com.facultyams.ui.components;

import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

public class QuickAccessPanel extends JPanel {
    private Consumer<String> navigationHandler;

    public QuickAccessPanel(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        setOpaque(false);
        setLayout(new GridLayout(2, 3, 10, 10));

        add(createItem("My Results", "results", Theme.ACCENT_BLUE_BG, Theme.ACCENT_BLUE, "Results"));
        add(createItem("My Courses", "courses", Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN, "Courses"));
        add(createItem("Timetable", "timetable", Theme.ACCENT_PURPLE_BG, Theme.ACCENT_PURPLE, "Timetable"));
        add(createItem("Attendance", "profile", Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER, "Attendance"));
        add(createItem("Downloads", "downloads", Theme.ACCENT_BLUE_BG, Theme.ACCENT_BLUE, "Course Materials"));
        add(createItem("Feedback", "feedback", Theme.ACCENT_ROSE_BG, Theme.ACCENT_ROSE, "Feedback"));
    }

    private JPanel createItem(String title, String iconName, Color iconBg, Color iconColor, String targetView) {
        JPanel item = new JPanel() {
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
                        if (navigationHandler != null) {
                            navigationHandler.accept(targetView);
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(isHovered ? Theme.PRIMARY_BORDER : Theme.CARD_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        item.setOpaque(false);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Icon Box
        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(iconBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(36, 36));
        iconBox.add(new JLabel(IconHelper.getIcon(iconName, 18, iconColor)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(Theme.TEXT_MAIN);

        item.add(iconBox, gbc);
        gbc.gridy = 1;
        item.add(lbl, gbc);

        return item;
    }
}
