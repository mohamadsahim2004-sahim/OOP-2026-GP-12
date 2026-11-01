package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.RecentSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Counts and recent activity shown on the Technical Officer dashboard. */
public class TechnicalDashboardDAO {

    public int countStudents() throws DatabaseException {
        return scalar("SELECT COUNT(*) FROM students s JOIN users u ON u.user_id = s.user_id WHERE u.is_active = TRUE");
    }

    public int countAttendanceRecords() throws DatabaseException {
        return scalar("SELECT COUNT(*) FROM session_attendance");
    }

    private int scalar(String sql) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the dashboard figures.", e);
        }
    }

    /** The most recently recorded sessions with their present / absent / late totals. */
    public List<RecentSession> recentSessions(int limit) throws DatabaseException {
        String sql = "SELECT a.date, c.course_code, cc.type, a.session_no, "
                   + "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END), "
                   + "SUM(CASE WHEN a.status = 'absent' THEN 1 ELSE 0 END), "
                   + "SUM(CASE WHEN a.status = 'late' THEN 1 ELSE 0 END) "
                   + "FROM session_attendance a "
                   + "JOIN course_component cc ON cc.component_id = a.component_id "
                   + "JOIN courses c ON c.course_id = cc.course_id "
                   + "GROUP BY a.component_id, a.session_no, a.date, c.course_code, cc.type "
                   + "ORDER BY a.date DESC, MAX(a.attendance_id) DESC LIMIT ?";
        List<RecentSession> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RecentSession(rs.getDate(1).toLocalDate(), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the recent attendance.", e);
        }
        return list;
    }

    /** Records saved for sessions dated today with the given status (present / absent / late). */
    public int countOnDate(java.time.LocalDate date, String status) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM session_attendance WHERE date = ? AND status = ?")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read today's attendance.", e);
        }
    }
}