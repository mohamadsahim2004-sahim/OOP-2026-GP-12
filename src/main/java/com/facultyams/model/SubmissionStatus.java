package com.facultyams.model;

/**
 * Status of an assignment submission.
 */
public enum SubmissionStatus {
    NOT_SUBMITTED("Not submitted"),
    SUBMITTED("Submitted"),
    LATE("Overdue"),
    GRADED("Graded");

    private final String display;

    SubmissionStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
