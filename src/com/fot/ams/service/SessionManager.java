package com.fot.ams.service;

import com.fot.ams.model.User;

import java.time.LocalDateTime;

/**
 * SessionManager maintains the active user session in memory.
 * Implements FR-AUTH-05 (secure logout terminates active session).
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startSession(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
    }

    public void endSession() {
        this.currentUser = null;
        this.loginTime = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}
