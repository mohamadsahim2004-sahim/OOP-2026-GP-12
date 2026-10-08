package com.facultyams.model;

import com.facultyams.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** System administrator. Maintains users, departments and courses. */
public class Admin extends User {

    private static final Set<ProfileField> EDITABLE =
            Collections.unmodifiableSet(EnumSet.allOf(ProfileField.class));

    public Admin() {
        super();
    }

    public Admin(int userId, String username, String fullName, String email) {
        super(userId, username, fullName, email);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }
}