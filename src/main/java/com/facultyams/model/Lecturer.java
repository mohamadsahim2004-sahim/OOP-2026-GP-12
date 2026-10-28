package com.facultyams.model;

import com.facultyams.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Lecturer extends User {

    private static final Set<ProfileField> EDITABLE =
            Collections.unmodifiableSet(
                    EnumSet.allOf(ProfileField.class));

    private int lecturerID;
    private String designation = "Lecturer";
    private String specialisation = "";
    private String phone = "";
    private String profilePicture;

    public Lecturer() {
        super();
    }

    public Lecturer(
            int userId,
            String username,
            String fullName,
            String email,
            String designation) {

        super(userId, username, fullName, email);
        this.lecturerID = userId;
        this.designation = designation;
    }

    // Compatibility constructor for the existing LecturerDAO.
    public Lecturer(
            int userID,
            String username,
            String password,
            String role,
            String fullName,
            String email,
            String phone,
            String profilePicture,
            int departmentID,
            int lecturerID,
            String designation,
            String specialisation) {

        super(userID, username, fullName, email);

        this.lecturerID = lecturerID;
        this.designation = designation;
        this.specialisation = specialisation;
        this.phone = phone;
        this.profilePicture = profilePicture;
    }

    @Override
    public Role getRole() {
        return Role.LECTURER;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }

    public int getLecturerID() {
        return lecturerID;
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

    // Compatibility with existing LecturerDashboard code.
    public int getUserID() {
        return getUserId();
    }
}