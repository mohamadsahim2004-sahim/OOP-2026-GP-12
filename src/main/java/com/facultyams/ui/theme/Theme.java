package com.facultyams.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.BorderFactory;

public class Theme {
    public static final int GRID = 8;
    public static final int PAGE_PADDING = 32;
    public static final int CARD_GAP = 16;
    public static final int CARD_PADDING = 24;
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int CARD_RADIUS = 8;
    public static final int BUTTON_RADIUS = 6;
    public static final int CONTROL_HEIGHT = 40;
    public static final int PROFILE_PAGE_PADDING = 24;
    public static final int PROFILE_CARD_GAP = 16;
    public static final int PROFILE_CARD_PADDING = 24;
    public static final int PROFILE_SECTION_GAP = 32;
    public static final int PROFILE_FIELD_ROW_GAP = 20;
    public static final int PROFILE_COLUMN_GAP = 24;
    public static final int PROFILE_LABEL_GAP = 6;
    public static final int PROFILE_FIELD_HEIGHT = 44;
    public static final int PROFILE_FIELD_INSET = 12;
    public static final int ENROLLMENT_PAGE_PADDING = 24;
    public static final int ENROLLMENT_SECTION_GAP = 16;
    public static final int ENROLLMENT_CARD_PADDING = 24;
    public static final int ENROLLMENT_ROW_HEIGHT = 52;
    public static final int ENROLLMENT_HEADER_HEIGHT = 40;
    public static final int ENROLLMENT_CELL_PADDING_X = 16;
    public static final int ENROLLMENT_CONTROL_HEIGHT = 40;
    public static final int ENROLLMENT_BUTTON_HEIGHT = 32;
    public static final int ENROLLMENT_CARD_RADIUS = 8;
    public static final int ENROLLMENT_CONTROL_RADIUS = 8;
    public static final int NOTICE_DIALOG_PADDING = 24;
    public static final int NOTICE_DIALOG_SECTION_GAP = 16;
    public static final int NOTICE_DIALOG_ELEMENT_GAP = 8;
    public static final int NOTICE_DIALOG_CONTROL_HEIGHT = 40;
    public static final int NOTICE_DIALOG_RADIUS = 8;
    public static final int NOTICE_DIALOG_CONTROL_RADIUS = 8;
    public static final int NOTICE_DIALOG_WIDTH = 560;
    public static final int NOTICE_DIALOG_BODY_MAX_HEIGHT = 240;
    public static final int NOTICE_DIALOG_SCROLLBAR_WIDTH = 8;
    public static final Color NOTICE_DIALOG_BORDER = new Color(232, 232, 236);
    public static final Color NOTICE_DIALOG_BODY = new Color(55, 65, 81);
    public static final Color NOTICE_DIALOG_HOVER = new Color(244, 244, 245);
    public static final Color NOTICE_DIALOG_SECONDARY_HOVER = new Color(250, 250, 250);
    public static final Color NOTICE_DIALOG_ATTACHMENT_BG = new Color(250, 250, 250);
    public static final Color NOTICE_DIALOG_SCROLLBAR = new Color(209, 213, 219);
    public static final Color NOTICE_DIALOG_SHADOW = new Color(15, 23, 42, 0);
    public static final Color NOTICE_DIALOG_PRESSED = new Color(30, 64, 175);
    public static final Color NOTICE_DIALOG_AMBER = new Color(180, 83, 9);
    public static final Color NOTICE_DIALOG_AMBER_BG = new Color(255, 251, 235);
    public static final Font FONT_TINY = new Font("Segoe UI", Font.PLAIN, 10);
    // Primary Brand Colors
    public static final Color PRIMARY = new Color(37, 99, 235);        // #2563EB Vibrant Blue
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);  // #1D4ED8 Darker Blue
    public static final Color PRIMARY_LIGHT = new Color(239, 244, 255);// #EFF4FF Tint
    public static final Color PRIMARY_BORDER = new Color(232, 232, 236);// #E8E8EC
    public static final Color SUCCESS = new Color(21, 128, 61);
    public static final Color SUCCESS_BG = new Color(240, 253, 244);
    public static final Color WARNING = new Color(180, 83, 9);
    public static final Color WARNING_BG = new Color(255, 251, 235);
    public static final Color DANGER = new Color(185, 28, 28);
    public static final Color DANGER_BG = new Color(254, 242, 242);
    public static final Color INFO = PRIMARY;
    public static final Color INFO_BG = PRIMARY_LIGHT;

    // Sidebar & Navigation
    public static final Color SIDEBAR_BG = new Color(13, 39, 67);
    public static final Color SIDEBAR_ACTIVE_BG = new Color(37, 99, 235);
    public static final Color SIDEBAR_ACTIVE_TEXT = Color.WHITE;
    public static final Color SIDEBAR_TEXT = new Color(210, 222, 237);
    public static final Color SIDEBAR_HOVER_BG = new Color(31, 62, 94);
    public static final Color SIDEBAR_BORDER = new Color(43, 69, 97);
    public static final Color SIDEBAR_BRAND = Color.WHITE;
    public static final Color LOGO_BLUE = new Color(29, 78, 216);

    // Backgrounds & Surfaces
    public static final Color BG_MAIN = new Color(250, 250, 250);
    public static final Color TIMETABLE_BG = new Color(250, 250, 250);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(232, 232, 236);
    public static final Color TIMETABLE_CARD_BORDER = new Color(232, 232, 236);
    public static final Color CARD_BORDER_SUBTLE = new Color(232, 232, 236);
    public static final Color CONTROL_BORDER = new Color(232, 232, 236);
    public static final Color TOPBAR_FIELD_BG = new Color(244, 244, 245);
    public static final Color NOTIFICATION_RED = new Color(185, 28, 28);
    public static final Color SHADOW = new Color(15, 23, 42, 0);

    // Text Colors
    public static final Color TEXT_MAIN = new Color(17, 24, 39);      // #111827
    public static final Color TEXT_MUTED = new Color(107, 114, 128);  // #6B7280
    public static final Color TEXT_SUBTLE = new Color(161, 161, 170); // #A1A1AA
    public static final Color ENROLLMENT_TEXT = new Color(17, 24, 39);
    public static final Color ENROLLMENT_MUTED = new Color(107, 114, 128);
    public static final Color ENROLLMENT_BORDER = new Color(232, 232, 236);
    public static final Color ENROLLMENT_ROW_HOVER = new Color(250, 250, 250);
    public static final Color ENROLLMENT_SUCCESS = new Color(21, 128, 61);
    public static final Color ENROLLMENT_SUCCESS_BG = new Color(240, 253, 244);
    public static final Color ENROLLMENT_DANGER = new Color(185, 28, 28);
    public static final Color ENROLLMENT_DANGER_BG = new Color(254, 242, 242);
    public static final Color ENROLLMENT_WARNING = new Color(180, 83, 9);
    public static final Color ENROLLMENT_WARNING_BG = new Color(255, 251, 235);
    public static final Color ENROLLMENT_CORE_BG = new Color(239, 244, 255);
    public static final Color ENROLLMENT_CORE_TEXT = new Color(37, 99, 235);
    public static final Color ENROLLMENT_ELECTIVE_BG = new Color(244, 244, 245);
    public static final Color ENROLLMENT_ELECTIVE_TEXT = new Color(107, 114, 128);
    public static final Color TABLE_HOVER = new Color(250, 250, 250);
    public static final Color BADGE_GRAY_BG = new Color(244, 244, 245);
    public static final Color BADGE_GRAY_TEXT = new Color(82, 82, 91);
    public static final Color BADGE_BLUE_BG = PRIMARY_LIGHT;
    public static final Color BADGE_BLUE_TEXT = PRIMARY;
    public static final Color BADGE_AMBER_BG = WARNING_BG;
    public static final Color BADGE_AMBER_TEXT = WARNING;
    public static final Color FILE_PDF = new Color(13, 148, 136);
    public static final Color FILE_DOC = new Color(37, 99, 235);
    public static final Color FILE_PRESENTATION = new Color(234, 88, 12);
    public static final Color FILE_ARCHIVE = new Color(107, 114, 128);
    public static final Color FILE_GENERIC = new Color(107, 114, 128);
    public static final Color FILE_SUBMISSION = new Color(219, 39, 119);
    public static final Font FONT_WEEK_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font ENROLLMENT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font ENROLLMENT_SECTION_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font ENROLLMENT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font ENROLLMENT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font ENROLLMENT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font ENROLLMENT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    // Metric Accents
    public static final Color ACCENT_BLUE_BG = new Color(239, 244, 255);
    public static final Color ACCENT_BLUE = new Color(37, 99, 235);

    public static final Color ACCENT_GREEN_BG = new Color(240, 253, 244);
    public static final Color ACCENT_GREEN = new Color(21, 128, 61);

    public static final Color ACCENT_PURPLE_BG = new Color(239, 244, 255);
    public static final Color ACCENT_PURPLE = new Color(37, 99, 235);

    public static final Color ACCENT_AMBER_BG = new Color(255, 251, 235);
    public static final Color ACCENT_AMBER = new Color(180, 83, 9);

    public static final Color ACCENT_ROSE_BG = new Color(254, 242, 242);
    public static final Color ACCENT_ROSE = new Color(185, 28, 28);
    public static final Color ACCENT_GRAY_BG = new Color(244, 244, 245);
    public static final Color ACCENT_GRAY = new Color(107, 114, 128);
    public static final Color TYPE_PRACTICAL_BG = new Color(239, 244, 255);
    public static final Color TYPE_TUTORIAL_BG = new Color(255, 251, 235);
    public static final Color TYPE_PERSONAL_BG = new Color(240, 253, 244);
    public static final Color TYPE_TUTORIAL = new Color(180, 83, 9);
    public static final Color TYPE_PERSONAL = new Color(21, 128, 61);

    // Banner Gradient
    public static final Color BANNER_GRADIENT_START = new Color(250, 250, 250);
    public static final Color BANNER_GRADIENT_END = new Color(250, 250, 250);

    // Donut Chart Colors
    public static final Color CHART_EXCELLENT = new Color(37, 99, 235);
    public static final Color CHART_GOOD = new Color(96, 142, 239);
    public static final Color CHART_AVERAGE = new Color(161, 188, 245);
    public static final Color CHART_NEEDS_IMP = new Color(209, 219, 239);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font TIMETABLE_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font TIMETABLE_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font PROFILE_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font PROFILE_STUDENT_NAME = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font PROFILE_DIALOG_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font PROFILE_VALUE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_METRIC_SUB = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Color PROFILE_ERROR = new Color(185, 28, 28);
    public static final Color PROFILE_WARNING = new Color(180, 83, 9);
    public static final Color PROFILE_SUCCESS = new Color(21, 128, 61);
    public static final Color PROFILE_LOCKED_BG = new Color(250, 250, 250);
    public static final Color PROFILE_OVERLAY = new Color(15, 23, 42, 0);
    public static final Color PROFILE_SHADOW_SOFT = new Color(15, 23, 42, 0);

    /** Installs FlatLaf only while a student portal is open and remembers what to restore on logout. */
    public static StudentLookAndFeel installStudentLookAndFeel() {
        LookAndFeel previous = UIManager.getLookAndFeel();
        StudentLookAndFeel scope = new StudentLookAndFeel(previous);
        try {
            if (!FlatLightLaf.setup()) {
                throw new IllegalStateException("Could not install FlatLaf for the student portal.");
            }

            UIManager.getLookAndFeelDefaults().put("defaultFont", FONT_BODY);
            UIManager.getLookAndFeelDefaults().put("Panel.background", BG_MAIN);
            UIManager.getLookAndFeelDefaults().put("Button.arc", BUTTON_RADIUS);
            UIManager.getLookAndFeelDefaults().put("Component.arc", CARD_RADIUS);
            UIManager.getLookAndFeelDefaults().put("TextComponent.arc", CARD_RADIUS);
            UIManager.getLookAndFeelDefaults().put("Component.focusWidth", 1);
            UIManager.getLookAndFeelDefaults().put("Component.borderColor", CARD_BORDER);
            UIManager.getLookAndFeelDefaults().put("Component.focusedBorderColor", PRIMARY);
            UIManager.getLookAndFeelDefaults().put("Table.rowHeight", 44);
            UIManager.getLookAndFeelDefaults().put("Table.selectionBackground", PRIMARY_LIGHT);
            UIManager.getLookAndFeelDefaults().put("Table.selectionForeground", TEXT_MAIN);
            UIManager.getLookAndFeelDefaults().put("Table.showHorizontalLines", false);
            UIManager.getLookAndFeelDefaults().put("Table.showVerticalLines", false);
            UIManager.getLookAndFeelDefaults().put("TableHeader.background", CARD_BG);
            UIManager.getLookAndFeelDefaults().put("TableHeader.foreground", TEXT_MUTED);
            UIManager.getLookAndFeelDefaults().put("ScrollBar.thumbArc", 8);
            UIManager.getLookAndFeelDefaults().put("ScrollBar.trackArc", 8);
            FlatLaf.updateUI();
            return scope;
        } catch (RuntimeException e) {
            try {
                scope.restore();
            } catch (RuntimeException restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            throw e;
        }
    }

    public static final class StudentLookAndFeel {
        private final LookAndFeel previous;
        private boolean restored;

        private StudentLookAndFeel(LookAndFeel previous) {
            this.previous = previous;
        }

        public void restore() {
            if (restored) {
                return;
            }
            try {
                UIManager.setLookAndFeel(previous);
                FlatLaf.updateUI();
                restored = true;
            } catch (javax.swing.UnsupportedLookAndFeelException e) {
                throw new IllegalStateException("Could not restore the previous application theme.", e);
            }
        }
    }

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
