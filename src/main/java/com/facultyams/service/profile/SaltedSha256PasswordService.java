package com.facultyams.service.profile;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** SHA-256 password hashing with a unique per-update random salt and constant-time comparison. */
public final class SaltedSha256PasswordService implements PasswordService {
    private final SecureRandom random = new SecureRandom();

    @Override public void changePassword(String storedHash, char[] currentPassword, char[] newPassword,
                                         java.util.function.Consumer<String> storeHash) {
        if (storedHash != null && !storedHash.isBlank() && !matches(currentPassword, storedHash)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] digest = hash(newPassword, salt);
        try {
            storeHash.accept(Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(digest));
        } finally {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(digest, (byte) 0);
        }
    }

    private boolean matches(char[] password, String storedHash) {
        try {
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) return false;
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual = hash(password, salt);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(salt, (byte) 0);
                Arrays.fill(expected, (byte) 0);
                Arrays.fill(actual, (byte) 0);
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] hash(char[] password, byte[] salt) {
        ByteBuffer encoded = null;
        byte[] bytes = null;
        try {
            encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password));
            bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(bytes);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IllegalArgumentException("Password contains invalid characters.", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        } finally {
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
            if (encoded != null && encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
        }
    }
}
