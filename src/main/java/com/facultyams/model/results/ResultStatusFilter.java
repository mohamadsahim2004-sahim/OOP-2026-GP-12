package com.facultyams.model.results;

import com.facultyams.service.ResultService.ResultStatus;
import java.awt.Color;

/** Display values and colors for the result-status filter. */
public enum ResultStatusFilter {
    ALL("All statuses", null, null),
    PASS("Pass", ResultStatus.PASS, new Color(16, 185, 129)),
    FAIL("Fail", ResultStatus.FAIL, new Color(239, 68, 68)),
    PENDING("Pending", ResultStatus.PENDING, new Color(107, 114, 128));

    private final String label;
    private final ResultStatus status;
    private final Color color;

    ResultStatusFilter(String label, ResultStatus status, Color color) {
        this.label = label;
        this.status = status;
        this.color = color;
    }

    public String label() { return label; }
    public ResultStatus status() { return status; }
    public Color color() { return color; }

    @Override public String toString() { return label; }
}
