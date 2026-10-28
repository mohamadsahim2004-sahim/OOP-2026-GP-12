package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Medical requests of ONE student. A request is one or more {@code medical} rows (one per absent session) that
 * share a {@code request_ref}. Every statement joins {@code session_attendance.user_id = studentId}, so another
 * student's rows can never be read, changed or removed through this class.
 */
public class StudentMedicalDAO {

    public record MedicalRow(int medicalId, int attendanceId, String ref, LocalDate sessionDate, LocalDate submitted,
                             String reason, String status, String doctor, String certificateNo, String notes,
                             String files, String remarks) { }

    /** A session the student wants to excuse: its status and how many medical rows already cover it. */
    public record SessionCheck(int attendanceId, LocalDate date, String status, int existingMedical) { }

    public List<MedicalRow> findAll(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(
                "SELECT m.medical_id, m.attendance_id, m.request_ref, sa.date, m.submitted_date, m.reason, "
                        + "m.approval_status, m.doctor, m.certificate_no, m.notes, m.certificate_files, m.reviewer_remarks "
                        + "FROM medical m JOIN session_attendance sa ON sa.attendance_id = m.attendance_id "
                        + "WHERE sa.user_id = ? ORDER BY m.submitted_date DESC, m.medical_id DESC",
                rs -> new MedicalRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getDate(4).toLocalDate(),
                        rs.getDate(5).toLocalDate(), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)), studentId);
    }

    /** Checks of the requested sessions; ids that do not belong to the student are simply not returned. */
    public List<SessionCheck> checkSessions(int studentId, List<Integer> attendanceIds) throws DatabaseException {
        if (attendanceIds.isEmpty()) {
            return List.of();
        }
        String marks = String.join(",", java.util.Collections.nCopies(attendanceIds.size(), "?"));
        Object[] params = new Object[attendanceIds.size() + 1];
        params[0] = studentId;
        for (int i = 0; i < attendanceIds.size(); i++) {
            params[i + 1] = attendanceIds.get(i);
        }
        return StudentDaoSupport.query(
                "SELECT sa.attendance_id, sa.date, sa.status, "
                        + "(SELECT COUNT(*) FROM medical md WHERE md.attendance_id = sa.attendance_id) "
                        + "FROM session_attendance sa WHERE sa.user_id = ? AND sa.attendance_id IN (" + marks + ")",
                rs -> new SessionCheck(rs.getInt(1), rs.getDate(2).toLocalDate(), rs.getString(3), rs.getInt(4)),
                params);
    }

    /** Inserts one pending row per session in a single transaction; any failure rolls everything back. */
    public void insertRequest(int studentId, String ref, List<Integer> attendanceIds, LocalDate submitted,
                              String reason, String doctor, String certificateNo, String notes, String files)
            throws DatabaseException {
        String sql = "INSERT INTO medical (attendance_id, reviewed_by, submitted_date, reason, approval_status, "
                + "request_ref, doctor, certificate_no, notes, certificate_files) "
                + "SELECT sa.attendance_id, NULL, ?, ?, 'pending', ?, ?, ?, ?, ? FROM session_attendance sa "
                + "WHERE sa.attendance_id = ? AND sa.user_id = ?";
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean auto = con.getAutoCommit();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int id : attendanceIds) {
                    ps.setDate(1, Date.valueOf(submitted));
                    ps.setString(2, reason);
                    ps.setString(3, ref);
                    ps.setString(4, doctor);
                    ps.setString(5, certificateNo);
                    ps.setString(6, notes);
                    ps.setString(7, files);
                    ps.setInt(8, id);
                    ps.setInt(9, studentId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Session " + id + " does not belong to this student.");
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the medical request: " + e.getMessage(), e);
        }
    }

    /** Removes the student's own PENDING rows of a request; reviewed requests cannot be withdrawn. */
    public int deletePending(int studentId, String ref) throws DatabaseException {
        return StudentDaoSupport.update(
                "DELETE m FROM medical m JOIN session_attendance sa ON sa.attendance_id = m.attendance_id "
                        + "WHERE sa.user_id = ? AND m.request_ref = ? AND m.approval_status = 'pending'", studentId, ref);
    }
}
