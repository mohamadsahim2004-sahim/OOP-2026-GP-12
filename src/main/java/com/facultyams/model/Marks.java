package com.facultyams.model;

public class Marks {
    private int marksId;
    private double obtainMarks;
    private double finalMarks;
    private String grade;
    private boolean isEligible;

    public Marks(){

    }

    public Marks(int marksId,double obtainMarks,double finalMarks,String grade,boolean isEligible){
        this.marksId=marksId;
        this.obtainMarks=obtainMarks;
        this.finalMarks=finalMarks;
        this.grade=grade;
        this.isEligible=isEligible;
    }

    public int getMarksId(){
        return marksId;
    }

    public void setMarksId(int marksId){
        this.marksId=marksId;
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

    
}
