package com.srms.model;

/**
 * Status of a medical excuse request.
 */
public enum MedicalStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn");

    private final String display;

    MedicalStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
