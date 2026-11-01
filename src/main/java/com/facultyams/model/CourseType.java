package com.facultyams.model;

public enum CourseType {
    CORE("Core"), ELECTIVE("Elective"), GENERAL("General");

    private final String label;
    CourseType(String label) { this.label = label; }
    public String label() { return label; }

    public static CourseType from(String value) {
        for (CourseType type : values()) {
            if (type.label.equalsIgnoreCase(value)) return type;
        }
        return GENERAL;
    }
}
