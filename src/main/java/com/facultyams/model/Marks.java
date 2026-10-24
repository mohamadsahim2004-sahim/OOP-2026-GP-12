package com.facultyams.model;

public class Marks {
    private int marksId;
    private int studentId;
    private int courseId;
    private double obtainMarks;
    private double finalMarks;
    private String grade;
    private boolean isEligible;
    private double overallMark;
    private double gradePoints;
    public Marks(){

    }

    public Marks(int marksId,double obtainMarks,double finalMarks,String grade,boolean isEligible){
        this.marksId=marksId;
        this.obtainMarks=obtainMarks;
        this.finalMarks=finalMarks;
        this.grade=grade;
        this.isEligible=isEligible;
    }
    public Marks(int marksId, int studentId, int courseId,
                 double obtainMarks, double finalMarks,
                 String grade, boolean isEligible) {
        this.marksId = marksId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.obtainMarks = obtainMarks;
        this.finalMarks = finalMarks;
        this.grade = grade;
        this.isEligible = isEligible;
    }
    public int getMarksId(){
        return marksId;
    }

    public void setMarksId(int marksId){
        this.marksId=marksId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
    public double getObtainMarks(){
        return obtainMarks;
    }

    public void setObtainMarks(double obtainMarks){
        this.obtainMarks=obtainMarks;
    }

    public double getFinalMarks() {
        return finalMarks;
    }
    public void setFinalMarks(double finalMarks){
        this.finalMarks=finalMarks;
    }

    public String getGrade(){
        return grade;
    }
    public void setGrade(String grade){
        this.grade=grade;
    }

    public boolean isEligible(){
        return isEligible;
    }
    public void setEligible(boolean eligible){
        this.isEligible=eligible;
    }

    public double getOverallMark() {
        return overallMark;
    }

    public void setOverallMark(double overallMark) {
        this.overallMark = overallMark;
    }

    public double getGradePoints() {
        return gradePoints;
    }

    public void setGradePoints(double gradePoints) {
        this.gradePoints = gradePoints;
    }
}
