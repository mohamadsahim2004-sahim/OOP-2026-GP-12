package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Notice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for notices (table {@code notices}, created by database/migrations/001_notices_timetables.sql).
 * SQL only - rules and permissions live in NoticeService. Every statement is a PreparedStatement.
 */
public class NoticeDAO {

    private static final String SELECT =
            "SELECT n.notice_id, n.title, n.content, n.notice_date, n.created_by, u.full_name AS created_by_name "
                    + "FROM notices n LEFT JOIN users u ON u.user_id = n.created_by ";

    /** Newest notice first. */
    private static final String ORDER = "ORDER BY n.notice_date DESC, n.notice_id DESC";

    /** SQL state MySQL reports for "table doesn't exist". */
    private static final String SQL_STATE_NO_TABLE = "42S02";

    public List<Notice> findAll() throws DatabaseException {
        return findMany(SELECT + ORDER);
    }

    /** Notices whose title or content contains the keyword (blank keyword = every notice). */
    public List<Notice> search(String keyword) throws DatabaseException {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String like = likePattern(keyword);
        return findMany(SELECT + "WHERE (n.title LIKE ? ESCAPE '!' OR n.content LIKE ? ESCAPE '!') " + ORDER,
                like, like);
    }

    public Notice findById(int noticeId) throws DatabaseException {
        List<Notice> list = findMany(SELECT + "WHERE n.notice_id = ?", noticeId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int countAll() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM notices";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw failure("Failed to count notices.", e);
        }
    }

    /** Inserts the notice, stores the generated id in it and returns that id. */
    public int insert(Notice notice) throws DatabaseException {
        String sql = "INSERT INTO notices (title, content, notice_date, created_by) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, notice.getTitle());
            ps.setString(2, notice.getContent());
            ps.setObject(3, notice.getNoticeDate());
            if (notice.getCreatedBy() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, notice.getCreatedBy());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notice.setNoticeId(keys.getInt(1));
                }
            }
            return notice.getNoticeId();
        } catch (SQLException e) {
            throw failure("Failed to add notice: " + e.getMessage(), e);
        }
    }

    /** Updates title, content and date. The author (created_by) is never changed. */
    public void update(Notice notice) throws DatabaseException {
        String sql = "UPDATE notices SET title = ?, content = ?, notice_date = ? WHERE notice_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, notice.getTitle());
            ps.setString(2, notice.getContent());
            ps.setObject(3, notice.getNoticeDate());
            ps.setInt(4, notice.getNoticeId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("Failed to update notice: " + e.getMessage(), e);
        }
    }

    public void delete(int noticeId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM notices WHERE notice_id = ?")) {
            ps.setInt(1, noticeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("Failed to delete notice: " + e.getMessage(), e);
        }
    }

    // ----- helpers -----

    /** "%keyword%" with LIKE wildcards in the keyword escaped, so typing "50%" or "_" searches for those characters. */
    private static String likePattern(String keyword) {
        String escaped = keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** A DatabaseException that tells the user to run the migration when the notices table is missing. */
    private static DatabaseException failure(String message, SQLException e) {
        if (SQL_STATE_NO_TABLE.equals(e.getSQLState())) {
            return new DatabaseException("The notices table is missing. Run "
                    + "database/migrations/001_notices_timetables.sql first. (" + e.getMessage() + ")", e);
        }
        return new DatabaseException(message, e);
    }

    private List<Notice> findMany(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Notice> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw failure("Failed to read notices.", e);
        }
    }

    private static Notice mapRow(ResultSet rs) throws SQLException {
        Notice notice = new Notice(
                rs.getInt("notice_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getObject("notice_date", LocalDate.class));
        int createdBy = rs.getInt("created_by");
        notice.setCreatedBy(rs.wasNull() ? null : createdBy);
        notice.setCreatedByName(rs.getString("created_by_name"));
        return notice;
    }
}