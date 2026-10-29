package com.facultyams.util;

import com.facultyams.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Reusable input validation helpers. Methods named require...() throw a
 * ValidationException with a user-friendly message; is...() methods return boolean.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** Sri Lankan style: 0XXXXXXXXX or +94XXXXXXXXX */
    private static final Pattern PHONE = Pattern.compile("^(0\\d{9}|\\+94\\d{9})$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{3,30}$");
    /** e.g. ICT2132, ENG1112, TMS3213 */
    private static final Pattern COURSE_CODE = Pattern.compile("^[A-Z]{2,4}\\d{4}$");
    /** e.g. DICT, DET, DBST */
    private static final Pattern DEPARTMENT_CODE = Pattern.compile("^[A-Z]{2,10}$");

    public static final int MIN_PASSWORD_LENGTH = 6;

    private ValidationUtil() {
        // utility class
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return !isBlank(phone) && PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        return !isBlank(username) && USERNAME.matcher(username.trim()).matches();
    }

    public static boolean isValidCourseCode(String code) {
        return !isBlank(code) && COURSE_CODE.matcher(code.trim()).matches();
    }

    public static boolean isValidDepartmentCode(String code) {
        return !isBlank(code) && DEPARTMENT_CODE.matcher(code.trim()).matches();
    }

    // ----- throwing helpers -----

    /** Returns the trimmed value, or throws if it is empty. */
    public static String requireNotBlank(String value, String fieldName) throws ValidationException {
        if (isBlank(value)) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static void requireMaxLength(String value, int max, String fieldName) throws ValidationException {
        if (value != null && value.trim().length() > max) {
            throw new ValidationException(fieldName + " must be at most " + max + " characters.");
        }
    }

    public static void requireInRange(int value, int min, int max, String fieldName) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max + ".");
        }
    }

    public static void requirePositiveId(int id, String fieldName) throws ValidationException {
        if (id <= 0) {
            throw new ValidationException("Invalid " + fieldName + ".");
        }
    }

    public static void requireValidPassword(String password) throws ValidationException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(password.trim())) {
            throw new ValidationException("Password must not start or end with spaces.");
        }
    }

    /**
     * Parses an integer typed in a text field.
     *
     * @throws ValidationException if the text is empty or not a whole number
     */
    public static int parseInt(String text, String fieldName) throws ValidationException {
        String value = requireNotBlank(text, fieldName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a whole number.", e);
        }
    }

    /** Trims the value and converts "" to null (for optional columns). */
    public static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}