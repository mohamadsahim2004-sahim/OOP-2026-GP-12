package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.JLabel;

public class BadgeLabel extends JLabel {
    private Color badgeBackground;
    private Color badgeForeground;
    private int cornerRadius = 12;

    public BadgeLabel(String text, Color bg, Color fg) {
        super(text, CENTER);
        this.badgeBackground = bg;
        this.badgeForeground = fg;
        setForeground(fg);
        setFont(Theme.FONT_BODY_BOLD);
        setOpaque(false);
        setBorder(Theme.createEmptyPadding(3, 10, 3, 10));
    }

    public static BadgeLabel createGradeBadge(String grade) {
        Color bg;
        Color fg;
        if (grade == null) grade = "N/A";
        String g = grade.trim().toUpperCase();

        if (g.startsWith("A")) {
            bg = new Color(220, 252, 231); // Emerald tint
            fg = new Color(22, 163, 74);   // Dark green
        } else if (g.startsWith("B")) {
            bg = new Color(219, 234, 254); // Blue tint
            fg = new Color(37, 99, 235);   // Blue
        } else if (g.startsWith("C")) {
            bg = new Color(254, 243, 199); // Amber tint
            fg = new Color(217, 119, 6);   // Amber
        } else {
            bg = new Color(254, 226, 226); // Rose tint
            fg = new Color(225, 29, 72);   // Rose
        }
        return new BadgeLabel(grade, bg, fg);
    }

    public static BadgeLabel createStatusBadge(String status) {
        Color bg;
        Color fg;
        if (status == null) status = "Unknown";
        String s = status.trim().toLowerCase();

        if (s.contains("approved") || s.contains("enrolled") || s.contains("eligible") || s.contains("pass")) {
            bg = new Color(220, 252, 231);
            fg = new Color(22, 163, 74);
        } else if (s.contains("pending") || s.contains("review") || s.contains("average")) {
            bg = new Color(254, 243, 199);
            fg = new Color(217, 119, 6);
        } else if (s.contains("rejected") || s.contains("at risk") || s.contains("failed") || s.contains("drop")) {
            bg = new Color(254, 226, 226);
            fg = new Color(225, 29, 72);
        } else {
            bg = new Color(241, 245, 249);
            fg = new Color(71, 85, 105);
        }
        return new BadgeLabel(status, bg, fg);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        g2.setColor(badgeBackground);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        g2.dispose();
        super.paintComponent(g);
    }
}
