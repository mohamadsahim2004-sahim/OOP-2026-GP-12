package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

public class CalendarWidget extends JPanel {

    public CalendarWidget() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        // Header: "May 2024"
        JLabel monthLbl = new JLabel("May 2024", JLabel.CENTER);
        monthLbl.setFont(Theme.FONT_BODY_BOLD);
        monthLbl.setForeground(Theme.TEXT_MAIN);
        add(monthLbl, BorderLayout.NORTH);

        // Days Grid (7 columns x 6 rows)
        JPanel grid = new JPanel(new GridLayout(6, 7, 4, 4));
        grid.setOpaque(false);

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : dayNames) {
            JLabel lbl = new JLabel(day, JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(day.equals("Sun") ? Theme.ACCENT_ROSE : Theme.TEXT_MUTED);
            grid.add(lbl);
        }

        // Calendar dates data: [dayNum, isCurrentMonth, isHighlighted, isSunday]
        int[][] dates = {
            {29, 0, 0, 0}, {30, 0, 0, 0}, {1, 1, 0, 0}, {2, 1, 0, 0}, {3, 1, 0, 0}, {4, 1, 0, 0}, {5, 1, 0, 1},
            {6, 1, 0, 0}, {7, 1, 0, 0}, {8, 1, 0, 0}, {9, 1, 0, 0}, {10, 1, 0, 0}, {11, 1, 0, 0}, {12, 1, 0, 1},
            {13, 1, 0, 0}, {14, 1, 0, 0}, {15, 1, 0, 0}, {16, 1, 0, 0}, {17, 1, 0, 0}, {18, 1, 0, 0}, {19, 1, 0, 1},
            {20, 1, 1, 0}, {21, 1, 0, 0}, {22, 1, 0, 0}, {23, 1, 0, 0}, {24, 1, 0, 0}, {25, 1, 0, 0}, {26, 1, 0, 1},
            {27, 1, 0, 0}, {28, 1, 0, 0}, {29, 1, 0, 0}, {30, 1, 0, 0}, {31, 1, 0, 0}, {1, 0, 0, 0}, {2, 0, 0, 1}
        };

        for (int[] d : dates) {
            final int dayNum = d[0];
            final boolean isCurrentMonth = d[1] == 1;
            final boolean isHighlighted = d[2] == 1;
            final boolean isSunday = d[3] == 1;

            JPanel dayCell = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isHighlighted) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setAntiAliasing(g2);
                        g2.setColor(Theme.PRIMARY);
                        int s = Math.min(getWidth(), getHeight()) - 2;
                        int x = (getWidth() - s) / 2;
                        int y = (getHeight() - s) / 2;
                        g2.fillRoundRect(x, y, s, s, 8, 8);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            dayCell.setOpaque(false);

            JLabel dayLbl = new JLabel(String.valueOf(dayNum), JLabel.CENTER);
            dayLbl.setFont(new Font("Segoe UI", isHighlighted ? Font.BOLD : Font.PLAIN, 11));

            if (isHighlighted) {
                dayLbl.setForeground(Color.WHITE);
            } else if (!isCurrentMonth) {
                dayLbl.setForeground(Theme.TEXT_SUBTLE);
            } else if (isSunday) {
                dayLbl.setForeground(Theme.ACCENT_ROSE);
            } else {
                dayLbl.setForeground(Theme.TEXT_MAIN);
            }

            dayCell.add(dayLbl);
            grid.add(dayCell);
        }

        add(grid, BorderLayout.CENTER);
    }
}
