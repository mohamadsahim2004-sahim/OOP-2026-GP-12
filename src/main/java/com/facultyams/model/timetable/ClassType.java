package com.facultyams.model.timetable;

/** The visual and academic category of a scheduled item. */
public enum ClassType {
    LECTURE("Lecture"), PRACTICAL("Practical"), TUTORIAL("Tutorial"), PERSONAL("Personal");

    private final String label;

    ClassType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
