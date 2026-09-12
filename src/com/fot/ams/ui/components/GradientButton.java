package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * GradientButton paints a smooth royal-violet gradient action button
 * with hover elevation and drop glow effect matching the reference image.
 */
public class GradientButton extends JButton {
    private boolean isHovered = false;
    private boolean isPressed = false;
    private final int cornerRadius = 14;

    public GradientButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(ThemeColors.FONT_BUTTON);
        setForeground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Subtle Bottom Shadow Glow
        int shadowOffset = isPressed ? 1 : (isHovered ? 4 : 3);
        g2.setColor(new Color(99, 102, 241, isHovered ? 85 : 55));
        g2.fillRoundRect(2, shadowOffset, w - 4, h - 3, cornerRadius, cornerRadius);

        // 2. Determine Gradient Colors based on mouse state
        Color startCol = isPressed ? ThemeColors.BUTTON_HOVER_START : (isHovered ? ThemeColors.BUTTON_HOVER_END : ThemeColors.BUTTON_GRADIENT_START);
        Color endCol = isPressed ? ThemeColors.BUTTON_HOVER_END : (isHovered ? new Color(129, 140, 248) : ThemeColors.BUTTON_GRADIENT_END);

        LinearGradientPaint gradient = new LinearGradientPaint(
            new Point2D.Float(0, 0),
            new Point2D.Float(w, 0),
            new float[]{0.0f, 1.0f},
            new Color[]{startCol, endCol}
        );
        g2.setPaint(gradient);

        int btnY = isPressed ? 2 : 0;
        int btnH = h - 3;
        g2.fillRoundRect(0, btnY, w, btnH, cornerRadius, cornerRadius);

        // 3. Draw Button Text
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (btnH - fm.getHeight()) / 2 + fm.getAscent() + btnY;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
