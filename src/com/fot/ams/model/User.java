package com.fot.ams.model;

import java.util.List;

/**
 * Abstract base class User representing key entities in the Faculty of Technology Academic Management System.
 * Demonstrates: Abstraction, Encapsulation, and Inheritance base.
 */
public abstract class User {
    private int userID;
    private String username;
    private String passwordHash;
    private UserRole role;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicture;
    private String departmentID;

    public User(int userID, String username, String passwordHash, UserRole role, 
                String fullName, String email, String phone, String departmentID) {
        this.userID = userID;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.profilePicture = "default_avatar.png";
        this.departmentID = departmentID;
    }

    // Abstract methods demonstrating Abstraction & Polymorphic behavior
    public abstract String getDashboardTitle();
    public abstract List<String> getPermissions();
    public abstract String getRoleSpecificIdentifier();

    // Encapsulation: Getters and Setters
    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getDepartmentID() {
        return departmentID;
    }

    public void setDepartmentID(String departmentID) {
        this.departmentID = departmentID;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
