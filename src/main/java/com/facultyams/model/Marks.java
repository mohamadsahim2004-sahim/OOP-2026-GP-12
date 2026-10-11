package com.facultyams.model;

public class Marks {
    private int id;
    private String courseCode;
    private String subject;
    private String examType;
    private String grade;
    private double score;
    private double maxScore;
    private String date;
    private int credits;
    private String semester;

    public Marks() {
    }

    public Marks(int id, String courseCode, String subject, String examType, String grade, 
                 double score, double maxScore, String date, int credits, String semester) {
        this.id = id;
        this.courseCode = courseCode;
        this.subject = subject;
        this.examType = examType;
        this.grade = grade;
        this.score = score;
        this.maxScore = maxScore;
        this.date = date;
        this.credits = credits;
        this.semester = semester;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getScoreFormatted() {
        return ((int) score) + "/" + ((int) maxScore);
    }
}
