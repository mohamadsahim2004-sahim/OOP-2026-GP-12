package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Small rounded label for class types and live session state. */
public class Badge extends JLabel {
    private final Color tint;
    private final Color ink;

    public Badge(String text, Color tint, Color ink) {
        super(text);
        this.tint = tint;
        this.ink = ink;
        setFont(Theme.FONT_TINY.deriveFont(Font.BOLD));
        setForeground(ink);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        g2.setColor(tint);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}
