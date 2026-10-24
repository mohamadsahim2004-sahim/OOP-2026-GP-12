package com.facultyams.ui.components;

import java.awt.*;
import java.awt.geom.Path2D;

import com.facultyams.Theme;

final class DeadlineIcons {
    private DeadlineIcons() {}

    static void paint(Graphics2D graphics, String name, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = Math.max(2, size / 5);
        if ("chevron".equals(name)) {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.38, y + size * 0.22);
            path.lineTo(x + size * 0.62, y + size * 0.5);
            path.lineTo(x + size * 0.38, y + size * 0.78);
            g.draw(path);
        } else if ("document".equals(name)) {
            int left = x + inset;
            int top = y + 1;
            int width = size - inset * 2;
            int height = size - 2;
            g.drawRoundRect(left, top, width, height, 4, 4);
            g.drawLine(left + 3, top + height / 3, left + width - 3, top + height / 3);
            g.drawLine(left + 3, top + height / 2, left + width - 3, top + height / 2);
            g.drawLine(left + 3, top + height * 2 / 3, left + width - 6, top + height * 2 / 3);
        } else if ("clock".equals(name)) {
            g.drawOval(x + 2, y + 2, size - 4, size - 4);
            g.drawLine(x + size / 2, y + size / 2, x + size / 2, y + size / 4);
            g.drawLine(x + size / 2, y + size / 2, x + size * 3 / 4, y + size / 2);
        } else if ("check".equals(name)) {
            g.drawLine(x + inset, y + size / 2, x + size / 2 - 1, y + size - inset);
            g.drawLine(x + size / 2 - 1, y + size - inset, x + size - inset, y + inset);
        }
        g.dispose();
    }
}
