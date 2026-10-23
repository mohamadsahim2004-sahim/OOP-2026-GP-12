package com.facultyams.model;

import com.facultyams.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lecturer. Can update their profile except username and password.
 * Lecturer-specific screens/services are implemented by Member 2.
 */
public class Lecturer extends User {

    private static final Set<ProfileField> EDITABLE =
            Collections.unmodifiableSet(EnumSet.allOf(ProfileField.class));

    private String designation = "Lecturer";

    public Lecturer() {
        super();
    }

    public Lecturer(int userId, String username, String fullName, String email, String designation) {
        super(userId, username, fullName, email);
        this.designation = designation;
    }

    @Override
    public Role getRole() {
        return Role.LECTURER;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}