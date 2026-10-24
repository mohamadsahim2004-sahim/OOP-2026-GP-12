
package com.facultyams.model;

import com.facultyams.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Common data and behaviour shared by every kind of user.
 */
public abstract class User {

    public enum ProfileField {
        FULL_NAME,
        EMAIL,
        CONTACT_NUMBER,
        PROFILE_PICTURE
    }

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String contactNumber;
    private String profilePicture;
    private Integer departmentId;
    private String departmentName;
    private boolean active = true;
    private LocalDateTime createdAt;
    private String legacyRole;

    protected User() {
    }

    protected User(
            int userId,
            String username,
            String fullName,
            String email) {

        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    /**
     * Compatibility constructor for existing user subclasses.
     */
    protected User(
            int userID,
            String username,
            String password,
            String role,
            String fullName,
            String email,
            String phone,
            String profilePicture,
            int departmentID) {

        this(userID, username, fullName, email);
        this.passwordHash = password;
        this.legacyRole = role;
        this.contactNumber = phone;
        this.profilePicture = profilePicture;
        this.departmentId = departmentID;
    }

    public abstract Role getRole();

    public abstract Set<ProfileField> getEditableProfileFields();

    public boolean canEditOwn(ProfileField field) {
        return getEditableProfileFields().contains(field);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Compatibility with the existing Lecturer module.
    public int getUserID() {
        return userId;
    }

    public void setUserID(int userID) {
        this.userId = userID;
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

    // Legacy accessor retained for compatibility.
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public String getLegacyRole() {
        return legacyRole;
    }

    public void setRole(String role) {
        this.legacyRole = role;
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

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    // Legacy phone accessors.
    public String getPhone() {
        return contactNumber;
    }

    public void setPhone(String phone) {
        this.contactNumber = phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    // Legacy department accessors.
    public int getDepartmentID() {
        return departmentId == null ? 0 : departmentId;
    }

    public void setDepartmentID(int departmentID) {
        this.departmentId = departmentID;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ")";
    }
}
