package com.fot.ams.model;

import java.util.Arrays;
import java.util.List;

/**
 * Lecturer role model extending User.
 * Attributes per SRS 7.2: designation, specialisation
 */
public class Lecturer extends User {
    private String designation;
    private String specialisation;

    public Lecturer(int userID, String username, String passwordHash, String fullName, 
                    String email, String phone, String departmentID, 
                    String designation, String specialisation) {
        super(userID, username, passwordHash, UserRole.LECTURER, fullName, email, phone, departmentID);
        this.designation = designation;
        this.specialisation = specialisation;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSpecialisation() {
        return specialisation;
    }

    public void setSpecialisation(String specialisation) {
        this.specialisation = specialisation;
    }

    @Override
    public String getDashboardTitle() {
        return "Lecturer Academic Dashboard";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(
            "Manage Assigned Course Materials",
            "Upload & Compute Exam & CA Marks (out of 100)",
            "View Whole Batch & Individual CA Summary",
            "View Undergraduate Attendance & Medical Records",
            "View Subject-wise Grades & GPA / CGPA Reports",
            "Publish Academic Notices"
        );
    }

    @Override
    public String getRoleSpecificIdentifier() {
        return designation + " (" + specialisation + ")";
    }
}
