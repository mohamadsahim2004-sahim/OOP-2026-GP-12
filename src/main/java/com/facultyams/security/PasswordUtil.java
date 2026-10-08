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
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 65_536;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // utility class
    }

    /** Creates a new salted hash for the given plain-text password. */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    /**
     * Checks a plain-text password against a stored hash.
     * Returns false (never throws) for a null or malformed stored hash.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(plainPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // bad number or bad Base64 -> treat as non-matching password
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing algorithm is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Helper for the team: prints a hash so new sample users can be added to
     * database/sample_data.sql.  Usage: java com.facultyams.security.PasswordUtil myPassword
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java com.facultyams.security.PasswordUtil <password>");
            return;
        }
        System.out.println(hashPassword(args[0]));
    }
}