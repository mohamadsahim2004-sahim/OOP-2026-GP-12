package com.facultyams.model;

import java.time.LocalDate;

public class Medical {
    private int medicalId;
    private  int attendanceId;
    private int reviewedId;
    private LocalDate submittedDate;
    private String reason;
    private  String approvalStatus;

    public Medical(){}

    public Medical(int attendanceId, int reviewedId, LocalDate submittedDate,String reason,String approvalStatus ){
        this.attendanceId=attendanceId;
        this.reviewedId=reviewedId;
        this.submittedDate=submittedDate;
        this.reason=reason;
        setApprovalStatus(approvalStatus);
    }
    public Medical(int medicalId,int attendanceId, int reviewedId, LocalDate submittedDate,String reason,String approvalStatus ){
         this.medicalId=medicalId;
         this.attendanceId=attendanceId;
        this.reviewedId=reviewedId;
        this.submittedDate=submittedDate;
        this.reason=reason;
        setApprovalStatus(approvalStatus);
    }

    public int getMedicalId() {
        return medicalId;
    }
    public void setMedicalId(int medicalId) {
        this.medicalId = medicalId;
    }
    public String getApprovalStatus() {
        return approvalStatus;
    }
    public void setApprovalStatus(String approvalStatus) {
    if (!approvalStatus.equals("pending") &&
        !approvalStatus.equals("approved") &&
        !approvalStatus.equals("rejected")) {
        throw new IllegalArgumentException("approval status must be pending,approved or rejected");
    }
    this.approvalStatus = approvalStatus;
}
    public int getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(int attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getReviewedId() {
        return reviewedId;
    }
    public void setReviewedId(int reviewedId) {
        this.reviewedId = reviewedId;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }
    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;

    }

    public boolean isApproved() {
    return approvalStatus.equals("approved");
}
    


}
