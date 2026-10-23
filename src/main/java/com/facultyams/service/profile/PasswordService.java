package com.facultyams.service.profile;

/** Strategy boundary for safely verifying and updating student passwords. */
@FunctionalInterface
public interface PasswordService {
    /** Verifies the existing credential and stores a salted hash of the new password. */
    void changePassword(String storedHash, char[] currentPassword, char[] newPassword,
                        java.util.function.Consumer<String> storeHash);
}
