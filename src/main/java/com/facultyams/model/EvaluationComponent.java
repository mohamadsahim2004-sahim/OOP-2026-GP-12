package com.facultyams.model;

public class EvaluationComponent {
    private int evalComponentId;
    private String componentName;
    private String category;
    private double maxMarks;
    private double weightage;

    public EvaluationComponent(){

    }
    public EvaluationComponent(int evalComponentId, String componentName,
                               String category, double maxMarks, double weightage) {
        this.evalComponentId = evalComponentId;
        this.componentName = componentName;
        this.category = category;
        this.maxMarks = maxMarks;
        this.weightage = weightage;
    }
    public int getEvalComponentId(){
        return evalComponentId;
    }

    public void setEvalComponentId(int evalComponentId){
        this.evalComponentId=evalComponentId;
    }

    public String getComponentName(){
        return componentName;
    }
    public void setComponentName(String componentName){
        this.componentName=componentName;
    }

    public String getCategory(){
        return category;
    }

    public void setCategory(String category){
        this.category=category;
    }

    public double getMaxMarks(){
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks){
        this.maxMarks=maxMarks;
    }
    public double getWeightage(){
        return weightage;
    }
    public void setWeightage(double weightage){
        this.weightage=weightage;
    }
}
