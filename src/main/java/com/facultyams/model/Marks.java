package com.facultyams.model;

public class Marks {
    private int marksid;
    private double obtainMarks;
    private double finalMarks;
    private String grade;
    private boolean isEligible;


    public Marks() {

    }
}

public Marks(int marksid,double obtainMarks,double finalMarks,String grade,boolean isEligible){
    this.marksid = marksid;
    this.obtainMarks = obtainMarks;
    this.finalMarks = finalMarks;
    this.grade = grade;
    this.isEligible = isEligible;
}

public int getMarksid(){
    return marksid;
}
public void setMarksid(int marksid){
    this.marksid = marksid;
}
public double getObtainMarks(){
    return obtainMarks;
}
public void setObtainMarks(double obtainMarks){
    this.obtainMarks( = obtainMarks;
}
public double getFinalMarks() {
    return finalMarks;
}

public void setFinalMarks(double finalMarks) {
    this.finalMarks = finalMarks;
}
