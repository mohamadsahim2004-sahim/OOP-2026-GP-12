package com.facultyams.model;

<<<<<<< HEAD
import com.facultyams.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Common data and behaviour shared by every kind of user.
 *
 * Abstraction:   User is abstract - a "plain" user cannot exist, only Admin,
 *                Lecturer, TechnicalOfficer or Student.
 * Polymorphism:  each subclass answers getRole() and getEditableProfileFields()
 *                differently (e.g. a Student may only edit contact number and picture).
 * Encapsulation: all fields are private with getters/setters.
 */
public abstract class User {

    /** Profile fields a user may be allowed to change on their own profile. */
    public enum ProfileField {
        FULL_NAME, EMAIL, CONTACT_NUMBER, PROFILE_PICTURE
    }

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String contactNumber;
    private String profilePicture;
    private Integer departmentId;
    private String departmentName;      // read-only, filled by DAO joins for display
    private boolean active = true;
    private LocalDateTime createdAt;

    protected User() {
    }

    protected User(int userId, String username, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    /** The role of this user (fixed by the subclass). */
    public abstract Role getRole();

    /**
     * Fields this kind of user may change on their OWN profile.
     * Username and password are never self-editable (only Admin can change them).
     */
    public abstract Set<ProfileField> getEditableProfileFields();

    public boolean canEditOwn(ProfileField field) {
        return getEditableProfileFields().contains(field);
    }

    // ----- getters / setters -----

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
=======
public abstract class User {   //srs variables
    private int userID;
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicture;
    private int departmentID;

    //"protected" is for allow child classes(lecturer)to call this constructor(User)
    protected User(int userID, String username, String password, String role, String fullName, String email, String phone, String profilePicture, int departmentID) {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.profilePicture = profilePicture;
        this.departmentID = departmentID;

    }

    //variables are private therefor getters and setters for access them
    //getters for read and setters for update
    public int getUserID() {
        return userID;
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa
    }

    public String getUsername() {
        return username;
    }

<<<<<<< HEAD
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
=======
    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa
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

<<<<<<< HEAD
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
=======
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
<<<<<<< HEAD

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
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
=======
}
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa
