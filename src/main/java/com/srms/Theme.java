package com.srms;

import java.awt.*;
import javax.swing.border.Border;
import javax.swing.BorderFactory;

/**
 * Clean, minimal theme tokens for the SRMS Moodle learning platform.
 * Palette: primary #2563EB, 1px #E5E7EB borders, 12px radius, white cards.
 */
public final class Theme {
    private Theme() {}

    // Primary Colors
    public static final Color PRIMARY = new Color(37, 99, 235);        // #2563EB
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);   // #1D4ED8
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);// #EFF6FF Tint
    public static final Color PRIMARY_BORDER = new Color(191, 219, 254);// #BFDBFE

    // Neutral Surfaces & Borders
    public static final Color BG_MAIN = new Color(248, 250, 252);     // #F8FAFC
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(229, 231, 235);  // #E5E7EB
    public static final Color DIVIDER = new Color(243, 244, 246);      // #F3F4F6
    public static final Color HOVER_BG = new Color(249, 250, 251);     // #F9FAFB

    // Typography Colors
    public static final Color TEXT_MAIN = new Color(15, 23, 42);       // #0F172A
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // #64748B
    public static final Color TEXT_SUBTLE = new Color(148, 163, 184);  // #94A3B8

    // Status Colors
    public static final Color SUCCESS = new Color(22, 163, 74);        // #16A34A
    public static final Color SUCCESS_BG = new Color(240, 253, 244);   // #F0FDF4
    public static final Color WARNING = new Color(245, 158, 11);       // #F59E0B
    public static final Color WARNING_BG = new Color(254, 243, 199);   // #FEF3C7
    public static final Color DANGER = new Color(220, 38, 38);         // #DC2626
    public static final Color DANGER_BG = new Color(254, 242, 242);    // #FEF2F2
    public static final Color INFO = new Color(37, 99, 235);
    public static final Color INFO_BG = new Color(239, 246, 255);

    // Activity Colors
    public static final Color ANNOUNCEMENT_PURPLE = new Color(147, 51, 234);
    public static final Color RESOURCE_TEAL = new Color(13, 148, 136);
    public static final Color ASSIGNMENT_PINK = new Color(219, 39, 119);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TINY = new Font("Segoe UI", Font.BOLD, 11);

    public static void setAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Border createPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
