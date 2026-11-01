package com.facultyams.model;

import java.util.Set;

/**
 * One assessable part of a course (quiz, assignment, practical, final exam ...).
 * {@code weightage} is the percentage of the FINAL course mark; {@code maxMarks} is what the
 * component is marked out of.
 */
public class EvaluationComponent {

    public static final String CA = "CA";
    public static final String ASSIGNMENT = "ASSIGNMENT";
    public static final String PRACTICAL = "PRACTICAL";
    public static final String FINAL_EXAM = "FINAL_EXAM";

    /** Every category the database accepts. */
    public static final Set<String> CATEGORIES = Set.of(CA, ASSIGNMENT, PRACTICAL, FINAL_EXAM);

    private int evalComponentId;
    private int courseId;
    private String componentName;
    private String category;
    private double maxMarks;
    private double weightage;

    public EvaluationComponent() {
    }

    public EvaluationComponent(int evalComponentId, String componentName,
                               String category, double maxMarks, double weightage) {
        this.evalComponentId = evalComponentId;
        this.componentName = componentName;
        this.category = category;
        this.maxMarks = maxMarks;
        this.weightage = weightage;
    }

    public EvaluationComponent(int evalComponentId, int courseId, String componentName,
                               String category, double maxMarks, double weightage) {
        this(evalComponentId, componentName, category, maxMarks, weightage);
        this.courseId = courseId;
    }

    /** CA, assignment and practical marks together form the continuous-assessment (CA) part. */
    public boolean isContinuousAssessment() {
        return !FINAL_EXAM.equals(category);
    }

    public boolean isFinalExam() {
        return FINAL_EXAM.equals(category);
    }

    public int getEvalComponentId() {
        return evalComponentId;
    }

    public void setEvalComponentId(int evalComponentId) {
        this.evalComponentId = evalComponentId;
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

    public double getWeightage() {
        return weightage;
    }

    public void setWeightage(double weightage) {
        this.weightage = weightage;
    }
}
