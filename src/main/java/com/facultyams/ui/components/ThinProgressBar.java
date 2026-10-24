package com.facultyams.ui.components;

import java.awt.*;
import javax.swing.JComponent;

import com.facultyams.Theme;

/**
 * A sleek 6px progress bar with a vertical marker indicating the minimum required attendance.
 */
public class ThinProgressBar extends JComponent {
    private double percent;
    private final double markerPercent;

    public ThinProgressBar(double percent, double markerPercent) {
        this.percent = percent;
        this.markerPercent = markerPercent;
        setPreferredSize(new Dimension(160, 16));
        setOpaque(false);
    }

    public void setPercent(double percent) {
        this.percent = percent;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();
        int barH = 6;
        int barY = (h - barH) / 2;

        // Track background
        g2.setColor(new Color(229, 231, 235)); // #E5E7EB
        g2.fillRoundRect(0, barY, w, barH, barH, barH);

        // Progress fill
        double clamped = Math.max(0, Math.min(100, percent));
        int fillW = (int) Math.round((clamped / 100.0) * w);

        if (fillW > 0) {
            Color fillCol;
            if (percent >= markerPercent) {
                fillCol = Theme.SUCCESS;
            } else if (percent >= markerPercent - 5.0) {
                fillCol = Theme.WARNING;
            } else {
                fillCol = Theme.DANGER;
            }
            g2.setColor(fillCol);
            g2.fillRoundRect(0, barY, fillW, barH, barH, barH);
        }

        // Marker indicator (at 80%)
        int markerX = (int) Math.round((markerPercent / 100.0) * w);
        int markerH = 12;
        int markerY = (h - markerH) / 2;

        g2.setColor(new Color(15, 23, 42)); // Dark marker line
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(markerX, markerY, markerX, markerY + markerH);

        g2.dispose();
    }
}
