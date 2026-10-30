package com.facultyams.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Attendance;
import java.util.Map;
import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.AttendanceRow;
import com.facultyams.model.SheetRow;
import com.facultyams.model.SummaryCounts;
import com.facultyams.util.SqlFilter;

public class AttendanceDAO {

 public boolean addAttendance(Attendance a) {
 String sql = "INSERT INTO session_attendance (user_id, component_id, session_no, date, status) VALUES (?, ?, ?, ?, ?)";

 try (Connection con = DatabaseConnection.getConnection();
PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, a.getUserId());
        ps.setInt(2, a.getComponentId());
        ps.setInt(3, a.getSessionNo());
        ps.setDate(4, java.sql.Date.valueOf(a.getDate()));
        ps.setString(5, a.getStatus());
        ps.executeUpdate();
    return true;

        } catch (SQLException | DatabaseException e) {
                 System.out.println("Error: " + e.getMessage());
        return false;
    }
 }

        public List<Attendance> getAttendanceByUser(int userId) {
            List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM session_attendance WHERE user_id = ?";

        try (Connection con = DatabaseConnection.getConnection();       
            PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
        while (rs.next()) {
         Attendance a = new Attendance(
             rs.getInt("attendance_id"),
             rs.getInt("user_id"),
             rs.getInt("component_id"),
            rs.getInt("session_no"),
            rs.getDate("date").toLocalDate(),
            rs.getString("status")
                                    );
                list.add(a);
}

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
    }
        return list;
    }

    public int countPresent(int userId, int componentId) {
        String sql = "SELECT COUNT(*) FROM session_attendance "
            + "WHERE user_id = ? AND component_id = ? AND status = 'present'";

        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, componentId);
            ResultSet rs = ps.executeQuery();
    if (rs.next()) {
    return rs.getInt(1);
 }

        } catch (SQLException | DatabaseException e) {
        System.out.println("Error: " + e.getMessage());
 }
        return 0;
 }

    public List<Integer> getStudentIdsByComponent(int componentId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT DISTINCT user_id FROM session_attendance WHERE component_id = ?";

            try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1    , componentId);
                ResultSet rs = ps.executeQuery();
             while (rs.next()) {
            ids.add(rs.getInt("user_id"));
 }

            }       catch (SQLException | DatabaseException e) {
 System.out.println("Error: " + e.getMessage());
 }
    return ids;
 }

        public boolean updateStatus(int attendanceId, String status) {
    if (!status.equals("present") && !status.equals("absent") && !status.equals("late")) {
            throw new IllegalArgumentException("Status must be present, absent or late");
 }
        String sql = "UPDATE session_attendance SET status = ? WHERE attendance_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, status);
        ps.setInt(2, attendanceId);
        ps.executeUpdate();
    return true;

 } catch (SQLException | DatabaseException e) {
 System.out.println("Error: " + e.getMessage());
 return false;
 }
}
    // find the attendance_id of one session 
    public int findAttendanceId(int userId, int componentId, int sessionNo) {
        String sql = "SELECT attendance_id FROM session_attendance "
                   + "WHERE user_id = ? AND component_id = ? AND session_no = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, componentId);
            ps.setInt(3, sessionNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("attendance_id");
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return -1;
    }

    //  every student with the name
    public List<Object[]> getAllStudents() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT s.user_id, u.full_name FROM students s "
                   + "JOIN users u ON s.user_id = u.user_id ORDER BY s.user_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{rs.getInt("user_id"), rs.getString("full_name")});
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return rows;
    }

    //  total number of students
    public int countStudents() {
        String sql = "SELECT COUNT(*) FROM students";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return 0;
    }

    //how many records on one date have this status
    public int countOnDate(LocalDate date, String status) {
        String sql = "SELECT COUNT(*) FROM session_attendance WHERE date = ? AND status = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setString(2, status);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return 0;
    }

    // ------------------------------------------------------------------ Technical Officer queries

    public int countLate(int userId, int componentId) {
        String sql = "SELECT COUNT(*) FROM session_attendance "
                   + "WHERE user_id = ? AND component_id = ? AND status = 'late'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, componentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return 0;
    }

    /** Students of the batch with whatever is already saved for this component and session number. */
    public List<SheetRow> loadSheet(int componentId, int sessionNo, String batch) throws DatabaseException {
        String sql = "SELECT s.user_id, u.full_name, s.registration_no, s.batch, a.attendance_id, a.status, a.date, "
                   + "(SELECT COUNT(*) FROM medical m WHERE m.attendance_id = a.attendance_id) AS medicals "
                   + "FROM students s JOIN users u ON u.user_id = s.user_id "
                   + "LEFT JOIN session_attendance a ON a.user_id = s.user_id AND a.component_id = ? AND a.session_no = ? "
                   + "WHERE u.is_active = TRUE AND s.batch = ? ORDER BY s.registration_no";
        List<SheetRow> rows = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, componentId);
            ps.setInt(2, sessionNo);
            ps.setString(3, batch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int attendanceId = rs.getInt(5);
                    boolean recorded = !rs.wasNull();
                    java.sql.Date d = rs.getDate(7);
                    rows.add(new SheetRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            recorded ? attendanceId : null, rs.getString(6),
                            d == null ? null : d.toLocalDate(), rs.getInt(8) > 0));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the attendance sheet.", e);
        }
        return rows;
    }

    /**
     * Saves one whole session in a single transaction. A student that already has a row for this
     * component and session number is updated (the unique key prevents duplicates), otherwise inserted.
     *
     * @return number of students saved
     */
    public int saveSession(int componentId, int sessionNo, LocalDate date, Map<Integer, String> statusByStudent)
            throws DatabaseException {
        String sql = "INSERT INTO session_attendance (user_id, component_id, session_no, date, status) "
                   + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE date = VALUES(date), status = VALUES(status)";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Map.Entry<Integer, String> e : statusByStudent.entrySet()) {
                    ps.setInt(1, e.getKey());
                    ps.setInt(2, componentId);
                    ps.setInt(3, sessionNo);
                    ps.setDate(4, java.sql.Date.valueOf(date));
                    ps.setString(5, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
            return statusByStudent.size();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the attendance (nothing was saved): " + e.getMessage(), e);
        }
    }

    private static final String HISTORY_FROM =
            "FROM session_attendance a "
          + "JOIN students s ON s.user_id = a.user_id "
          + "JOIN users u ON u.user_id = a.user_id "
          + "JOIN course_component cc ON cc.component_id = a.component_id "
          + "JOIN courses c ON c.course_id = cc.course_id "
          + "LEFT JOIN medical m ON m.attendance_id = a.attendance_id ";

    private static SqlFilter toSqlFilter(AttendanceFilter f, boolean withStatus) {
        SqlFilter sf = new SqlFilter()
                .andIf(f.studentId(), "a.user_id = ?")
                .containsAny(f.student(), "u.full_name", "s.registration_no", "u.username")
                .andIf(f.courseId(), "c.course_id = ?")
                .andIf(f.componentId(), "a.component_id = ?")
                .andIf(f.batch(), "s.batch = ?")
                .andIf(f.from(), "a.date >= ?")
                .andIf(f.to(), "a.date <= ?");
        if (withStatus) {
            sf.andIf(f.status(), "a.status = ?");
        }
        return sf;
    }

    /** Saved attendance, newest first (at most {@code limit} rows). */
    public List<AttendanceRow> findHistory(AttendanceFilter f, int limit) throws DatabaseException {
        SqlFilter sf = toSqlFilter(f, true);
        String sql = "SELECT a.attendance_id, a.user_id, u.full_name, s.registration_no, s.batch, c.course_code, "
                   + "c.course_name, cc.type, a.session_no, a.date, a.status, m.approval_status "
                   + HISTORY_FROM + sf.where()
                   + " ORDER BY a.date DESC, s.registration_no, c.course_code, cc.type, a.session_no LIMIT ?";
        List<AttendanceRow> rows = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(sf.bind(ps, 1), limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AttendanceRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getInt(9),
                            rs.getDate(10).toLocalDate(), rs.getString(11), rs.getString(12)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the attendance records.", e);
        }
        return rows;
    }

    /** Per student and component counts; the date range, course, batch and student criteria all apply. */
    public List<SummaryCounts> findSummaryCounts(AttendanceFilter f) throws DatabaseException {
        SqlFilter sf = toSqlFilter(f, false);
        String sql = "SELECT a.user_id, u.full_name, s.registration_no, s.batch, a.component_id, c.course_code, "
                   + "c.course_name, cc.type, "
                   + "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END), "
                   + "SUM(CASE WHEN a.status = 'late' THEN 1 ELSE 0 END), "
                   + "SUM(CASE WHEN a.status = 'absent' THEN 1 ELSE 0 END), "
                   + "SUM(CASE WHEN a.status = 'absent' AND m.approval_status = 'approved' THEN 1 ELSE 0 END), "
                   + "COUNT(a.attendance_id) "
                   + HISTORY_FROM + sf.where()
                   + " GROUP BY a.user_id, u.full_name, s.registration_no, s.batch, a.component_id, c.course_code, "
                   + "c.course_name, cc.type ORDER BY c.course_code, cc.type, s.registration_no";
        List<SummaryCounts> rows = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            sf.bind(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new SummaryCounts(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8),
                            rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not calculate the attendance summary.", e);
        }
        return rows;
    }

    /** Updates one record's status; returns false if no such record exists. */
    public boolean updateStatusChecked(int attendanceId, String status) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE session_attendance SET status = ? WHERE attendance_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, attendanceId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Could not correct the attendance record.", e);
        }
    }
}