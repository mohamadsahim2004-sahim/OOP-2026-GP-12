package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import javax.swing.JButton;

/** Solid blue rounded action button with explicit hover and pressed states. */
public class PrimaryButton extends JButton {
    private boolean hovered;

    public PrimaryButton(String text) {
        super(text);
        setFont(Theme.COURSE_BODY_BOLD);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(180, Theme.CONTROL_HEIGHT));
        setBorder(Theme.createPadding(0, 16, 0, 16));
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
                : hovered ? Theme.PRIMARY_DARK : Theme.PRIMARY;
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
        g.dispose();
        super.paintComponent(graphics);
    }
}
