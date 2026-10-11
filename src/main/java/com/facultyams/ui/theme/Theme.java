package com.facultyams.ui.theme;

import java.awt.*;
import javax.swing.border.Border;
import javax.swing.BorderFactory;

public class Theme {
    // Primary Brand Colors
    public static final Color PRIMARY = new Color(37, 99, 235);        // #2563EB Vibrant Blue
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);  // #1D4ED8 Darker Blue
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);// #EFF6FF Tint
    public static final Color PRIMARY_BORDER = new Color(191, 219, 254);// #BFDBFE

    // Sidebar & Navigation
    public static final Color SIDEBAR_BG = Color.WHITE;
    public static final Color SIDEBAR_ACTIVE_BG = new Color(37, 99, 235);
    public static final Color SIDEBAR_ACTIVE_TEXT = Color.WHITE;
    public static final Color SIDEBAR_TEXT = new Color(71, 85, 105);   // Slate 600
    public static final Color SIDEBAR_HOVER_BG = new Color(241, 245, 249); // Slate 100
    public static final Color LOGO_BLUE = new Color(29, 78, 216);

    // Backgrounds & Surfaces
    public static final Color BG_MAIN = new Color(245, 247, 251);     // Crisp soft dashboard grey
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(235, 238, 242);
    public static final Color CARD_BORDER_SUBTLE = new Color(241, 245, 249);

    // Text Colors
    public static final Color TEXT_MAIN = new Color(15, 23, 42);      // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // Slate 500
    public static final Color TEXT_SUBTLE = new Color(148, 163, 184); // Slate 400

    // Metric Accents
    public static final Color ACCENT_BLUE_BG = new Color(235, 243, 255);
    public static final Color ACCENT_BLUE = new Color(37, 99, 235);

    public static final Color ACCENT_GREEN_BG = new Color(236, 253, 245);
    public static final Color ACCENT_GREEN = new Color(16, 185, 129);

    public static final Color ACCENT_PURPLE_BG = new Color(243, 232, 255);
    public static final Color ACCENT_PURPLE = new Color(147, 51, 234);

    public static final Color ACCENT_AMBER_BG = new Color(254, 243, 199);
    public static final Color ACCENT_AMBER = new Color(245, 158, 11);

    public static final Color ACCENT_ROSE_BG = new Color(254, 226, 226);
    public static final Color ACCENT_ROSE = new Color(239, 68, 68);

    // Banner Gradient
    public static final Color BANNER_GRADIENT_START = new Color(237, 244, 255);
    public static final Color BANNER_GRADIENT_END = new Color(244, 248, 255);

    // Donut Chart Colors
    public static final Color CHART_EXCELLENT = new Color(16, 185, 129);  // Green 60%
    public static final Color CHART_GOOD = new Color(37, 99, 235);       // Blue 20%
    public static final Color CHART_AVERAGE = new Color(245, 158, 11);    // Amber 12%
    public static final Color CHART_NEEDS_IMP = new Color(239, 68, 68);   // Rose 8%

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_METRIC_SUB = new Font("Segoe UI", Font.PLAIN, 11);

    public static void setAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Border createEmptyPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
