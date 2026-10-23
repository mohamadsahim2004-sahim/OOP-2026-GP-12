package com.facultyams.model.results;

/** Exact result grades available in the academic-results filter. */
public enum ResultGrade {
    ALL("All grades", ""),
    A_PLUS("A+", "A+"),
    A("A", "A"),
    A_MINUS("A-", "A-"),
    B_PLUS("B+", "B+"),
    B("B", "B"),
    B_MINUS("B-", "B-"),
    C_PLUS("C+", "C+"),
    C("C", "C"),
    C_MINUS("C-", "C-"),
    D("D", "D"),
    E("E", "E");

    private final String label;
    private final String value;

    ResultGrade(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String label() { return label; }
    public String value() { return value; }

    @Override public String toString() { return label; }
}
