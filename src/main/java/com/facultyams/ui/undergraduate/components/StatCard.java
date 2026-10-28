package com.facultyams.ui.components;

import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

public class StatCard extends CardPanel {

    public StatCard(String iconName, Color iconBg, Color iconColor, String title, String value, String subText, Color subColor) {
        super(14);
        setLayout(new BorderLayout(14, 0));
        setBorder(Theme.createEmptyPadding(16, 16, 16, 16));

        // Icon Container
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(iconBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconContainer.setOpaque(false);
        iconContainer.setPreferredSize(new Dimension(50, 50));
        iconContainer.setLayout(new GridBagLayout());
        JLabel iconLbl = new JLabel(IconHelper.getIcon(iconName, 26, iconColor));
        iconContainer.add(iconLbl);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Title (e.g. CGPA, Courses Enrolled)
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_SMALL);
        titleLbl.setForeground(Theme.TEXT_MUTED);

        // Metric value such as a 4.0-scale GPA or course count.
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(Theme.FONT_METRIC);
        valLbl.setForeground(Theme.TEXT_MAIN);

        // Subtitle (e.g. Current CGPA, This Semester)
        JLabel subLbl = new JLabel(subText);
        subLbl.setFont(Theme.FONT_METRIC_SUB);
        subLbl.setForeground(subColor);

        contentPanel.add(titleLbl);
        contentPanel.add(Box.createVerticalStrut(2));
        contentPanel.add(valLbl);
        contentPanel.add(Box.createVerticalStrut(2));
        contentPanel.add(subLbl);

        add(iconContainer, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }
}
