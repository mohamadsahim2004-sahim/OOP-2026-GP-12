package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.student.StudentAssignment;
import com.facultyams.model.student.StudentSubmissionDetails;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Assignment and submission queries restricted to a student's enrolled courses. */
public class StudentAssignmentDAO {

    private static final String ASSIGNMENT_SELECT =
            "SELECT a.assignment_id, a.course_id, c.course_code, c.course_name, a.title, a.description, "
                    + "a.due_at, a.cutoff_at, a.created_at, "
                    + "%s AS week_no, a.allow_late, a.max_marks, a.allowed_types, a.max_file_mb, a.max_files, "
                    + "s.status, s.submitted_at, s.marks, s.feedback, s.comment "
                    + "FROM assignments a JOIN courses c ON c.course_id = a.course_id "
                    + "JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = ? AND e.status = 'ENROLLED' "
                    + "LEFT JOIN submissions s ON s.assignment_id = a.assignment_id AND s.student_id = e.student_id ";

    public List<StudentAssignment> findForStudent(int studentId, String courseCode) throws DatabaseException {
        String sql = assignmentSelect() + (courseCode == null
                ? "ORDER BY a.due_at, a.assignment_id"
                : "WHERE c.course_code = ? ORDER BY a.due_at, a.assignment_id");
        return courseCode == null
                ? StudentDaoSupport.query(sql, StudentAssignmentDAO::mapAssignment, studentId)
                : StudentDaoSupport.query(sql, StudentAssignmentDAO::mapAssignment, studentId, courseCode);
    }

    public StudentAssignment findForStudent(int studentId, long assignmentId) throws DatabaseException {
        return StudentDaoSupport.queryOne(assignmentSelect()
                + "WHERE a.assignment_id = ?", StudentAssignmentDAO::mapAssignment, studentId, assignmentId);
    }

    private static String assignmentSelect() throws DatabaseException {
        return ASSIGNMENT_SELECT.formatted(hasWeekNumber() ? "a.week_no" : "WEEK(a.created_at, 3)");
    }

    private static boolean hasWeekNumber() throws DatabaseException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, "assignments", null)) {
                while (columns.next()) {
                    if ("week_no".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Assignment schema could not be inspected.", ex);
        }
    }

    public StudentSubmissionDetails findSubmission(int studentId, long assignmentId) throws DatabaseException {
        StudentSubmissionDetails details = StudentDaoSupport.queryOne(
                "SELECT submission_id, assignment_id, submitted_at, status, comment, marks, feedback "
                        + "FROM submissions WHERE student_id = ? AND assignment_id = ?",
                rs -> new StudentSubmissionDetails(rs.getLong("submission_id"), rs.getLong("assignment_id"),
                        localDateTime(rs.getTimestamp("submitted_at")), rs.getString("status"),
                        rs.getString("comment"), nullableDouble(rs, "marks"), rs.getString("feedback"), List.of()),
                studentId, assignmentId);
        if (details == null) return null;
        List<StudentSubmissionDetails.SubmittedFile> files = StudentDaoSupport.query(
                "SELECT original_name, stored_path, file_size FROM submission_files "
                        + "WHERE submission_id = ? ORDER BY file_id",
                rs -> new StudentSubmissionDetails.SubmittedFile(rs.getString("original_name"),
                        rs.getString("stored_path"), rs.getLong("file_size")), details.id());
        return new StudentSubmissionDetails(details.id(), details.assignmentId(), details.submittedAt(),
                details.status(), details.comment(), details.marks(), details.feedback(), files);
    }

    public void saveSubmission(int studentId, long assignmentId, String comment, String status,
                               List<StudentSubmissionDetails.SubmittedFile> files) throws DatabaseException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long submissionId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO submissions (assignment_id, student_id, comment, submitted_at, status, marks, feedback) "
                                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, NULL, NULL) "
                                + "ON DUPLICATE KEY UPDATE submission_id = LAST_INSERT_ID(submission_id), "
                                + "comment = VALUES(comment), submitted_at = CURRENT_TIMESTAMP, status = VALUES(status), "
                                + "marks = NULL, feedback = NULL",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, assignmentId);
                    statement.setInt(2, studentId);
                    statement.setString(3, comment);
                    statement.setString(4, status);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Submission ID was not returned.");
                        submissionId = keys.getLong(1);
                    }
                }

                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM submission_files WHERE submission_id = ?")) {
                    delete.setLong(1, submissionId);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO submission_files (submission_id, original_name, stored_path, file_size) "
                                + "VALUES (?, ?, ?, ?)")) {
                    for (StudentSubmissionDetails.SubmittedFile file : files) {
                        insert.setLong(1, submissionId);
                        insert.setString(2, file.name());
                        insert.setString(3, file.storedPath());
                        insert.setLong(4, file.size());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("The submission could not be saved.", ex);
        }
    }

    private static StudentAssignment mapAssignment(ResultSet rs) throws SQLException {
        String types = rs.getString("allowed_types");
        List<String> allowedTypes = types == null || types.isBlank() ? List.of()
                : Arrays.stream(types.split(",")).map(String::trim)
                .filter(s -> !s.isBlank()).map(s -> s.toLowerCase(Locale.ROOT)).distinct().toList();
        return new StudentAssignment(rs.getLong("assignment_id"), rs.getInt("course_id"),
                rs.getString("course_code"), rs.getString("course_name"), rs.getString("title"),
                rs.getString("description"), localDateTime(rs.getTimestamp("due_at")),
                localDateTime(rs.getTimestamp("cutoff_at")), rs.getBoolean("allow_late"),
                nullableDouble(rs, "max_marks"), allowedTypes, rs.getInt("max_file_mb"),
                rs.getInt("max_files"), rs.getString("status"),
                localDateTime(rs.getTimestamp("submitted_at")), nullableDouble(rs, "marks"),
                rs.getString("feedback"), rs.getString("comment"),
                rs.getObject("week_no") instanceof Number week ? week.intValue() : null,
                localDateTime(rs.getTimestamp("created_at")));
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
