package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Small vector icon set for timetable controls and metadata. */
public final class TimetableIcon implements Icon {
    public enum Shape { LOCATION, PERSON, CLOCK, BELL, BELL_OFF, CALENDAR, SEARCH, CLOSE }

    private final Shape shape;
    private final Color color;
    private final int size;

    public TimetableIcon(Shape shape, Color color, int size) {
        this.shape = shape;
        this.color = color;
        this.size = size;
    }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.translate(x, y);
        g.scale(size / 20.0, size / 20.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (shape) {
            case LOCATION -> {
                Path2D pin = new Path2D.Double();
                pin.moveTo(10, 19); pin.curveTo(8, 16, 3.5, 11.5, 3.5, 8);
                pin.curveTo(3.5, 4.5, 6.4, 2, 10, 2);
                pin.curveTo(13.6, 2, 16.5, 4.5, 16.5, 8);
                pin.curveTo(16.5, 11.5, 12, 16, 10, 19);
                g.draw(pin); g.drawOval(8, 6, 4, 4);
            }
            case PERSON -> {
                g.drawOval(7, 2, 6, 6);
                g.draw(new Arc2D.Double(3, 9, 14, 10, 185, 170, Arc2D.OPEN));
            }
            case CLOCK -> {
                g.drawOval(2, 2, 16, 16); g.drawLine(10, 5, 10, 10); g.drawLine(10, 10, 14, 12);
            }
            case BELL, BELL_OFF -> {
                g.draw(new Arc2D.Double(4, 3, 12, 14, 0, 180, Arc2D.OPEN));
                g.drawLine(4, 15, 16, 15); g.draw(new Arc2D.Double(8, 14, 4, 4, 180, 180, Arc2D.OPEN));
                if (shape == Shape.BELL_OFF) {
                    g.drawLine(3, 3, 17, 17);
                }
            }
            case CALENDAR -> {
                g.drawRoundRect(2, 4, 16, 14, 2, 2); g.drawLine(2, 8, 18, 8);
                g.drawLine(6, 2, 6, 6); g.drawLine(14, 2, 14, 6);
            }
            case SEARCH -> {
                g.drawOval(2, 2, 11, 11); g.drawLine(11, 11, 18, 18);
            }
            case CLOSE -> {
                g.drawLine(5, 5, 15, 15); g.drawLine(15, 5, 5, 15);
            }
        }
        g.dispose();
    }
}
