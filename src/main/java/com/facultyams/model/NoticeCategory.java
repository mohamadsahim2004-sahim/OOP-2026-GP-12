package com.facultyams.model;

import java.awt.Color;

public enum NoticeCategory {
    EXAMINATIONS("Examinations", new Color(254, 226, 226), new Color(220, 38, 38)),
    ACADEMIC("Academic", new Color(219, 234, 254), new Color(29, 78, 216)),
    EVENTS("Events", new Color(220, 252, 231), new Color(22, 163, 74)),
    GENERAL("General", new Color(243, 244, 246), new Color(75, 85, 99));

    private final String label;
    private final Color tint;
    private final Color foreground;

    NoticeCategory(String label, Color tint, Color foreground) {
        this.label = label;
        this.tint = tint;
        this.foreground = foreground;
    }

    public String displayLabel() { return label; }
    public Color tint() { return tint; }
    public Color foreground() { return foreground; }

    public static NoticeCategory fromLabel(String label) {
        if (label == null) return GENERAL;
        for (NoticeCategory category : values()) {
            if (category.label.equalsIgnoreCase(label.trim())) return category;
        }
        return GENERAL;
    }
}
