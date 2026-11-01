package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.student.StudentMaterial;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** Course materials, always joined to the student's ENROLLED courses so other courses are never returned. */
public class StudentMaterialDAO {

    private static final String SELECT =
            "SELECT cm.materialID, c.course_id, c.course_code, c.course_name, lu.full_name AS lecturer, "
                    + "cm.description, cm.notes, cm.referenceText, %s AS week_no, %s AS uploaded_at "
                    + "FROM course_materials cm JOIN courses c ON c.course_id = cm.courseID "
                    + "JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = ? AND e.status = 'ENROLLED' "
                    + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id ";

    private static final StudentDaoSupport.RowMapper<StudentMaterial> MAPPER = rs -> new StudentMaterial(
            rs.getInt("materialID"), rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_name"),
            rs.getString("lecturer"), rs.getString("description"), rs.getString("notes"),
            rs.getString("referenceText"), weekNumber(rs),
            rs.getTimestamp("uploaded_at") == null ? null : rs.getTimestamp("uploaded_at").toLocalDateTime());

    public List<StudentMaterial> findForEnrolledCourses(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(select() + "ORDER BY c.course_code, cm.materialID", MAPPER, studentId);
    }

    /** A file the lecturer uploaded with a material. */
    public record StoredFile(String fileName, byte[] data) { }

    /** The uploaded file of a material, only when the student is enrolled in its course (otherwise null). */
    public StoredFile findFile(int studentId, int materialId) throws DatabaseException {
        return StudentDaoSupport.queryOne(
                "SELECT f.file_name, f.file_data FROM course_material_files f "
                        + "JOIN course_materials cm ON cm.materialID = f.materialID "
                        + "JOIN enrollments e ON e.course_id = cm.courseID AND e.student_id = ? AND e.status = 'ENROLLED' "
                        + "WHERE f.materialID = ?",
                rs -> new StoredFile(rs.getString(1), rs.getBytes(2)), studentId, materialId);
    }

    /** One material, only when the student is enrolled in its course. */
    public StudentMaterial findOne(int studentId, int materialId) throws DatabaseException {
        return StudentDaoSupport.queryOne(select() + "WHERE cm.materialID = ?", MAPPER, studentId, materialId);
    }

    private static String select() throws DatabaseException {
        return SELECT.formatted(hasColumn("week_no") ? "cm.week_no" : "NULL",
                hasColumn("uploaded_at") ? "cm.uploaded_at" : "NULL");
    }

    private static boolean hasColumn(String columnName) throws DatabaseException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null,
                    "course_materials", null)) {
                while (columns.next()) {
                    if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Course material schema could not be inspected.", ex);
        }
    }

    private static Integer weekNumber(java.sql.ResultSet rs) throws java.sql.SQLException {
        Object value = rs.getObject("week_no");
        return value instanceof Number number ? number.intValue() : null;
    }
}
