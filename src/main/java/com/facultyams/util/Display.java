package com.facultyams.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text formatting shared by the screens and the exported reports so both always read the same. */
public final class Display {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Display() {
    }

    /** "present" -> "Present", "THEORY" -> "Theory"; null -> "". */
    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String date(LocalDate d) {
        return d == null ? "" : d.toString();
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "" : d.format(DATE_TIME);
    }

    public static String percent(double p) {
        return String.format(Locale.ROOT, "%.1f%%", p);
    }

    public static String orDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    public static String yesNo(boolean b) {
        return b ? "Yes" : "No";
    }
}