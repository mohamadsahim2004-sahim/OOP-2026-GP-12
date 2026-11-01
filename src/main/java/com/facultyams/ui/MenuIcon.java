
package com.facultyams.ui;

import javax.swing.*;
import java.awt.*;

public class MenuIcon implements Icon {

    private final String type;
    private final Color color;

    public MenuIcon(String type, Color color) {
        this.type = type;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return 20;
    }

    @Override
    public int getIconHeight() {
        return 20;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f));
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        int left = x + 2;
        int top = y + 2;

        switch (type) {
            case "dashboard":
                g2.drawRoundRect(left, top, 6, 6, 2, 2);
                g2.drawRoundRect(left + 9, top, 6, 6, 2, 2);
                g2.drawRoundRect(left, top + 9, 6, 6, 2, 2);
                g2.drawRoundRect(left + 9, top + 9, 6, 6, 2, 2);
                break;

            case "profile":
                g2.drawOval(left + 5, top, 7, 7);
                g2.drawArc(left + 2, top + 8, 13, 10, 0, 180);
                break;

            case "book":
                g2.drawRoundRect(left, top + 1, 15, 15, 2, 2);
                g2.drawLine(left + 7, top + 1, left + 7, top + 16);
                break;

            case "edit":
                g2.drawLine(left + 3, top + 14, left + 13, top + 4);
                g2.drawLine(left + 2, top + 16, left + 6, top + 15);
                g2.drawLine(left + 12, top + 3, left + 15, top + 6);
                break;

            case "students":
                g2.drawOval(left, top + 2, 6, 6);
                g2.drawOval(left + 9, top + 2, 6, 6);
                g2.drawArc(left - 2, top + 8, 11, 9, 0, 180);
                g2.drawArc(left + 6, top + 8, 11, 9, 0, 180);
                break;

            case "check":
                g2.drawRoundRect(left + 2, top, 12, 16, 2, 2);
                g2.drawLine(left + 4, top + 8, left + 7, top + 11);
                g2.drawLine(left + 7, top + 11, left + 12, top + 5);
                break;

            case "chart":
                g2.drawLine(left, top + 16, left + 16, top + 16);
                g2.drawLine(left, top + 16, left, top + 1);
                g2.drawRect(left + 3, top + 10, 3, 6);
                g2.drawRect(left + 8, top + 6, 3, 10);
                g2.drawRect(left + 13, top + 2, 3, 14);
                break;

            case "calendar":
                g2.drawRoundRect(left, top + 2, 16, 14, 2, 2);
                g2.drawLine(left, top + 6, left + 16, top + 6);
                g2.drawLine(left + 4, top, left + 4, top + 4);
                g2.drawLine(left + 12, top, left + 12, top + 4);
                break;

            case "notice":
                g2.drawLine(left + 2, top + 7, left + 13, top + 3);
                g2.drawLine(left + 2, top + 7, left + 2, top + 12);
                g2.drawLine(left + 2, top + 12, left + 13, top + 16);
                g2.drawLine(left + 13, top + 3, left + 13, top + 16);
                break;
        }

        g2.dispose();
    }
}
