package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only queries that are SCOPED TO A LECTURER: every query takes the lecturer's id and
 * only returns rows for courses where {@code courses.lecturer_id} is that lecturer.
 *
 * <p>A student is "authorized" for a lecturer when the student is enrolled in, has marks in, or
 * has attendance recorded for one of the lecturer's courses.
 */
public class LecturerAccessDAO {

    public record StudentRow(int studentId, String registrationNo, String fullName,
                             String batch, String status) {
    }

    /** Counts of one student's attendance rows in one course. */
    public record AttendanceAgg(int studentId, String registrationNo, String fullName,
                                int present, int absent, int approvedMedical) {
    }

    public record AttendanceRecord(String component, int sessionNo, LocalDate date,
                                   String status, String medicalStatus) {
    }

    /** Medical record WITHOUT the reason text: the diagnosis never leaves the database. */
    public record MedicalRow(int medicalId, String registrationNo, String fullName, String component,
                             int sessionNo, LocalDate sessionDate, LocalDate submittedDate,
                             String status, String reviewedBy) {
    }

    /** "student s is linked to one of lecturer ? 's courses" - any course. Params: lecturerId. */
    static final String LINK_ANY =
            "EXISTS (SELECT 1 FROM courses c WHERE c.lecturer_id = ? AND (" +
                    "EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id = s.user_id AND e.course_id = c.course_id) " +
                    "OR EXISTS (SELECT 1 FROM marks m WHERE m.student_id = s.user_id AND m.course_id = c.course_id) " +
                    "OR EXISTS (SELECT 1 FROM session_attendance sa JOIN course_component cc " +
                    "ON cc.component_id = sa.component_id " +
                    "WHERE sa.user_id = s.user_id AND cc.course_id = c.course_id)))";

    /** Same, restricted to one course. Params: lecturerId, courseId. */
    static final String LINK_ONE =
            "EXISTS (SELECT 1 FROM courses c WHERE c.lecturer_id = ? AND c.course_id = ? AND (" +
                    "EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id = s.user_id AND e.course_id = c.course_id) " +
                    "OR EXISTS (SELECT 1 FROM marks m WHERE m.student_id = s.user_id AND m.course_id = c.course_id) " +
                    "OR EXISTS (SELECT 1 FROM session_attendance sa JOIN course_component cc " +
                    "ON cc.component_id = sa.component_id " +
                    "WHERE sa.user_id = s.user_id AND cc.course_id = c.course_id)))";

    static final String SQL_OWNS_COURSE =
            "SELECT COUNT(*) FROM courses WHERE course_id = ? AND lecturer_id = ?";

    static final String SQL_STUDENTS_OF_COURSE =
            "SELECT s.user_id, s.registration_no, u.full_name, s.batch, s.student_status " +
                    "FROM students s JOIN users u ON u.user_id = s.user_id " +
                    "WHERE " + LINK_ONE + " ORDER BY s.registration_no";

    static final String SQL_SEARCH_STUDENTS =
            "SELECT s.user_id, s.registration_no, u.full_name, s.batch, s.student_status " +
                    "FROM students s JOIN users u ON u.user_id = s.user_id " +
                    "WHERE " + LINK_ANY + " AND (s.registration_no LIKE ? OR u.full_name LIKE ?) " +
                    "ORDER BY s.registration_no LIMIT 25";

    static final String SQL_STUDENT_IN_COURSE =
            "SELECT COUNT(*) FROM students s WHERE s.user_id = ? AND " + LINK_ONE;

    static final String SQL_STUDENT_AUTHORIZED =
            "SELECT COUNT(*) FROM students s WHERE s.user_id = ? AND " + LINK_ANY;

    static final String SQL_COUNT_STUDENTS =
            "SELECT COUNT(*) FROM students s WHERE " + LINK_ANY;

    static final String SQL_COMPONENT_COUNT =
            "SELECT COUNT(*) FROM course_component WHERE course_id = ?";

    /** Medical only offsets an ABSENT session, so a present session is never counted twice. */
    static final String SQL_ATTENDANCE_AGG =
            "SELECT s.user_id, s.registration_no, u.full_name, " +
                    "SUM(CASE WHEN sa.status IN ('present', 'late') THEN 1 ELSE 0 END) AS present_count, " +
                    "SUM(CASE WHEN sa.status = 'absent' THEN 1 ELSE 0 END) AS absent_count, " +
                    "SUM(CASE WHEN sa.status = 'absent' AND md.approval_status = 'approved' THEN 1 ELSE 0 END) " +
                    "AS approved_medical " +
                    "FROM session_attendance sa " +
                    "JOIN course_component cc ON cc.component_id = sa.component_id " +
                    "JOIN students s ON s.user_id = sa.user_id " +
                    "JOIN users u ON u.user_id = s.user_id " +
                    "LEFT JOIN medical md ON md.attendance_id = sa.attendance_id " +
                    "WHERE cc.course_id = ? " +
                    "GROUP BY s.user_id, s.registration_no, u.full_name " +
                    "ORDER BY s.registration_no";

    static final String SQL_ATTENDANCE_RECORDS =
            "SELECT cc.type, sa.session_no, sa.date, sa.status, md.approval_status " +
                    "FROM session_attendance sa " +
                    "JOIN course_component cc ON cc.component_id = sa.component_id " +
                    "LEFT JOIN medical md ON md.attendance_id = sa.attendance_id " +
                    "WHERE cc.course_id = ? AND sa.user_id = ? " +
                    "ORDER BY cc.type, sa.session_no";

    static final String SQL_MEDICAL_ROWS =
            "SELECT md.medical_id, s.registration_no, u.full_name, cc.type, sa.session_no, " +
                    "sa.date, md.submitted_date, md.approval_status, rv.full_name AS reviewer " +
                    "FROM medical md " +
                    "JOIN session_attendance sa ON sa.attendance_id = md.attendance_id " +
                    "JOIN course_component cc ON cc.component_id = sa.component_id " +
                    "JOIN courses c ON c.course_id = cc.course_id " +
                    "JOIN students s ON s.user_id = sa.user_id " +
                    "JOIN users u ON u.user_id = s.user_id " +
                    "LEFT JOIN users rv ON rv.user_id = md.reviewed_by " +
                    "WHERE c.course_id = ? AND c.lecturer_id = ? " +
                    "ORDER BY md.submitted_date DESC, md.medical_id DESC";

    static final String SQL_PENDING_MEDICAL_COUNT =
            "SELECT COUNT(*) FROM medical md " +
                    "JOIN session_attendance sa ON sa.attendance_id = md.attendance_id " +
                    "JOIN course_component cc ON cc.component_id = sa.component_id " +
                    "JOIN courses c ON c.course_id = cc.course_id " +
                    "WHERE c.lecturer_id = ? AND md.approval_status = 'pending'";

    private static StudentRow student(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StudentRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
    }

    public boolean ownsCourse(int lecturerId, int courseId) throws DatabaseException {
        return JdbcSupport.count(SQL_OWNS_COURSE, courseId, lecturerId) > 0;
    }

    public List<StudentRow> findStudentsOfCourse(int lecturerId, int courseId) throws DatabaseException {
        return JdbcSupport.list(SQL_STUDENTS_OF_COURSE, LecturerAccessDAO::student, lecturerId, courseId);
    }

    public List<StudentRow> searchStudents(int lecturerId, String text) throws DatabaseException {
        String like = "%" + text.trim() + "%";
        return JdbcSupport.list(SQL_SEARCH_STUDENTS, LecturerAccessDAO::student, lecturerId, like, like);
    }

    public boolean isStudentInCourse(int lecturerId, int courseId, int studentId) throws DatabaseException {
        return JdbcSupport.count(SQL_STUDENT_IN_COURSE, studentId, lecturerId, courseId) > 0;
    }

    public boolean isStudentAuthorized(int lecturerId, int studentId) throws DatabaseException {
        return JdbcSupport.count(SQL_STUDENT_AUTHORIZED, studentId, lecturerId) > 0;
    }

    public int countStudents(int lecturerId) throws DatabaseException {
        return JdbcSupport.count(SQL_COUNT_STUDENTS, lecturerId);
    }

    public int countAttendanceComponents(int courseId) throws DatabaseException {
        return JdbcSupport.count(SQL_COMPONENT_COUNT, courseId);
    }

    public List<AttendanceAgg> attendanceByStudent(int courseId) throws DatabaseException {
        return JdbcSupport.list(SQL_ATTENDANCE_AGG, rs -> new AttendanceAgg(
                rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getInt(4), rs.getInt(5), rs.getInt(6)), courseId);
    }

    public List<AttendanceRecord> attendanceRecords(int courseId, int studentId) throws DatabaseException {
        return JdbcSupport.list(SQL_ATTENDANCE_RECORDS, rs -> new AttendanceRecord(
                rs.getString(1), rs.getInt(2), rs.getDate(3).toLocalDate(),
                rs.getString(4), rs.getString(5)), courseId, studentId);
    }

    public List<MedicalRow> medicalRows(int lecturerId, int courseId) throws DatabaseException {
        return JdbcSupport.list(SQL_MEDICAL_ROWS, rs -> new MedicalRow(
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(),
                rs.getString(8), rs.getString(9)), courseId, lecturerId);
    }

    public int countPendingMedical(int lecturerId) throws DatabaseException {
        return JdbcSupport.count(SQL_PENDING_MEDICAL_COUNT, lecturerId);
    }
}
