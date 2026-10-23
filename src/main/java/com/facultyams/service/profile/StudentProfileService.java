package com.facultyams.service.profile;

import com.facultyams.model.Student;

/** Persistence boundary for student profile operations. */
public interface StudentProfileService {
    /** Returns the current student's profile. */
    Student getProfile();

    /** Updates the student's contact details. */
    void updateContactInfo(String email, String phone, String address, String emergencyNumber, String relationship);

    /** Updates the student's photo path. */
    void updatePhoto(String imagePath);

    /** Verifies the old password and stores a salted password hash. */
    void changePassword(char[] currentPassword, char[] newPassword);
}
