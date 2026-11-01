package com.facultyams;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.swing.border.Border;

/** Compatibility names for older student views; the student UI theme is defined in one place. */
public final class Theme {
    private Theme() {}

    public static final int PAGE_PADDING = com.facultyams.ui.theme.Theme.PAGE_PADDING;
    public static final int COURSE_CARD_GAP = 24;
    public static final int COURSE_ROW_GAP = 12;
    public static final int COURSE_ICON_GAP = 12;
    public static final int COURSE_ROW_PADDING = 16;
    public static final int SECTION_GAP = com.facultyams.ui.theme.Theme.CARD_GAP;
    public static final int CARD_GAP = com.facultyams.ui.theme.Theme.CARD_GAP;
    public static final int CARD_PADDING = com.facultyams.ui.theme.Theme.CARD_PADDING;
    public static final int CONTROL_GAP = com.facultyams.ui.theme.Theme.SPACE_3;
    public static final int SPACE_1 = com.facultyams.ui.theme.Theme.SPACE_1;
    public static final int SPACE_2 = com.facultyams.ui.theme.Theme.SPACE_2;
    public static final int SPACE_3 = com.facultyams.ui.theme.Theme.SPACE_3;
    public static final int CONTROL_HEIGHT = com.facultyams.ui.theme.Theme.CONTROL_HEIGHT;
    public static final int RADIUS_CARD = com.facultyams.ui.theme.Theme.CARD_RADIUS;
    public static final int RADIUS_CONTROL = com.facultyams.ui.theme.Theme.BUTTON_RADIUS;
    public static final int DEADLINE_ROW_HEIGHT = 64;
    public static final int DEADLINE_CARD_PADDING = com.facultyams.ui.theme.Theme.CARD_PADDING;

    public static final Color COURSE_PAGE_BG = com.facultyams.ui.theme.Theme.BG_MAIN;
    public static final Color COURSE_TEXT_MAIN = com.facultyams.ui.theme.Theme.TEXT_MAIN;
    public static final Color COURSE_TEXT_MUTED = com.facultyams.ui.theme.Theme.TEXT_MUTED;
    public static final Color PRIMARY = com.facultyams.ui.theme.Theme.PRIMARY;
    public static final Color PRIMARY_DARK = com.facultyams.ui.theme.Theme.PRIMARY_HOVER;
    public static final Color PRIMARY_LIGHT = com.facultyams.ui.theme.Theme.PRIMARY_LIGHT;
    public static final Color PRIMARY_BORDER = com.facultyams.ui.theme.Theme.PRIMARY_BORDER;
    public static final Color BG_MAIN = com.facultyams.ui.theme.Theme.BG_MAIN;
    public static final Color CARD_BG = com.facultyams.ui.theme.Theme.CARD_BG;
    public static final Color CARD_BORDER = com.facultyams.ui.theme.Theme.CARD_BORDER;
    public static final Color DIVIDER = com.facultyams.ui.theme.Theme.CARD_BORDER_SUBTLE;
    public static final Color HOVER_BG = com.facultyams.ui.theme.Theme.ENROLLMENT_ROW_HOVER;
    public static final Color ENROLLMENT_ROW_HOVER = com.facultyams.ui.theme.Theme.ENROLLMENT_ROW_HOVER;
    public static final Color TOPBAR_FIELD_BG = com.facultyams.ui.theme.Theme.TOPBAR_FIELD_BG;
    public static final Color ACCENT_GRAY_BG = com.facultyams.ui.theme.Theme.ACCENT_GRAY_BG;
    public static final Color ACCENT_GREEN_BG = com.facultyams.ui.theme.Theme.ACCENT_GREEN_BG;
    public static final Color TEXT_MAIN = com.facultyams.ui.theme.Theme.TEXT_MAIN;
    public static final Color TEXT_MUTED = com.facultyams.ui.theme.Theme.TEXT_MUTED;
    public static final Color TEXT_SUBTLE = com.facultyams.ui.theme.Theme.TEXT_SUBTLE;
    public static final Color SUCCESS = com.facultyams.ui.theme.Theme.SUCCESS;
    public static final Color SUCCESS_BG = com.facultyams.ui.theme.Theme.SUCCESS_BG;
    public static final Color WARNING = com.facultyams.ui.theme.Theme.WARNING;
    public static final Color WARNING_BG = com.facultyams.ui.theme.Theme.WARNING_BG;
    public static final Color DANGER = com.facultyams.ui.theme.Theme.DANGER;
    public static final Color DANGER_BG = com.facultyams.ui.theme.Theme.DANGER_BG;
    public static final Color INFO = com.facultyams.ui.theme.Theme.INFO;
    public static final Color INFO_BG = com.facultyams.ui.theme.Theme.INFO_BG;
    public static final Color ANNOUNCEMENT_PURPLE = com.facultyams.ui.theme.Theme.PRIMARY;
    public static final Color RESOURCE_TEAL = com.facultyams.ui.theme.Theme.PRIMARY;
    public static final Color ASSIGNMENT_PINK = com.facultyams.ui.theme.Theme.PRIMARY;
    public static final Font FONT_TITLE = com.facultyams.ui.theme.Theme.FONT_TITLE;
    public static final Font FONT_SECTION = com.facultyams.ui.theme.Theme.FONT_SECTION;
    public static final Font FONT_SUBTITLE = com.facultyams.ui.theme.Theme.FONT_SUBTITLE;
    public static final Font FONT_BODY_BOLD = com.facultyams.ui.theme.Theme.FONT_BODY_BOLD;
    public static final Font FONT_BODY = com.facultyams.ui.theme.Theme.FONT_BODY;
    public static final Font COURSE_BODY = com.facultyams.ui.theme.Theme.ENROLLMENT_BODY;
    public static final Font COURSE_BODY_BOLD = com.facultyams.ui.theme.Theme.ENROLLMENT_BODY_BOLD;
    public static final Font FONT_SMALL = com.facultyams.ui.theme.Theme.FONT_SMALL;
    public static final Font FONT_TINY = com.facultyams.ui.theme.Theme.FONT_TINY;

    public static void setAntiAliasing(Graphics2D g2) {
        com.facultyams.ui.theme.Theme.setAntiAliasing(g2);
    }

    public static Border createPadding(int top, int left, int bottom, int right) {
        return com.facultyams.ui.theme.Theme.createEmptyPadding(top, left, bottom, right);
    }
}
