package com.facultyams.service;

import com.facultyams.dao.UserDAO;
import com.facultyams.model.User;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Session;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User login(
            String username,
            String password) {

        if (username == null
                || username.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Username is required."
            );
        }

        if (password == null
                || password.isEmpty()) {

            throw new IllegalArgumentException(
                    "Password is required."
            );
        }

        User user =
                userDAO.findByUsername(
                        username.trim()
                );

        if (user == null) {
            throw new IllegalArgumentException(
                    "Invalid username or password."
            );
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "This account is inactive."
            );
        }

        if (!PasswordUtil.matches(
                password,
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Invalid username or password."
            );
        }

        Session.login(user);

        return user;
    }

    public void logout() {
        Session.logout();
    }
}