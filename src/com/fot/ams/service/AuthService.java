package com.fot.ams.service;

import com.fot.ams.dao.UserDAO;
import com.fot.ams.dao.UserDAOImpl;
import com.fot.ams.exception.AuthenticationException;
import com.fot.ams.exception.DatabaseException;
import com.fot.ams.exception.InvalidCredentialsException;
import com.fot.ams.model.User;

/**
 * AuthService provides business logic for authentication and session creation.
 * Satisfies FR-AUTH-01 through FR-AUTH-05.
 */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user against credentials.
     *
     * @param username The username, index number, or staff ID
     * @param password The plain text password entered
     * @return Authenticated User object
     * @throws AuthenticationException when validation or authentication fails
     * @throws DatabaseException when database error occurs
     */
    public User login(String username, String password) throws AuthenticationException, DatabaseException {
        // NFR-02: Form validation
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Please enter your Username or Student/Staff ID.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Please enter your Password.");
        }

        // FR-AUTH-02 & FR-AUTH-04: Authenticate credentials without revealing which field is wrong
        User user = userDAO.authenticate(username.trim(), password);
        if (user == null) {
            throw new InvalidCredentialsException();
        }

        // FR-AUTH-03 & FR-AUTH-05: Session start
        SessionManager.getInstance().startSession(user);
        return user;
    }

    /**
     * Registers a new user with validation and SHA-256 password hashing.
     */
    public User register(User user, String rawPassword, String confirmPassword) 
            throws AuthenticationException, DatabaseException {
        if (user == null) {
            throw new AuthenticationException("Registration profile details cannot be empty.");
        }

        // 1. Validate Full Name
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new AuthenticationException("Please enter your Full Name.");
        }

        // 2. Validate Username / ID
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new AuthenticationException("Please enter a valid Username or Student/Staff ID.");
        }
        String cleanUsername = user.getUsername().trim();
        user.setUsername(cleanUsername);

        // 3. Check for existing username
        if (userDAO.usernameExists(cleanUsername)) {
            throw new AuthenticationException("Username or ID '" + cleanUsername + "' is already registered. Please sign in or use another ID.");
        }

        // 4. Validate Email
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new AuthenticationException("Please enter a valid Email Address (e.g. name@fot.ruh.ac.lk).");
        }

        // 5. Role-specific validation
        if (user instanceof com.fot.ams.model.Undergraduate) {
            com.fot.ams.model.Undergraduate ug = (com.fot.ams.model.Undergraduate) user;
            if (ug.getIndexNumber() == null || ug.getIndexNumber().trim().isEmpty()) {
                throw new AuthenticationException("Please provide your Student Index Number (e.g. TG/2024/2105).");
            }
            if (userDAO.usernameExists(ug.getIndexNumber().trim())) {
                throw new AuthenticationException("Index number '" + ug.getIndexNumber() + "' is already registered.");
            }
        } else if (user instanceof com.fot.ams.model.Lecturer) {
            com.fot.ams.model.Lecturer lec = (com.fot.ams.model.Lecturer) user;
            if (lec.getDesignation() == null || lec.getDesignation().trim().isEmpty()) {
                throw new AuthenticationException("Please specify Lecturer Designation.");
            }
            if (lec.getSpecialisation() == null || lec.getSpecialisation().trim().isEmpty()) {
                throw new AuthenticationException("Please specify Academic Specialisation.");
            }
        }

        // 6. Validate Password
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters long.");
        }
        if (!rawPassword.equals(confirmPassword)) {
            throw new AuthenticationException("Passwords do not match. Please verify and re-type.");
        }

        // 7. Hash Password with SHA-256 (NFR-05)
        String passwordHash = com.fot.ams.security.PasswordHasher.hashPassword(rawPassword);
        user.setPasswordHash(passwordHash);

        // 8. Persist User via DAO
        userDAO.registerUser(user);

        return user;
    }

    /**
     * Logs out the current user session.
     */
    public void logout() {
        SessionManager.getInstance().endSession();
    }
}
