package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RoleBadgeButton provides quick-fill demo presets for evaluation.
 * Placed in the position of the social login pills from the reference image.
 */
public class RoleBadgeButton extends JButton {
    private boolean isHovered = false;
    private final int cornerRadius = 12;

    public RoleBadgeButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(ThemeColors.FONT_CHIP);
        setForeground(ThemeColors.CHIP_TEXT);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                setForeground(ThemeColors.BUTTON_GRADIENT_START);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                setForeground(ThemeColors.CHIP_TEXT);
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

        // Background
        g2.setColor(isHovered ? ThemeColors.CHIP_HOVER_BG : ThemeColors.CHIP_BG);
        g2.fillRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

        // Border
        g2.setColor(isHovered ? ThemeColors.INPUT_BORDER_FOCUS : ThemeColors.CHIP_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

        // Text
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
