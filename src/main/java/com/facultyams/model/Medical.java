package com.facultyams.model;

public class Medical {
    private String refNo;
    private String studentRegNo;
    private String courseCode;
    private String courseName;
    private String startDate;
    private String endDate;
    private String reason;
    private String doctorName;
    private String hospitalClinic;
    private String submissionDate;
    private String status; // Approved, Pending, Rejected
    private String adminRemarks;

    public Medical() {
        this.status = "Pending";
    }

    public Medical(String refNo, String studentRegNo, String courseCode, String courseName, 
                   String startDate, String endDate, String reason, String doctorName, 
                   String hospitalClinic, String submissionDate, String status, String adminRemarks) {
        this.refNo = refNo;
        this.studentRegNo = studentRegNo;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.doctorName = doctorName;
        this.hospitalClinic = hospitalClinic;
        this.submissionDate = submissionDate;
        this.status = status;
        this.adminRemarks = adminRemarks;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getStudentRegNo() {
        return studentRegNo;
    }

    public void setStudentRegNo(String studentRegNo) {
        this.studentRegNo = studentRegNo;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getHospitalClinic() {
        return hospitalClinic;
    }

    public void setHospitalClinic(String hospitalClinic) {
        this.hospitalClinic = hospitalClinic;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }
}
