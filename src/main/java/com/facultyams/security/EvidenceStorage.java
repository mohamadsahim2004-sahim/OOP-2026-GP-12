package com.facultyams.security;

import com.facultyams.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores medical supporting documents outside the source tree under random names.
 * <ul>
 *   <li>only PDF, JPG and PNG up to {@value #MAX_MB} MB are accepted; the file's first bytes must match its extension</li>
 *   <li>the stored name is generated (never the user's file name), so a name can neither collide nor traverse folders</li>
 *   <li>{@link #resolve} refuses anything that is not a name this class generated</li>
 * </ul>
 * The folder is {@code $FOTAMS_UPLOAD_DIR} if set, otherwise {@code ~/.fotams/medical-evidence}.
 */
public final class EvidenceStorage {

    public static final int MAX_MB = 5;
    public static final long MAX_BYTES = MAX_MB * 1024L * 1024L;
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{32}\\.(pdf|jpg|png)$");

    /** Result of {@link #store}: the generated storage name and the cleaned original name. */
    public record Stored(String storedName, String originalName) {
    }

    private final Path root;

    public EvidenceStorage() {
        this(defaultRoot());
    }

    public EvidenceStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    private static Path defaultRoot() {
        String env = System.getenv("FOTAMS_UPLOAD_DIR");
        if (env != null && !env.isBlank()) {
            return Paths.get(env);
        }
        return Paths.get(System.getProperty("user.home"), ".fotams", "medical-evidence");
    }

    /** Checks the file without storing it; throws a message the user can act on. */
    public String validate(Path source) throws ValidationException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new ValidationException("The selected document could not be found.");
        }
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        String normalized = switch (ext) {
            case "pdf" -> "pdf";
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            default -> throw new ValidationException("The document must be a PDF, JPG or PNG file.");
        };
        try {
            long size = Files.size(source);
            if (size == 0) {
                throw new ValidationException("The selected document is empty.");
            }
            if (size > MAX_BYTES) {
                throw new ValidationException("The document is larger than " + MAX_MB + " MB.");
            }
            byte[] head = new byte[8];
            int read;
            try (InputStream in = Files.newInputStream(source)) {
                read = in.readNBytes(head, 0, head.length);
            }
            if (!matchesSignature(normalized, head, read)) {
                throw new ValidationException("The file content does not match a ." + ext + " document.");
            }
        } catch (IOException e) {
            throw new ValidationException("The document could not be read: " + e.getMessage(), e);
        }
        return normalized;
    }

    private static boolean matchesSignature(String ext, byte[] h, int n) {
        return switch (ext) {
            case "pdf" -> n >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "jpg" -> n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "png" -> n >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                    && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
            default -> false;
        };
    }

    /** Validates and copies the file into the storage folder. */
    public Stored store(Path source) throws ValidationException {
        String ext = validate(source);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(root);
            try {
                Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
            } catch (UnsupportedOperationException | IOException ignored) {
                // not a POSIX file system (e.g. Windows): the user profile folder is already private
            }
            Files.copy(source, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ValidationException("The document could not be stored: " + e.getMessage(), e);
        }
        return new Stored(storedName, cleanName(source.getFileName().toString()));
    }

    /** Absolute path of a stored document, or null if the name is not one this class generated or the file is gone. */
    public Path resolve(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            return null;
        }
        Path p = root.resolve(storedName).normalize();
        return p.startsWith(root) && Files.isRegularFile(p) ? p : null;
    }

    public void delete(String storedName) {
        Path p = resolve(storedName);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                System.err.println("Could not delete " + storedName + ": " + e.getMessage());
            }
        }
    }

    private static String cleanName(String name) {
        String n = name.replaceAll("[\\p{Cntrl}]", "").trim();
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }
}