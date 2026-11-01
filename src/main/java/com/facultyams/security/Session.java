package com.facultyams.security;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.model.User;

/** Holds the currently authenticated user for this desktop application. */
public final class Session {
    /** volatile: SwingWorker background threads read the session that the Swing thread wrote. */
    private static volatile User currentUser;

    private Session() { }

    public static void login(User user) { start(user); }

    public static void start(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        currentUser = user;
    }

    public static void logout() { end(); }
    public static void end() { currentUser = null; }

    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() { return hasRole(Role.ADMIN); }
    public static boolean hasRole(Role role) {
        return currentUser != null && role != null && currentUser.getRole() == role;
    }

    public static void requireLogin() {
        if (!isLoggedIn()) throw new AuthorizationException("User must be logged in.");
    }

    public static void requireAdmin() { requireRole(Role.ADMIN); }

    public static void requireRole(Role role) {
        requireLogin();
        if (role == null || !hasRole(role)) {
            String required = role == null ? "the requested role" : role.getDisplayName();
            throw new AuthorizationException(required + " access required.");
        }
    }
}
