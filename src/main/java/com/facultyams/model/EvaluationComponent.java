package com.facultyams.model;

public class EvaluationComponent {
    private int evalComponentid;
    private String componentName;
    private String category;
    private double maxMarks;
    private double weightage;


    public EvaluationComponent() {
    }

    public EvaluationComponent(int evalComponentid, String componentName,
                               String category, double maxMarks, double weightage) {
        this.evalComponentid = evalComponentid;
        this.componentName = componentName;
        this.category = category;
        this.maxMarks = maxMarks;
        this.weightage = weightage;
    }

    public int getEvalComponentid() {
        return evalComponentid;
    }

    public void setEvalComponentid(int evalComponentid) {
        this.evalComponentid = evalComponentid;
    }


    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }
}