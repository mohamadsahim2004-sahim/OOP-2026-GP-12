package com.fot.ams.ui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Design system tokens defining colors, fonts, and geometry matching the reference image.
 */
public class ThemeColors {

    // Outer Background
    public static final Color WINDOW_BG = new Color(241, 243, 249); // Soft ambient lavender-grey

    // Main Card
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(230, 233, 242);
    public static final int CARD_CORNER_RADIUS = 28;

    // Left Hero Card Mesh / Linear Gradient Colors
    public static final Color HERO_DEEP_BLUE = new Color(18, 30, 135);     // Deep sapphire indigo
    public static final Color HERO_ROYAL_PURPLE = new Color(88, 48, 195);  // Vibrant purple
    public static final Color HERO_VIOLET = new Color(147, 86, 235);       // Soft violet
    public static final Color HERO_CYAN_BLOOM = new Color(86, 192, 245, 160); // Sky glow

    // Right Form Colors
    public static final Color TEXT_HEADLINE = new Color(15, 23, 42);   // Slate 900
    public static final Color TEXT_SUBTITLE = new Color(100, 116, 139); // Slate 500
    public static final Color TEXT_LABEL = new Color(51, 65, 85);      // Slate 700
    public static final Color TEXT_MUTED = new Color(148, 163, 184);   // Slate 400

    // Input Fields
    public static final Color INPUT_BG = new Color(250, 250, 252);
    public static final Color INPUT_BORDER = new Color(226, 232, 240);
    public static final Color INPUT_BORDER_FOCUS = new Color(99, 102, 241);
    public static final Color INPUT_FOCUS_GLOW = new Color(99, 102, 241, 40);

    // Primary Action Button (Gradient: Indigo to Purple)
    public static final Color BUTTON_GRADIENT_START = new Color(67, 56, 202);
    public static final Color BUTTON_GRADIENT_END = new Color(99, 102, 241);
    public static final Color BUTTON_HOVER_START = new Color(55, 48, 163);
    public static final Color BUTTON_HOVER_END = new Color(79, 70, 229);
    public static final Color BUTTON_SHADOW = new Color(99, 102, 241, 90);

    // Role Demo Badges
    public static final Color CHIP_BG = new Color(241, 245, 249);
    public static final Color CHIP_BORDER = new Color(226, 232, 240);
    public static final Color CHIP_HOVER_BG = new Color(238, 242, 255);
    public static final Color CHIP_TEXT = new Color(71, 85, 105);

    // Error Alert
    public static final Color ERROR_BG = new Color(254, 242, 242);
    public static final Color ERROR_BORDER = new Color(254, 202, 202);
    public static final Color ERROR_TEXT = new Color(220, 38, 38);

    // Fonts
    public static final Font FONT_HERO_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HERO_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADLINE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_CHIP = new Font("Segoe UI", Font.BOLD, 11);
}
