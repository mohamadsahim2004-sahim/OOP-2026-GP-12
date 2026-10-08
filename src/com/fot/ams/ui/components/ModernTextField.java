package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * ModernTextField with rounded corners, focus glow, and placeholder text.
 */
public class ModernTextField extends JTextField {
    private String placeholder;
    private boolean isFocused = false;
    private final int cornerRadius = 12;

    public ModernTextField(String placeholder) {
        this.placeholder = placeholder;
        setOpaque(false);
        setFont(ThemeColors.FONT_INPUT);
        setForeground(ThemeColors.TEXT_HEADLINE);
        setCaretColor(ThemeColors.INPUT_BORDER_FOCUS);
        setBorder(new EmptyBorder(10, 14, 10, 14));

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Background
        g2.setColor(ThemeColors.INPUT_BG);
        g2.fillRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

        // 2. Draw Focus Ring or Normal Border
        if (isFocused) {
            // Soft glow ring
            g2.setColor(ThemeColors.INPUT_FOCUS_GLOW);
            g2.setStroke(new BasicStroke(3.5f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius, cornerRadius);

            g2.setColor(ThemeColors.INPUT_BORDER_FOCUS);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius, cornerRadius);
        } else {
            g2.setColor(ThemeColors.INPUT_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius, cornerRadius);
        }

        g2.dispose();

        // Paint the actual text content
        super.paintComponent(g);

        // Draw placeholder when empty and not focused (or empty)
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D gp = (Graphics2D) g.create();
            gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gp.setColor(ThemeColors.TEXT_MUTED);
            gp.setFont(getFont());
            Insets insets = getInsets();
            int y = (h - gp.getFontMetrics().getHeight()) / 2 + gp.getFontMetrics().getAscent();
            gp.drawString(placeholder, insets.left, y);
            gp.dispose();
        }
    }
}
