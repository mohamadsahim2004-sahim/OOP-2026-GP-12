package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public class ModernButton extends JButton {
    public enum StyleType {
        PRIMARY,
        OUTLINE,
        GHOST,
        DANGER,
        SUCCESS
    }

    private StyleType styleType;
    private int cornerRadius = 10;
    private boolean isHovered = false;

    public ModernButton(String text) {
        this(text, StyleType.PRIMARY);
    }

    public ModernButton(String text, StyleType styleType) {
        super(text);
        this.styleType = styleType;
        initButton();
    }

    private void initButton() {
        setFont(Theme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(Theme.createEmptyPadding(8, 16, 8, 16));

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
        });
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int width = getWidth();
        int height = getHeight();

        Color bg = Theme.PRIMARY;
        Color fg = Color.WHITE;
        Color border = null;

        switch (styleType) {
            case PRIMARY:
                bg = isHovered ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
                fg = Color.WHITE;
                break;
            case OUTLINE:
                bg = isHovered ? Theme.PRIMARY_LIGHT : Color.WHITE;
                fg = Theme.PRIMARY;
                border = isHovered ? Theme.PRIMARY : Theme.CARD_BORDER;
                break;
            case GHOST:
                bg = isHovered ? new Color(241, 245, 249) : new Color(0, 0, 0, 0);
                fg = isHovered ? Theme.TEXT_MAIN : Theme.TEXT_MUTED;
                break;
            case DANGER:
                bg = isHovered ? new Color(225, 29, 72) : new Color(244, 63, 94);
                fg = Color.WHITE;
                break;
            case SUCCESS:
                bg = isHovered ? new Color(21, 128, 61) : new Color(34, 197, 94);
                fg = Color.WHITE;
                break;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        setForeground(fg);
        g2.dispose();
        super.paintComponent(g);
    }
}
