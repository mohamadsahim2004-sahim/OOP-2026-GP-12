package com.facultyams.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password hashing using salted PBKDF2 (built into the JDK, no extra dependency).
 * Plain-text passwords are never stored.
 *
 * Stored format:  pbkdf2$&lt;iterations&gt;$&lt;base64 salt&gt;$&lt;base64 hash&gt;
 */
public final class PasswordUtil package com.facultyams.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {

        if (password == null) {
            throw new IllegalArgumentException(
                    "Password cannot be null"
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            password.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : hash) {
                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm unavailable",
                    e
            );
        }
    }

    public static boolean matches(
            String password,
            String hashedPassword) {

        if (password == null || hashedPassword == null) {
            return false;
        }

        return hashPassword(password)
                .equalsIgnoreCase(hashedPassword);
    }
}