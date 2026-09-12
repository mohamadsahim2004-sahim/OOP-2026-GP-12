package com.fot.ams.dao;

import com.fot.ams.exception.DatabaseException;
import com.fot.ams.model.User;

import java.util.List;

/**
 * UserDAO interface defining data access operations for users.
 * Follows DAO design pattern as prescribed in SRS Section 9.
 */
public interface UserDAO {
    /**
     * Finds a user by unique username (or student index / staff ID).
     */
    User findByUsername(String username) throws DatabaseException;

    /**
     * Validates credentials against database / secure store.
     */
    User authenticate(String username, String rawPassword) throws DatabaseException;

    /**
     * Checks if a username or student/staff ID is already registered.
     */
    boolean usernameExists(String username) throws DatabaseException;

    /**
     * Registers a new user with persistence.
     */
    void registerUser(User user) throws DatabaseException;

    /**
     * Retrieves all registered users.
     */
    List<User> getAllUsers() throws DatabaseException;

    /**
     * Saves or updates a user profile.
     */
    void saveUser(User user) throws DatabaseException;
}
