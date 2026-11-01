package com.facultyams.ui.components;

import java.awt.*;

import com.facultyams.Theme;

/** Small vector icons keep the course overview independent of icon fonts. */
public final class Icons {
    public enum Type { SEARCH, CHEVRON, GRID, LIST, DOTS, BOOK, CHECK, CLOSE }

    private Icons() {}

    public static void paint(Graphics2D graphics, Type type, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case SEARCH -> {
                g.drawOval(3, 3, 13, 13);
                g.drawLine(14, 14, 21, 21);
            }
            case CHEVRON -> {
                g.drawLine(6, 9, 12, 15);
                g.drawLine(12, 15, 18, 9);
            }
            case GRID -> {
                g.drawRoundRect(3, 3, 7, 7, 1, 1);
                g.drawRoundRect(14, 3, 7, 7, 1, 1);
                g.drawRoundRect(3, 14, 7, 7, 1, 1);
                g.drawRoundRect(14, 14, 7, 7, 1, 1);
            }
            case LIST -> {
                for (int row = 5; row <= 17; row += 6) {
                    g.fillOval(3, row - 1, 2, 2);
                    g.drawLine(9, row, 21, row);
                }
            }
            case DOTS -> {
                for (int row = 7; row <= 17; row += 5) g.fillOval(10, row - 1, 4, 4);
            }
            case BOOK -> {
                g.drawRoundRect(3, 4, 18, 16, 2, 2);
                g.drawLine(12, 5, 12, 19);
                g.drawLine(6, 8, 10, 8);
                g.drawLine(14, 8, 18, 8);
                g.drawLine(6, 12, 10, 12);
                g.drawLine(14, 12, 18, 12);
            }
            case CHECK -> {
                g.drawLine(4, 12, 10, 18);
                g.drawLine(10, 18, 20, 6);
            }
            case CLOSE -> {
                g.drawLine(6, 6, 18, 18);
                g.drawLine(18, 6, 6, 18);
            }
        }
        g.dispose();
    }
}
