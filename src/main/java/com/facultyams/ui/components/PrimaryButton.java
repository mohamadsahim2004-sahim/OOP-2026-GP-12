package com.facultyams.ui.components;

import java.awt.*;
import javax.swing.JButton;

import com.facultyams.ui.theme.Theme;

/** Solid blue rounded action button with explicit hover and pressed states. */
public class PrimaryButton extends JButton {
    private boolean hovered;

    public PrimaryButton(String text) {
        super(text);
        setFont(Theme.FONT_BODY_BOLD);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(180, 36));
        setBorder(Theme.createEmptyPadding(0, 16, 0, 16));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        Color fill = getModel().isPressed() ? new Color(30, 64, 175)
                : hovered ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
        g.dispose();
        super.paintComponent(graphics);
    }
}
