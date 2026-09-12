package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * ModernPasswordField with rounded corners, focus glow, placeholder text,
 * and an interactive Eye toggle icon to reveal/hide password.
 */
public class ModernPasswordField extends JPasswordField {
    private String placeholder;
    private boolean isFocused = false;
    private boolean isPasswordVisible = false;
    private final int cornerRadius = 12;
    private final char defaultEchoChar;
    private boolean isEyeHovered = false;

    public ModernPasswordField(String placeholder) {
        this.placeholder = placeholder;
        this.defaultEchoChar = getEchoChar();
        setOpaque(false);
        setFont(ThemeColors.FONT_INPUT);
        setForeground(ThemeColors.TEXT_HEADLINE);
        setCaretColor(ThemeColors.INPUT_BORDER_FOCUS);
        // Extra right padding so text doesn't overlap eye icon
        setBorder(new EmptyBorder(10, 14, 10, 42));

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

        // Toggle eye on click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isOverEyeIcon(e.getX(), e.getY())) {
                    togglePasswordVisibility();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean over = isOverEyeIcon(e.getX(), e.getY());
                if (over != isEyeHovered) {
                    isEyeHovered = over;
                    setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    repaint();
                }
            }
        });
    }

    private boolean isOverEyeIcon(int x, int y) {
        int iconRightMargin = 12;
        int iconSize = 22;
        int eyeX = getWidth() - iconRightMargin - iconSize;
        int eyeY = (getHeight() - iconSize) / 2;
        return x >= eyeX && x <= eyeX + iconSize && y >= eyeY && y <= eyeY + iconSize;
    }

    public void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            setEchoChar((char) 0);
        } else {
            setEchoChar(defaultEchoChar != 0 ? defaultEchoChar : '•');
        }
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

        // Paint text
        super.paintComponent(g);

        // Draw placeholder if empty
        if (getPassword().length == 0 && placeholder != null) {
            Graphics2D gp = (Graphics2D) g.create();
            gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gp.setColor(ThemeColors.TEXT_MUTED);
            gp.setFont(getFont());
            Insets insets = getInsets();
            int y = (h - gp.getFontMetrics().getHeight()) / 2 + gp.getFontMetrics().getAscent();
            gp.drawString(placeholder, insets.left, y);
            gp.dispose();
        }

        // Draw Eye Icon on right side
        Graphics2D ge = (Graphics2D) g.create();
        ge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawEyeIcon(ge, w, h);
        ge.dispose();
    }

    private void drawEyeIcon(Graphics2D g2, int w, int h) {
        int iconSize = 18;
        int eyeX = w - 32;
        int eyeY = (h - iconSize) / 2;

        Color iconColor = isEyeHovered ? ThemeColors.INPUT_BORDER_FOCUS : ThemeColors.TEXT_MUTED;
        g2.setColor(iconColor);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw eye outline (upper & lower arcs)
        g2.drawArc(eyeX, eyeY + 2, iconSize, 12, 0, 180);
        g2.drawArc(eyeX, eyeY + 2, iconSize, 12, 180, 180);

        // Pupil circle
        if (!isPasswordVisible) {
            // Closed/Masked or normal pupil
            g2.fillOval(eyeX + 6, eyeY + 5, 6, 6);
        } else {
            // Active visible pupil
            g2.fillOval(eyeX + 6, eyeY + 5, 6, 6);
            // Diagonal slash line across the eye
            g2.drawLine(eyeX - 1, eyeY + 14, eyeX + iconSize + 1, eyeY + 2);
        }
    }
}
