package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class DonutChartPanel extends JPanel {

    public DonutChartPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(16, 0));

        JPanel chartCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();
                int size = Math.min(w, h) - 16;
                int x = (w - size) / 2;
                int y = (h - size) / 2;

                int strokeWidth = 18;
                int cx = x + size / 2;
                int cy = y + size / 2;

                // Segments: 60% Green, 20% Blue, 12% Amber, 8% Rose
                // Total = 360 degrees
                double[] values = {60, 20, 12, 8};
                Color[] colors = {
                    Theme.CHART_EXCELLENT,
                    Theme.CHART_GOOD,
                    Theme.CHART_AVERAGE,
                    Theme.CHART_NEEDS_IMP
                };

                double startAngle = 90; // Start at top
                for (int i = 0; i < values.length; i++) {
                    double angle = (values[i] / 100.0) * 360.0;
                    g2.setColor(colors[i]);
                    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

                    // Draw arc
                    Arc2D.Double arc = new Arc2D.Double(
                        x + strokeWidth / 2.0,
                        y + strokeWidth / 2.0,
                        size - strokeWidth,
                        size - strokeWidth,
                        startAngle,
                        -angle + 1.5, // tiny gap
                        Arc2D.OPEN
                    );
                    g2.draw(arc);
                    startAngle -= angle;
                }

                // Center Text: 84%
                g2.setColor(Theme.TEXT_MAIN);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                String mainText = "84%";
                int tx = cx - fm.stringWidth(mainText) / 2;
                int ty = cy - 2;
                g2.drawString(mainText, tx, ty);

                // Center Subtitle: Overall Performance
                g2.setColor(Theme.TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                FontMetrics fmSub = g2.getFontMetrics();
                String subText = "Overall";
                int sx1 = cx - fmSub.stringWidth(subText) / 2;
                g2.drawString(subText, sx1, ty + 14);

                String subText2 = "Performance";
                int sx2 = cx - fmSub.stringWidth(subText2) / 2;
                g2.drawString(subText2, sx2, ty + 26);

                g2.dispose();
            }
        };
        chartCanvas.setOpaque(false);
        chartCanvas.setPreferredSize(new Dimension(140, 140));

        // Legend panel
        JPanel legendPanel = new JPanel();
        legendPanel.setOpaque(false);
        legendPanel.setLayout(new GridLayout(4, 1, 0, 8));

        legendPanel.add(createLegendItem("Excellent", "60%", Theme.CHART_EXCELLENT));
        legendPanel.add(createLegendItem("Good", "20%", Theme.CHART_GOOD));
        legendPanel.add(createLegendItem("Average", "12%", Theme.CHART_AVERAGE));
        legendPanel.add(createLegendItem("Needs Improvement", "8%", Theme.CHART_NEEDS_IMP));

        add(chartCanvas, BorderLayout.WEST);
        add(legendPanel, BorderLayout.CENTER);
    }

    private JPanel createLegendItem(String label, String pct, Color color) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        // Dot
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(color);
                g2.fillOval(2, 5, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 18));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MAIN);

        JLabel pctLbl = new JLabel(pct);
        pctLbl.setFont(Theme.FONT_BODY_BOLD);
        pctLbl.setForeground(Theme.TEXT_MAIN);

        p.add(dot, BorderLayout.WEST);
        p.add(lbl, BorderLayout.CENTER);
        p.add(pctLbl, BorderLayout.EAST);
        return p;
    }
}
