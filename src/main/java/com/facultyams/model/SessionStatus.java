package com.facultyams.model;

/**
 * Status of attendance for an individual class session.
 */
public enum SessionStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    MEDICAL_PENDING("Medical Pending"),
    MEDICAL_APPROVED("Medical Approved"),
    NOT_HELD("Not Held");

    private final String display;

    SessionStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
