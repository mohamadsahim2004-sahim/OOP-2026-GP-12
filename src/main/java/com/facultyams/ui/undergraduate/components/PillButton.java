package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Compact toggle button used for timetable day selection. */
public final class PillButton extends JButton {
    private boolean selected;

    public PillButton(String text, boolean selected) {
        super(text);
        this.selected = selected;
        setFont(Theme.FONT_BODY_BOLD);
        setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(selected ? Color.WHITE : Theme.TEXT_MUTED);
        addChangeListener(e -> repaint());
    }

    public void setSelectedState(boolean selected) {
        this.selected = selected;
        setForeground(selected ? Color.WHITE : Theme.TEXT_MUTED);
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        Color bg = selected ? Theme.PRIMARY : getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        if (!selected) {
            g2.setColor(Theme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
