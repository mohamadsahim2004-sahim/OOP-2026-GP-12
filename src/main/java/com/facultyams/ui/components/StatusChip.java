package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.Color;

/** Semantic status label styled as a small rounded chip. */
public final class StatusChip extends Badge {
    public StatusChip(String text) {
        super(text, background(text), foreground(text));
        setFont(Theme.FONT_BODY_BOLD);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 9, 4, 9));
    }

    private static Color background(String text) {
        if (text.startsWith("Ongoing")) return Theme.ACCENT_GREEN_BG;
        if (text.startsWith("Up next")) return Theme.PRIMARY_LIGHT;
        return Theme.ACCENT_GRAY_BG;
    }

    private static Color foreground(String text) {
        if (text.startsWith("Ongoing")) return Theme.TYPE_PERSONAL;
        if (text.startsWith("Up next")) return Theme.PRIMARY;
        return Theme.TEXT_MUTED;
    }
}
