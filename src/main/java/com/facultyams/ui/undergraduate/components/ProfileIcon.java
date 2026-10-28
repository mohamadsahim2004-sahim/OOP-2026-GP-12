package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.Icon;

/** Small vector icons used by profile controls. */
public final class ProfileIcon implements Icon {
    public enum Shape { LOCK, CAMERA, COPY, MAIL, EDIT, CHECK, EYE, EYE_OFF, CLOSE }

    private final Shape shape;
    private final Color color;
    private final int size;

    /** Creates a vector profile icon. */
    public ProfileIcon(Shape shape, Color color) {
        this(shape, color, 16);
    }

    /** Creates a vector profile icon with the requested size. */
    public ProfileIcon(Shape shape, Color color, int size) {
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
        g.setColor(color);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (shape) {
            case LOCK -> {
                g.drawRoundRect(size / 4, size / 2, size / 2, size * 2 / 5, 2, 2);
                g.drawArc(size / 3, size / 6, size / 3, size / 2, 0, 180);
            }
            case CAMERA -> {
                g.drawRoundRect(2, size / 3, size - 4, size / 2, 3, 3);
                g.drawOval(size / 3, size / 2 - 1, size / 3, size / 3);
                g.drawLine(size / 3, size / 3, size / 2, size / 5);
                g.drawLine(size / 2, size / 5, size * 2 / 3, size / 3);
            }
            case COPY -> {
                g.drawRoundRect(size / 3, size / 6, size / 2, size * 3 / 5, 2, 2);
                g.drawRoundRect(size / 6, size / 3, size / 2, size * 3 / 5, 2, 2);
            }
            case MAIL -> {
                g.drawRoundRect(1, size / 4, size - 2, size / 2, 2, 2);
                g.drawLine(2, size / 3, size / 2, size * 3 / 5);
                g.drawLine(size - 2, size / 3, size / 2, size * 3 / 5);
            }
            case EDIT -> {
                g.drawLine(size / 4, size * 3 / 4, size * 3 / 4, size / 4);
                g.drawLine(size / 5, size * 4 / 5, size / 3, size * 3 / 4);
                g.drawLine(size * 3 / 4, size / 4, size * 4 / 5, size / 3);
            }
            case CHECK -> {
                g.drawLine(size / 5, size / 2, size * 2 / 5, size * 4 / 5);
                g.drawLine(size * 2 / 5, size * 4 / 5, size * 4 / 5, size / 5);
            }
            case EYE -> {
                g.drawOval(size / 6, size / 3, size * 2 / 3, size / 3);
                g.fillOval(size * 5 / 12, size * 5 / 12, size / 6, size / 6);
            }
            case EYE_OFF -> {
                g.drawOval(size / 6, size / 3, size * 2 / 3, size / 3);
                g.fillOval(size * 5 / 12, size * 5 / 12, size / 6, size / 6);
                g.drawLine(size / 5, size * 4 / 5, size * 4 / 5, size / 5);
            }
            case CLOSE -> {
                g.drawLine(size / 4, size / 4, size * 3 / 4, size * 3 / 4);
                g.drawLine(size * 3 / 4, size / 4, size / 4, size * 3 / 4);
            }
        }
        g.dispose();
    }
}
