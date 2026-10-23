package com.facultyams.model;

import com.facultyams.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Technical Officer. Can update their profile except username and password.
 * Attendance/medical features are implemented by Member 3.
 */
public class TechnicalOfficer extends User {

    private static final Set<ProfileField> EDITABLE =
            Collections.unmodifiableSet(EnumSet.allOf(ProfileField.class));

    public TechnicalOfficer() {
        super();
    }

    public TechnicalOfficer(int userId, String username, String fullName, String email) {
        super(userId, username, fullName, email);
    }

    @Override
    public Role getRole() {
        return Role.TECHNICAL_OFFICER;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }
}