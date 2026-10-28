package com.facultyams.service.profile;

import com.facultyams.model.student.StudentProfile;

/** Profile operations for the logged-in student. */
public interface StudentProfileService {
    /** Returns the logged-in student's profile (fresh from the database). */
    StudentProfile getProfile();

    /** Updates the contact number (the only editable text field). Throws a message-bearing exception if invalid. */
    void updateContactNumber(String phone);

    /** Updates the profile picture path. */
    void updatePhoto(String imagePath);

    /** Verifies the current password, then stores a PBKDF2 hash of the new one. */
    void changePassword(char[] currentPassword, char[] newPassword);
}
