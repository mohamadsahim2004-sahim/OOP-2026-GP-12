package com.facultyams.ui.technical;

import com.facultyams.ui.admin.AdminTheme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small bar chart drawn with Java2D (no chart library); every bar is labelled with its value. */
final class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color[] COLORS = {
            AdminTheme.CARD_BLUE, AdminTheme.CARD_GREEN, AdminTheme.CARD_ORANGE, AdminTheme.CARD_PURPLE};

    private Map<String, Integer> data = new LinkedHashMap<>();
    private String title = "";

    BarChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(320, 170));
        setMinimumSize(new Dimension(200, 130));
        putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
    }

    void setData(String title, Map<String, Integer> data) {
        this.title = title == null ? "" : title;
        this.data = new LinkedHashMap<>(data);
        StringBuilder text = new StringBuilder(this.title + ": ");
        data.forEach((k, v) -> text.append(k).append(' ').append(v).append("; "));
        getAccessibleContext().setAccessibleName(text.toString());
        setToolTipText(text.toString());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setFont(AdminTheme.FONT_BOLD);
        g2.setColor(AdminTheme.TEXT);
        g2.drawString(title, 8, 18);
        if (data.isEmpty()) {
            g2.dispose();
            return;
        }
        int max = Math.max(1, data.values().stream().mapToInt(Integer::intValue).max().orElse(1));
        int left = 8;
        int top = 32;
        int bottom = h - 24;
        int slot = (w - 2 * left) / data.size();
        int barW = Math.max(14, Math.min(60, slot - 14));
        g2.setFont(AdminTheme.FONT);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(AdminTheme.BORDER);
        g2.drawLine(left, bottom, w - left, bottom);
        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int barH = (int) Math.round((bottom - top - 16) * (e.getValue() / (double) max));
            int x = left + i * slot + (slot - barW) / 2;
            g2.setColor(COLORS[i % COLORS.length]);
            g2.fillRoundRect(x, bottom - barH, barW, barH, 6, 6);
            g2.setColor(AdminTheme.TEXT);
            String value = String.valueOf(e.getValue());
            g2.drawString(value, x + (barW - fm.stringWidth(value)) / 2, bottom - barH - 4);
            String label = e.getKey();
            while (label.length() > 3 && fm.stringWidth(label) > slot - 4) {
                label = label.substring(0, label.length() - 2);
            }
            g2.setColor(AdminTheme.TEXT_MUTED);
            g2.drawString(label, left + i * slot + (slot - fm.stringWidth(label)) / 2, bottom + 16);
            i++;
        }
        g2.dispose();
    }
}