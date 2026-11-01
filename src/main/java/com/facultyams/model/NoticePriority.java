package com.facultyams.model;

import java.awt.Color;

public enum NoticePriority {
    URGENT("Urgent", new Color(254, 226, 226), new Color(220, 38, 38)),
    IMPORTANT("Important", new Color(254, 243, 199), new Color(217, 119, 6)),
    NORMAL("Normal", new Color(219, 234, 254), new Color(37, 99, 235));

    private final String label;
    private final Color tint;
    private final Color foreground;

    NoticePriority(String label, Color tint, Color foreground) {
        this.label = label;
        this.tint = tint;
        this.foreground = foreground;
    }

    public String displayLabel() { return label; }
    public Color tint() { return tint; }
    public Color foreground() { return foreground; }
}
