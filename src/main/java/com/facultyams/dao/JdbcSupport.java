package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers shared by the Lecturer-module DAOs. Unlike a "log and return 0" style, every
 * failure is reported as a {@link DatabaseException}, so a database problem can never be
 * mistaken for real data (for example "0 % attendance").
 */
final class JdbcSupport {

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private JdbcSupport() {
    }

    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** First column of the first row as an int (for SELECT COUNT(*)); 0 when there is no row. */
    static int count(String sql, Object... params) throws DatabaseException {
        List<Integer> rows = list(sql, rs -> rs.getInt(1), params);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    /** Runs one INSERT / UPDATE / DELETE and returns the number of rows it matched. */
    static int update(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Runs one statement on an open transaction connection. */
    static int update(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    static int count(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    static DatabaseException wrap(SQLException e) {
        return new DatabaseException("Database error: " + e.getMessage(), e);
    }
}
