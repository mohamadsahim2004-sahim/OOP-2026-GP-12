package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Flat primary or outline action button for the timetable. */
public class FlatButton extends JButton {
    private final boolean primary;

    public FlatButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setFont(Theme.FONT_BODY_BOLD);
        setForeground(primary ? Color.WHITE : Theme.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        setMinimumSize(new Dimension(0, Theme.CONTROL_HEIGHT));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
    }

    @Override public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, Theme.CONTROL_HEIGHT);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        boolean hover = getModel().isRollover();
        if (primary) {
            g.setColor(hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
        } else {
            g.setColor(hover ? Theme.PRIMARY_LIGHT : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
            g.setColor(Theme.CONTROL_BORDER);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
