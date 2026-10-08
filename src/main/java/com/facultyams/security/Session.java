package com.facultyams.security;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.model.User;

/**
 * Holds the currently logged-in user for this desktop application.
 * Other modules use it like:
 *
 * <pre>
 *   User me = Session.getCurrentUser();
 *   Session.requireRole(Role.LECTURER);
 * </pre>
 */
public final class Session {

    private static User currentUser;

    private Session() {
        // static holder
    }

    public static synchronized void start(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot start a session without a user");
        }
        currentUser = user;
    }

    public static synchronized void end() {
        currentUser = null;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Returns true if a user is logged in and has one of the given roles. */
    public static synchronized boolean hasRole(Role... roles) {
        if (currentUser == null) {
            return false;
        }
        for (Role role : roles) {
            if (currentUser.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Throws if nobody is logged in or the logged-in user has none of the given roles.
     */
    public static synchronized void requireRole(Role... roles) throws AuthorizationException {
        if (currentUser == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
        if (!hasRole(roles)) {
            throw new AuthorizationException("Your role (" + currentUser.getRole().getDisplayName()
                    + ") is not allowed to perform this action.");
        }
    }

    /** Throws if nobody is logged in. */
    public static synchronized void requireLogin() throws AuthorizationException {
        if (currentUser == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
    }
}