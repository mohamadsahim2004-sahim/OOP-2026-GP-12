package com.facultyams.model;

public class Grade {
    private String letter;
    private double gradePoint;
    private String description;

    public Grade() {
    }

    public Grade(String letter, double gradePoint, String description) {
        this.letter = letter;
        this.gradePoint = gradePoint;
        this.description = description;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public double getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(double gradePoint) {
        this.gradePoint = gradePoint;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
