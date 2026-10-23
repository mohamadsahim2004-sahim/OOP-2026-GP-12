package com.srms.model;

import java.awt.Color;

public enum DeadlineType {
    PROJECT("Project", new Color(219, 234, 254), new Color(29, 78, 216), true),
    ASSIGNMENT("Assignment", new Color(237, 233, 254), new Color(109, 40, 217), true),
    EXAM("Exam", new Color(254, 226, 226), new Color(220, 38, 38), false),
    QUIZ("Quiz", new Color(254, 243, 199), new Color(217, 119, 6), false);

    private final String displayLabel;
    private final Color badgeBackground;
    private final Color badgeForeground;
    private final boolean submittable;

    DeadlineType(String displayLabel, Color badgeBackground, Color badgeForeground, boolean submittable) {
        this.displayLabel = displayLabel;
        this.badgeBackground = badgeBackground;
        this.badgeForeground = badgeForeground;
        this.submittable = submittable;
    }

    public String displayLabel() { return displayLabel; }
    public Color badgeBackground() { return badgeBackground; }
    public Color badgeForeground() { return badgeForeground; }
    public boolean isSubmittable() { return submittable; }
}
