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
import com.facultyams.model.Medical;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import com.facultyams.model.EvidenceRef;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.MedicalRow;
import com.facultyams.model.SessionInfo;
import com.facultyams.model.SubmittableSession;
import com.facultyams.util.SqlFilter;


public class MedicalDAO {

    public boolean addMedical(Medical m) {
        String sql = "INSERT INTO MEDICAL (attendance_id, reviewed_by, submitted_date, reason, approval_status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getAttendanceId());
            ps.setInt(2, m.getReviewedId());
            ps.setDate(3, java.sql.Date.valueOf(m.getSubmittedDate()));
            ps.setString(4, m.getReason());
            ps.setString(5, m.getApprovalStatus());
            ps.executeUpdate();
            return true;
        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public List<Medical> getMedicalsByUser(int userId) {
        List<Medical> list = new ArrayList<>();
        String sql = "SELECT m.* FROM MEDICAL m "
                   + "JOIN session_attendance a ON m.attendance_id = a.attendance_id "
                   + "WHERE a.user_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Medical m = new Medical(
                    rs.getInt("medical_id"),
                    rs.getInt("attendance_id"),
                    rs.getInt("reviewed_by"),
                    rs.getDate("submitted_date").toLocalDate(),
                    rs.getString("reason"),
                    rs.getString("approval_status")
                );
                list.add(m);
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateApprovalStatus(int medicalId, String status, int reviewedBy) {
        if (!status.equals("pending") && !status.equals("approved") && !status.equals("rejected")) {
            throw new IllegalArgumentException("Approval status must be pending, approved or rejected");
        }
        String sql = "UPDATE MEDICAL SET approval_status = ?, reviewed_by = ? WHERE medical_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reviewedBy);
            ps.setInt(3, medicalId);
            ps.executeUpdate();
            return true;

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public int countApproved(int userId, int componentId) {
        String sql = "SELECT COUNT(*) FROM medical m "
                   + "JOIN session_attendance a ON m.attendance_id = a.attendance_id "
                   + "WHERE a.user_id = ? AND a.component_id = ? AND a.status = 'absent' AND m.approval_status = 'approved'";

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

    public String getAttendanceStatus(int attendanceId) {
        String sql = "SELECT status FROM session_attendance WHERE attendance_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, attendanceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
    //  medicals of all students (or one student), filtered using status and date range.
    public List<Object[]> searchMedicals(String status, int studentId, LocalDate from, LocalDate to) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT m.medical_id, a.user_id, u.full_name, a.session_no, "
                   + "m.submitted_date, m.reason, m.approval_status "
                   + "FROM medical m "
                   + "JOIN session_attendance a ON m.attendance_id = a.attendance_id "
                   + "JOIN users u ON a.user_id = u.user_id "
                   + "WHERE (? = 'all' OR m.approval_status = ?) "
                   + "AND (? = 0 OR a.user_id = ?) "
                   + "AND m.submitted_date BETWEEN ? AND ? "
                   + "ORDER BY m.medical_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, studentId);
            ps.setInt(4, studentId);
            ps.setDate(5, java.sql.Date.valueOf(from));
            ps.setDate(6, java.sql.Date.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("medical_id"),
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getInt("session_no"),
                    rs.getDate("submitted_date"),
                    rs.getString("reason"),
                    rs.getString("approval_status")
                });
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return rows;
    }

    // how many medicals have this status 
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM medical WHERE approval_status = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
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

    private static final String MEDICAL_SELECT =
            "SELECT m.medical_id, m.attendance_id, s.user_id, u.full_name, s.registration_no, s.batch, "
          + "c.course_code, c.course_name, cc.type, a.session_no, a.date, a.status, m.submitted_date, m.reason, "
          + "m.evidence_name, m.evidence_file, m.approval_status, r.full_name, m.decided_at, m.rejection_reason "
          + "FROM medical m "
          + "JOIN session_attendance a ON a.attendance_id = m.attendance_id "
          + "JOIN students s ON s.user_id = a.user_id "
          + "JOIN users u ON u.user_id = s.user_id "
          + "JOIN course_component cc ON cc.component_id = a.component_id "
          + "JOIN courses c ON c.course_id = cc.course_id "
          + "LEFT JOIN users r ON r.user_id = m.reviewed_by ";

    /** Medical submissions matching the filter, newest first (dates in the filter apply to the submission date). */
    public List<MedicalRow> findMedicals(MedicalFilter f, int limit) throws DatabaseException {
        SqlFilter sf = new SqlFilter()
                .andIf(f.studentId(), "a.user_id = ?")
                .containsAny(f.student(), "u.full_name", "s.registration_no", "u.username")
                .andIf(f.courseId(), "c.course_id = ?")
                .andIf(f.batch(), "s.batch = ?")
                .andIf(f.status(), "m.approval_status = ?")
                .andIf(f.from(), "m.submitted_date >= ?")
                .andIf(f.to(), "m.submitted_date <= ?");
        String sql = MEDICAL_SELECT + sf.where() + " ORDER BY m.submitted_date DESC, m.medical_id DESC LIMIT ?";
        List<MedicalRow> rows = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(sf.bind(ps, 1), limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp decided = rs.getTimestamp(19);
                    String file = rs.getString(16);
                    rows.add(new MedicalRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getInt(10),
                            rs.getDate(11).toLocalDate(), rs.getString(12), rs.getDate(13).toLocalDate(),
                            rs.getString(14), rs.getString(15), file != null && !file.isBlank(),
                            rs.getString(17), rs.getString(18),
                            decided == null ? null : decided.toLocalDateTime(), rs.getString(20)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the medical records.", e);
        }
        return rows;
    }

    /** Totals per status, always containing pending, approved and rejected (0 when none). */
    public Map<String, Integer> countByStatus() throws DatabaseException {
        Map<String, Integer> totals = new LinkedHashMap<>();
        totals.put("pending", 0);
        totals.put("approved", 0);
        totals.put("rejected", 0);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT approval_status, COUNT(*) FROM medical GROUP BY approval_status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                totals.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not count the medical records.", e);
        }
        return totals;
    }

    /** The attendance record a medical would be filed against, or null if it does not exist. */
    public SessionInfo findSession(int attendanceId) throws DatabaseException {
        String sql = "SELECT a.attendance_id, a.user_id, a.status, a.date, "
                   + "(SELECT COUNT(*) FROM medical m WHERE m.attendance_id = a.attendance_id) "
                   + "FROM session_attendance a WHERE a.attendance_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SessionInfo(rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getDate(4).toLocalDate(), rs.getInt(5) > 0);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the attendance record.", e);
        }
        return null;
    }

    /** Absent sessions of the student that have no medical yet, newest first. */
    public List<SubmittableSession> findSubmittable(int studentId) throws DatabaseException {
        String sql = "SELECT a.attendance_id, a.session_no, a.date, c.course_code, c.course_name, cc.type "
                   + "FROM session_attendance a "
                   + "JOIN course_component cc ON cc.component_id = a.component_id "
                   + "JOIN courses c ON c.course_id = cc.course_id "
                   + "WHERE a.user_id = ? AND a.status = 'absent' "
                   + "AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id = a.attendance_id) "
                   + "ORDER BY a.date DESC, c.course_code, a.session_no";
        List<SubmittableSession> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SubmittableSession(rs.getInt(1), rs.getInt(2), rs.getDate(3).toLocalDate(),
                            rs.getString(4), rs.getString(5), rs.getString(6)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the absent sessions.", e);
        }
        return list;
    }

    /** Stores one new pending submission (reviewer and decision fields stay empty) and returns its id. */
    public int insertSubmission(Medical m) throws DatabaseException {
        insertSubmissions(List.of(m));
        return 1;
    }

    /** Stores several pending submissions in one transaction; all are saved or none. */
    public int insertSubmissions(List<Medical> list) throws DatabaseException {
        String sql = "INSERT INTO medical (attendance_id, reviewed_by, submitted_by, submitted_date, reason, "
                   + "evidence_name, evidence_file, approval_status) VALUES (?, NULL, ?, ?, ?, ?, ?, 'pending')";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Medical m : list) {
                    ps.setInt(1, m.getAttendanceId());
                    ps.setInt(2, m.getSubmittedBy());
                    ps.setDate(3, java.sql.Date.valueOf(m.getSubmittedDate()));
                    ps.setString(4, m.getReason());
                    ps.setString(5, m.getEvidenceName());
                    ps.setString(6, m.getEvidenceFile());
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
                return list.size();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException("A medical has already been submitted for one of these sessions.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the medical submission (nothing was saved).", e);
        }
    }

    /** Removes a submission that was just inserted but whose document could not be stored. */
    public void deleteSubmission(int medicalId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM medical WHERE medical_id = ? AND approval_status = 'pending'")) {
            ps.setInt(1, medicalId);
            ps.executeUpdate();
        } catch (SQLException | DatabaseException e) {
            System.err.println("Could not roll back medical " + medicalId + ": " + e.getMessage());
        }
    }

    /**
     * Records a decision. The WHERE clause only matches a still-pending medical, so a second decision
     * on the same medical changes nothing and this returns false.
     */
    public boolean decide(int medicalId, String status, int reviewerId, String rejectionReason) throws DatabaseException {
        String sql = "UPDATE medical SET approval_status = ?, reviewed_by = ?, decided_at = ?, rejection_reason = ? "
                   + "WHERE medical_id = ? AND approval_status = 'pending'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reviewerId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, rejectionReason);
            ps.setInt(5, medicalId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the decision.", e);
        }
    }

    /** Current status of a medical, or null if it does not exist. */
    public String statusOf(int medicalId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT approval_status FROM medical WHERE medical_id = ?")) {
            ps.setInt(1, medicalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the medical.", e);
        }
    }

    /** Storage reference of a medical's document (null if the medical does not exist or has no document). */
    public EvidenceRef findEvidence(int medicalId) throws DatabaseException {
        String sql = "SELECT m.medical_id, a.user_id, m.evidence_file, m.evidence_name FROM medical m "
                   + "JOIN session_attendance a ON a.attendance_id = m.attendance_id WHERE m.medical_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, medicalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(3) != null) {
                    return new EvidenceRef(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the medical document.", e);
        }
        return null;
    }
}