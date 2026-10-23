package com.facultyams.model.results;

import java.math.BigDecimal;

/** Letter grades supported by the GPA calculator. */
public enum LetterGrade {
    A_PLUS("A+", "4.0"),
    A("A", "4.0"),
    A_MINUS("A-", "3.7"),
    B_PLUS("B+", "3.3"),
    B("B", "3.0"),
    B_MINUS("B-", "2.7"),
    C_PLUS("C+", "2.3"),
    C("C", "2.0"),
    C_MINUS("C-", "1.7"),
    D("D", "1.0"),
    F("F", "0.0");

    private final String label;
    private final BigDecimal points;

    LetterGrade(String label, String points) {
        this.label = label;
        this.points = new BigDecimal(points);
    }

    public String label() {
        return label;
    }

    public BigDecimal points() {
        return points;
    }

    @Override
    public String toString() {
        return label;
    }
}
