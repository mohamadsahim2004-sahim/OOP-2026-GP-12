package com.facultyams.ui.components;

import java.awt.*;
import javax.swing.JComponent;

import com.facultyams.Theme;

/** A single rounded 6px progress track and solid-color fill. */
public class RoundedProgressBar extends JComponent {
    private final Color fillColor;
    private int value;

    public RoundedProgressBar(int value, Color fillColor) {
        this.value = Math.max(0, Math.min(100, value));
        this.fillColor = fillColor;
        setOpaque(false);
        setPreferredSize(new Dimension(100, 6));
        setMinimumSize(new Dimension(24, 6));
    }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(100, value));
        repaint();
    }

    public int getValue() {
        return value;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        int height = 6;
        int y = (getHeight() - height) / 2;
        g.setColor(Theme.CARD_BORDER);
        g.fillRoundRect(0, y, getWidth(), height, height, height);
        int width = (int) Math.round(getWidth() * (value / 100.0));
        if (width > 0) {
            g.setColor(fillColor);
            g.fillRoundRect(0, y, width, height, height, height);
        }
        g.dispose();
    }
}
