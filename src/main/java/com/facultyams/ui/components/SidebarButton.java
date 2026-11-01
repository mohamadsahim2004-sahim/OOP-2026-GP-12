package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Theme-consistent sidebar navigation button with active and hover states. */
public final class SidebarButton extends JButton {
    private boolean active;

    public SidebarButton(String text, boolean active) {
        super(text);
        this.active = active;
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(Theme.FONT_BODY);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        updateForeground();
    }

    public void setActive(boolean active) {
        this.active = active;
        updateForeground();
        repaint();
    }
    private void updateForeground() { setForeground(active ? Color.WHITE : Theme.SIDEBAR_TEXT); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        if (active || getModel().isRollover()) {
            g2.setColor(active ? Theme.SIDEBAR_ACTIVE_BG : Theme.SIDEBAR_HOVER_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
