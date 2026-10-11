package com.facultyams.model;

public class CourseMaterial {
    private int id;
    private String courseCode;
    private String courseName;
    private String title;
    private String type; // Lecture Note, Lab Sheet, Assignment, Syllabus, Past Paper
    private String fileSize;
    private String uploadDate;
    private String uploadedBy;

    public CourseMaterial() {
    }

    public CourseMaterial(int id, String courseCode, String courseName, String title, 
                          String type, String fileSize, String uploadDate, String uploadedBy) {
        this.id = id;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.title = title;
        this.type = type;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.uploadedBy = uploadedBy;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
