package com.fot.ams.model;

import java.util.Arrays;
import java.util.List;

/**
 * Admin role model extending User.
 * Demonstrates: Inheritance and Polymorphism.
 */
public class Admin extends User {

    public Admin(int userID, String username, String passwordHash, String fullName, 
                 String email, String phone, String departmentID) {
        super(userID, username, passwordHash, UserRole.ADMIN, fullName, email, phone, departmentID);
    }

    @Override
    public String getDashboardTitle() {
        return "System Administrator Portal";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(
            "User Profile Management (CRUD)",
            "Course Management (CRUD)",
            "Notice Board Management (CRUD)",
            "Timetable Management (CRUD)",
            "System Audit & Passwords Reset"
        );
    }

    @Override
    public String getRoleSpecificIdentifier() {
        return "Admin-ID: ADM-" + getUserID();
    }
}
