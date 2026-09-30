package com.facultyams.model;

public class EvaluationComponent {
    private int componentId;
    private int courseId;
    private String componentName;
    private double percentage;

    public EvaluationComponent() {
}
    public EvaluationComponent(int componentId, int courseId, String componentName, double percentage) {
        this.componentId = componentId;
        this.courseId = courseId;
        this.componentName = componentName;
        this.percentage = percentage;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
