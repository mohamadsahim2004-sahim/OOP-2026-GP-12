package com.facultyams.service.profile;

import com.facultyams.dao.StudentProfileDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.User;
import com.facultyams.model.student.StudentProfile;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.StudentAccess;
import com.facultyams.service.StudentServiceException;
import com.facultyams.service.profile.validation.PasswordStrengthEvaluator;
import com.facultyams.service.profile.validation.SriLankanPhoneValidator;

import java.util.Arrays;

/**
 * Database-backed profile service. The student id always comes from the session. Only the contact number and
 * the profile picture are written; name, e-mail, registration number, batch, status and department are
 * administered elsewhere and are never touched here.
 */
public final class DbStudentProfileService implements StudentProfileService {
    private final StudentProfileDAO profileDAO = new StudentProfileDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SriLankanPhoneValidator phoneValidator = new SriLankanPhoneValidator();
    private final PasswordStrengthEvaluator strength = new PasswordStrengthEvaluator();

    @Override
    public StudentProfile getProfile() {
        int me = StudentAccess.currentStudentId();
        try {
            StudentProfile p = profileDAO.findProfile(me);
            if (p == null) {
                throw new StudentServiceException("Your student record could not be found.");
            }
            return p;
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your profile could not be loaded. Please try again.", e);
        }
    }

    @Override
    public void updateContactNumber(String phone) {
        int me = StudentAccess.currentStudentId();
        String value = phone == null ? "" : phone.trim();
        String error = phoneValidator.validate(value);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        value = value.replaceAll("[\\s-]", "");
        if (value.length() > 15) {
            throw new IllegalArgumentException("Contact number is too long.");
        }
        try {
            StudentProfile current = profileDAO.findProfile(me);
            profileDAO.updateContactAndPicture(me, value, current == null ? null : current.getProfileImageUrl());
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your contact number could not be saved. Please try again.", e);
        }
    }

    @Override
    public void updatePhoto(String imagePath) {
        int me = StudentAccess.currentStudentId();
        if (imagePath != null && imagePath.isBlank()) imagePath = null;   // blank = remove the photo
        if (imagePath != null && imagePath.length() > 255) {
            throw new IllegalArgumentException("The image path is too long (maximum 255 characters).");
        }
        try {
            StudentProfile current = profileDAO.findProfile(me);
            profileDAO.updateContactAndPicture(me, current == null ? null : current.getPhoneNumber(), imagePath);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your photo could not be saved. Please try again.", e);
        }
    }

    @Override
    public void changePassword(char[] currentPassword, char[] newPassword) {
        int me = StudentAccess.currentStudentId();
        if (currentPassword == null || currentPassword.length == 0) {
            throw new IllegalArgumentException("Enter your current password.");
        }
        if (newPassword == null) {
            throw new IllegalArgumentException("Enter a new password.");
        }
        try {
            boolean[] results = strength.evaluate(newPassword, currentPassword);
            for (boolean ok : results) {
                if (!ok) {
                    throw new IllegalArgumentException("The new password does not meet every requirement.");
                }
            }
            User user = userDAO.findById(me);
            if (user == null || !PasswordUtil.verifyPassword(new String(currentPassword), user.getPasswordHash())) {
                throw new IllegalArgumentException("The current password is incorrect.");
            }
            userDAO.updatePassword(me, PasswordUtil.hashPassword(new String(newPassword)));
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your password could not be changed. Please try again.", e);
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }
}
