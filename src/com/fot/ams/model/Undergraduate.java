package com.fot.ams.model;

import java.util.Arrays;
import java.util.List;

/**
 * Undergraduate role model extending User.
 * Attributes per SRS 7.2: indexNumber, batch, enrollmentStatus, registrationYear
 */
public class Undergraduate extends User {
    private String indexNumber;
    private String batch;
    private String enrollmentStatus; // active, repeat, batch-missed
    private int registrationYear;

    public Undergraduate(int userID, String username, String passwordHash, String fullName, 
                         String email, String phone, String departmentID, 
                         String indexNumber, String batch, String enrollmentStatus, int registrationYear) {
        super(userID, username, passwordHash, UserRole.UNDERGRADUATE, fullName, email, phone, departmentID);
        this.indexNumber = indexNumber;
        this.batch = batch;
        this.enrollmentStatus = enrollmentStatus;
        this.registrationYear = registrationYear;
    }

    public String getIndexNumber() {
        return indexNumber;
    }

    public void setIndexNumber(String indexNumber) {
        this.indexNumber = indexNumber;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(String enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public int getRegistrationYear() {
        return registrationYear;
    }

    public void setRegistrationYear(int registrationYear) {
        this.registrationYear = registrationYear;
    }

    @Override
    public String getDashboardTitle() {
        return "Student Academic Portal";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(
            "View Own Attendance Details (Theory, Practical, Combined %)",
            "View Own Medical Records & Approval Status",
            "View Enrolled Course Details & Learning Materials",
            "View Subject-wise Grades, SGPA & CGPA",
            "View Academic Timetable & Notices",
            "Update Contact Details & Profile Photo"
        );
    }

    @Override
    public String getRoleSpecificIdentifier() {
        return "Index: " + indexNumber + " • Batch " + batch + " (" + enrollmentStatus + ")";
    }
}
