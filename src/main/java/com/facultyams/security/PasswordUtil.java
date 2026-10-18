package com.facultyams.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Password hashing utility using SHA-256.
 * Plain-text passwords are never stored.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static boolean matches(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        return hashPassword(password).equalsIgnoreCase(hashedPassword);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return matches(plainPassword, hashedPassword);
    }
}