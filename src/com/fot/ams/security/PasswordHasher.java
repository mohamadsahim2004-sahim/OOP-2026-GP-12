package com.fot.ams.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordHasher provides SHA-256 password hashing to satisfy
 * SRS Non-Functional Requirement NFR-05 (plain-text storage is prohibited).
 */
public class PasswordHasher {

    /**
     * Computes the SHA-256 hash of a plain text password.
     *
     * @param plainText The plain text password
     * @return Hexadecimal encoded SHA-256 hash string
     */
    public static String hashPassword(String plainText) {
        if (plainText == null) {
            plainText = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in environment", e);
        }
    }

    /**
     * Verifies if a plain text password matches a stored SHA-256 hash.
     *
     * @param plainText The plain text entered by user
     * @param storedHash The hex hash stored in database
     * @return true if password matches, false otherwise
     */
    public static boolean verify(String plainText, String storedHash) {
        if (plainText == null || storedHash == null) {
            return false;
        }
        String computedHash = hashPassword(plainText);
        return computedHash.equalsIgnoreCase(storedHash);
    }
}
