package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Day tab with a separate vector-painted today indicator. */
public final class PillTab extends JButton {
    private boolean selected;
    private final boolean today;

    public PillTab(String text, boolean selected, boolean today) {
        super(text);
        this.selected = selected;
        this.today = today;
        setFont(Theme.FONT_BODY_BOLD);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, today ? 23 : 14));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(selected ? Color.WHITE : Theme.TEXT_MUTED);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMinimumSize(new Dimension(0, Theme.CONTROL_HEIGHT));
        setRolloverEnabled(true);
    }

    public void setSelectedState(boolean selected) {
        this.selected = selected;
        setForeground(selected ? Color.WHITE : Theme.TEXT_MUTED);
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Theme.CONTROL_HEIGHT);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        if (selected) {
            g.setColor(Theme.PRIMARY);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        } else {
            g.setColor(getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g.setColor(Theme.CONTROL_BORDER);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
        }
        if (today) {
            g.setColor(selected ? Color.WHITE : Theme.PRIMARY);
            g.fillOval(getWidth() - 15, (getHeight() - 6) / 2, 6, 6);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
