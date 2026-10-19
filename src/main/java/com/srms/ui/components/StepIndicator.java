package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import javax.swing.JPanel;

/**
 * Step indicator component for 3-step wizards (Dates, Sessions, Details).
 */
public class StepIndicator extends JPanel {
    private final String[] stepLabels = { "1 Dates", "2 Courses & sessions", "3 Medical details" };
    private int currentStep = 1;

    public StepIndicator() {
        setPreferredSize(new Dimension(500, 48));
        setOpaque(false);
    }

    public void setStep(int step) {
        this.currentStep = step;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();
        int stepCount = stepLabels.length;
        int stepSpacing = w / (stepCount);

        for (int i = 0; i < stepCount; i++) {
            int cx = stepSpacing * i + stepSpacing / 2;
            int cy = 18;

            // Connecting line
            if (i < stepCount - 1) {
                int nextCx = stepSpacing * (i + 1) + stepSpacing / 2;
                g2.setColor(i + 1 < currentStep ? Theme.PRIMARY : new Color(229, 231, 235));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(cx + 16, cy, nextCx - 16, cy);
            }

            // Circle
            boolean isActive = (i + 1 == currentStep);
            boolean isCompleted = (i + 1 < currentStep);

            if (isActive) {
                g2.setColor(Theme.PRIMARY);
                g2.fillOval(cx - 12, cy - 12, 24, 24);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(String.valueOf(i + 1), cx - 4, cy + 5);
            } else if (isCompleted) {
                g2.setColor(Theme.SUCCESS);
                g2.fillOval(cx - 12, cy - 12, 24, 24);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString("✓", cx - 5, cy + 5);
            } else {
                g2.setColor(new Color(241, 245, 249));
                g2.fillOval(cx - 12, cy - 12, 24, 24);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - 12, cy - 12, 24, 24);
                g2.setColor(Theme.TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(String.valueOf(i + 1), cx - 4, cy + 5);
            }

            // Text Label
            g2.setFont(isActive ? Theme.FONT_BODY_BOLD : Theme.FONT_SMALL);
            g2.setColor(isActive ? Theme.PRIMARY : (isCompleted ? Theme.TEXT_MAIN : Theme.TEXT_MUTED));
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(stepLabels[i]);
            g2.drawString(stepLabels[i], cx - (strW / 2), cy + 24);
        }

        g2.dispose();
    }
}
