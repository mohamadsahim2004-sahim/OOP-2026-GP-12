package com.facultyams.model;

public class CourseMaterial {
    private int materialID;
    private int courseID;
    private String description;
    private String notes;
    private String referenceText;  //other classs can not directly access these fields

    public CourseMaterial(){

    }  //to help creeate object
    public CourseMaterial(int materialID,int courseID,String description,String notes,String referenceText){
        this.materialID=materialID;
        this.courseID=courseID;
        this.description=description;
        this.notes=notes;
        this.referenceText=referenceText;  //create object and initialize all 5 fields once
    }
    public int getMaterialID(){
        return materialID;
    }
    public void setMaterialID(){
        this.materialID=materialID;
    }
    public int getCourseID(){
        return courseID;
    }
    public void setCourseID(int courseID){
        this.courseID=courseID;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description=description;
    }
    public String getNotes(){
        return notes;
    }
    public void setNotes(String notes){
        this.notes=notes;
    }
    public String getReferenceText(){
        return referenceText;
    }
    public void setReferenceText(String referenceText){
        this.referenceText=referenceText;
    }
    //getters return the current value of a field
    //setters change the value of a field
}
