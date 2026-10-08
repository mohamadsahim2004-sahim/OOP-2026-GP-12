package com.facultyams.service;

import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

/** Login / logout. */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Verifies the credentials, starts the session and returns the logged-in user.
     * The returned object is the correct subclass (Admin, Lecturer, ...).
     *
     * @throws ValidationException     if username or password is empty
     * @throws AuthenticationException if the credentials are wrong or the account is inactive
     * @throws DatabaseException       if the database cannot be reached
     */
    public User login(String username, String password)
            throws ValidationException, AuthenticationException, DatabaseException {
        String cleanUsername = ValidationUtil.requireNotBlank(username, "Username");
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required.");
        }

        User user = userDAO.findByUsername(cleanUsername);
        // Same message for "no such user" and "wrong password" so usernames cannot be guessed.
        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Please contact the administrator.");
        }

        Session.start(user);
        return user;
    }

    public void logout() {
        Session.end();
    }

    public User getCurrentUser() {
        return Session.getCurrentUser();
    }
}