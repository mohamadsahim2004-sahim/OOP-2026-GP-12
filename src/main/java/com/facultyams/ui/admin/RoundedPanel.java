package com.facultyams.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** White rounded surface with a thin border; used for cards, banners and the user chip. */
public class RoundedPanel extends JPanel {
    private static final int DEFAULT_RADIUS = 14;

    private final int radius;
    private Color fill = Color.WHITE;
    private Color line = AdminTheme.BORDER;

    public RoundedPanel() {
        this(new BorderLayout(), DEFAULT_RADIUS);
    }

    public RoundedPanel(LayoutManager layout) {
        this(layout, DEFAULT_RADIUS);
    }

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
    }

    /** Overrides the fill and border colours. */
    public RoundedPanel colors(Color fill, Color line) {
        this.fill = fill;
        this.line = line;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, w, h, radius, radius);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
