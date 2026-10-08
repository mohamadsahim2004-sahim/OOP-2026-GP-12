package com.facultyams.security;

import com.facultyams.model.User;

public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return isLoggedIn()
                && currentUser.getRole() == Role.ADMIN;
    }

    public static boolean hasRole(Role role) {
        return isLoggedIn()
                && currentUser.getRole() == role;
    }

    public static void requireLogin() {

        if (!isLoggedIn()) {
            throw new SecurityException(
                    "User must be logged in."
            );
        }
    }

    public static void requireAdmin() {

        requireLogin();

        if (!isAdmin()) {
            throw new SecurityException(
                    "Administrator access required."
            );
        }
    }
}