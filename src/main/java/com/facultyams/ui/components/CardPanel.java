package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.JPanel;

public class CardPanel extends JPanel {
    private int cornerRadius = 16;
    private Color backgroundColor = Theme.CARD_BG;
    private Color borderColor = Theme.CARD_BORDER;
    private boolean drawBorder = true;

    public CardPanel() {
        this(16);
    }

    public CardPanel(int radius) {
        this.cornerRadius = radius;
        setOpaque(false);
    }

    public CardPanel(int radius, Color bg) {
        this.cornerRadius = radius;
        this.backgroundColor = bg;
        setOpaque(false);
    }

    public void setBackgroundColor(Color bg) {
        this.backgroundColor = bg;
        repaint();
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
        repaint();
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int width = getWidth();
        int height = getHeight();

        // Background fill
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // Subtle border
        if (drawBorder) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
