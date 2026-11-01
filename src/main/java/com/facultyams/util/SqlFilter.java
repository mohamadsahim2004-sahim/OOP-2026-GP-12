package com.facultyams.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE part of a query from optional criteria. Values are never concatenated into the SQL:
 * only the fixed clause text is, and every value is bound as a parameter.
 */
public final class SqlFilter {

    private final StringBuilder where = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    /** Adds {@code clause} (with ? placeholders) joined by AND. */
    public SqlFilter and(String clause, Object... values) {
        where.append(where.length() == 0 ? " WHERE " : " AND ").append(clause);
        for (Object v : values) {
            params.add(v instanceof LocalDate d ? java.sql.Date.valueOf(d) : v);
        }
        return this;
    }

    /** Adds {@code clause} only when {@code value} is not null / blank. */
    public SqlFilter andIf(Object value, String clause) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return this;
        }
        return and(clause, value instanceof String s ? s.trim() : value);
    }

    /** Case-insensitive "contains" match of {@code text} against any of the columns (blank text adds nothing). */
    public SqlFilter containsAny(String text, String... columns) {
        if (text == null || text.isBlank()) {
            return this;
        }
        StringBuilder clause = new StringBuilder("(");
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            clause.append(i == 0 ? "" : " OR ").append(columns[i]).append(" LIKE ?");
            values[i] = "%" + escapeLike(text.trim()) + "%";
        }
        return and(clause.append(")").toString(), values);
    }

    public String where() {
        return where.toString();
    }

    /** Binds the collected values to {@code ps}, starting at parameter {@code firstIndex}; returns the next free index. */
    public int bind(PreparedStatement ps, int firstIndex) throws SQLException {
        int index = firstIndex;
        for (Object p : params) {
            ps.setObject(index++, p);
        }
        return index;
    }

    /** Escapes LIKE wildcards so user text is matched literally. */
    public static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}