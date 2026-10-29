package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;

/** Vector icons used by the enrollment screen. */
public final class EnrollmentIcons {
    public enum Type { CHEVRON_LEFT, SEARCH, CHEVRON_DOWN, CLOSE, CHECK }

    private EnrollmentIcons() {}

    public static void paint(Graphics2D graphics, Type type, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case CHEVRON_LEFT -> {
                g.drawLine(14, 5, 7, 12);
                g.drawLine(7, 12, 14, 19);
            }
            case SEARCH -> {
                g.drawOval(3, 3, 13, 13);
                g.drawLine(14, 14, 21, 21);
            }
            case CHEVRON_DOWN -> {
                g.drawLine(6, 9, 12, 15);
                g.drawLine(12, 15, 18, 9);
            }
            case CLOSE -> {
                g.drawLine(6, 6, 18, 18);
                g.drawLine(18, 6, 6, 18);
            }
            case CHECK -> {
                g.drawLine(4, 12, 10, 18);
                g.drawLine(10, 18, 20, 6);
            }
        }
        g.dispose();
    }
}
