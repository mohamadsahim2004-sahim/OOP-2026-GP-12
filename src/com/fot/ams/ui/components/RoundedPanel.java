package com.fot.ams.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A JPanel that paints with anti-aliased rounded corners, optional border, and background color.
 */
public class RoundedPanel extends JPanel {
    private int cornerRadius;
    private Color backgroundColor;
    private Color borderColor;
    private int borderWidth;

    public RoundedPanel(int radius, Color bgColor) {
        this(radius, bgColor, null, 0);
    }

    public RoundedPanel(int radius, Color bgColor, Color borderColor, int borderWidth) {
        super();
        this.cornerRadius = radius;
        this.backgroundColor = bgColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        setOpaque(false);
    }

    public void setBackgroundColor(Color bgColor) {
        this.backgroundColor = bgColor;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw background
        if (backgroundColor != null) {
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        }

        // Draw border if set
        if (borderColor != null && borderWidth > 0) {
            g2.setColor(borderColor);
            g2.setStroke(new java.awt.BasicStroke(borderWidth));
            g2.drawRoundRect(borderWidth / 2, borderWidth / 2, 
                             width - borderWidth, height - borderWidth, 
                             cornerRadius, cornerRadius);
        }

        g2.dispose();
    }
}
