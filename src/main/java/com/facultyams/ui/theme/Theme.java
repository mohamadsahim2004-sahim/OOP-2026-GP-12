package com.facultyams.ui.theme;

import java.awt.*;
import javax.swing.border.Border;
import javax.swing.BorderFactory;

public class Theme {
    public static final int GRID = 8;
    public static final int PAGE_PADDING = 24;
    public static final int CARD_GAP = 12;
    public static final int CARD_PADDING = 16;
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int CARD_RADIUS = 12;
    public static final int BUTTON_RADIUS = 8;
    public static final int CONTROL_HEIGHT = 36;
    public static final Font FONT_TINY = new Font("Segoe UI", Font.PLAIN, 10);
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
    public static final Color BG_MAIN = new Color(245, 247, 251);
    public static final Color TIMETABLE_BG = new Color(244, 246, 251);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(235, 238, 242);
    public static final Color TIMETABLE_CARD_BORDER = new Color(230, 234, 242);
    public static final Color CARD_BORDER_SUBTLE = new Color(241, 245, 249);
    public static final Color CONTROL_BORDER = new Color(214, 220, 232);
    public static final Color TOPBAR_FIELD_BG = new Color(248, 250, 252);
    public static final Color NOTIFICATION_RED = new Color(220, 38, 38);
    public static final Color SHADOW = new Color(15, 23, 42, 10);

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
    public static final Color ACCENT_GRAY_BG = new Color(241, 245, 249);
    public static final Color ACCENT_GRAY = new Color(100, 116, 139);
    public static final Color TYPE_PRACTICAL_BG = new Color(243, 232, 255);
    public static final Color TYPE_TUTORIAL_BG = new Color(254, 243, 199);
    public static final Color TYPE_PERSONAL_BG = new Color(236, 253, 245);
    public static final Color TYPE_TUTORIAL = new Color(180, 83, 9);
    public static final Color TYPE_PERSONAL = new Color(5, 150, 105);

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
    public static final Font TIMETABLE_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font TIMETABLE_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_METRIC_SUB = new Font("Segoe UI", Font.PLAIN, 11);

    public static void setAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Border createEmptyPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
