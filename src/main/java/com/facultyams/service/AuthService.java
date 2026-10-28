package com.facultyams.service;

import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Session;

/** Authenticates users and manages their session. */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService() { this(new UserDAO()); }

    /** Constructor injection also allows tests to provide a fake DAO. */
    public AuthService(UserDAO userDAO) {
        if (userDAO == null) throw new IllegalArgumentException("UserDAO cannot be null.");
        this.userDAO = userDAO;
    }

    public User login(String username, String password)
            throws ValidationException, AuthenticationException, DatabaseException {
        if (username == null || username.isBlank()) {
            Session.end();
            throw new ValidationException("Username is required.");
        }
        if (password == null || password.isEmpty()) {
            Session.end();
            throw new ValidationException("Password is required.");
        }

        User user;
        try {                                                   // NEW
            user = userDAO.findByUsername(username.trim());     // NEW
        } catch (DatabaseException e) {                         // NEW
            Session.end(); // a failed attempt must never leave an earlier session active
            throw e;                                            // NEW
        }                                                       // NEW
        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            Session.end();
            throw new AuthenticationException("Invalid username or password.");
        }
        if (!user.isActive()) {
            Session.end();
            throw new AuthenticationException("This account is inactive.");
        }
        Session.start(user);
        return user;
    }

    public void logout() { Session.end(); }
}