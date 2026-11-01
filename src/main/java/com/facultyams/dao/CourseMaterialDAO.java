package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.CourseMaterial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Course materials ({@code course_materials}); reads are always limited to one lecturer's courses. */
public class CourseMaterialDAO {

    static final String SQL_BY_LECTURER =
            "SELECT cm.materialID, cm.courseID, cm.description, cm.notes, cm.referenceText " +
                    "FROM course_materials cm JOIN courses c ON c.course_id = cm.courseID " +
                    "WHERE c.lecturer_id = ? ORDER BY c.course_code, cm.materialID";

    static final String SQL_COURSE_OF_MATERIAL =
            "SELECT courseID FROM course_materials WHERE materialID = ?";

    static final String SQL_INSERT =
            "INSERT INTO course_materials (courseID, description, notes, referenceText) VALUES (?, ?, ?, ?)";

    static final String SQL_UPDATE =
            "UPDATE course_materials SET courseID = ?, description = ?, notes = ?, referenceText = ? " +
                    "WHERE materialID = ?";

    static final String SQL_DELETE =
            "DELETE FROM course_materials WHERE materialID = ?";

    public List<CourseMaterial> findByLecturer(int lecturerId) throws DatabaseException {
        return JdbcSupport.list(SQL_BY_LECTURER, rs -> new CourseMaterial(
                rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5)), lecturerId);
    }

    /** @return the course the material belongs to, or -1 when there is no such material */
    public int findCourseId(int materialId) throws DatabaseException {
        List<Integer> rows = JdbcSupport.list(SQL_COURSE_OF_MATERIAL, rs -> rs.getInt(1), materialId);
        return rows.isEmpty() ? -1 : rows.get(0);
    }

    public boolean addMaterial(CourseMaterial m) throws DatabaseException {
        return JdbcSupport.update(SQL_INSERT, m.getCourseID(), m.getDescription(), m.getNotes(),
                m.getReferenceText()) > 0;
    }

    public boolean updateMaterial(CourseMaterial m) throws DatabaseException {
        return JdbcSupport.update(SQL_UPDATE, m.getCourseID(), m.getDescription(), m.getNotes(),
                m.getReferenceText(), m.getMaterialID()) > 0;
    }

    // ------------------------------------------------------------------ uploaded files (course_material_files)

    static final String SQL_SAVE_FILE =
            "INSERT INTO course_material_files (materialID, file_name, file_size, file_data) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), file_size = VALUES(file_size), " +
                    "file_data = VALUES(file_data), uploaded_at = CURRENT_TIMESTAMP";

    static final String SQL_FILE_NAMES_BY_LECTURER =
            "SELECT f.materialID, f.file_name, f.file_size FROM course_material_files f " +
                    "JOIN course_materials cm ON cm.materialID = f.materialID " +
                    "JOIN courses c ON c.course_id = cm.courseID WHERE c.lecturer_id = ?";

    /** Name and size of an uploaded file, without its bytes. */
    public record FileInfo(String fileName, long sizeBytes) {
    }

    /**
     * Inserts (new) or updates the material AND stores the file with it in ONE transaction, so a material never
     * exists half-saved. The file's bytes live in the database, so students on any computer can download it.
     */
    public void saveWithFile(CourseMaterial m, boolean isNew, String fileName, byte[] data) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean auto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int id = m.getMaterialID();
                if (isNew) {
                    try (PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                        JdbcSupport.bind(ps, m.getCourseID(), m.getDescription(), m.getNotes(), m.getReferenceText());
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("The new material has no id.");
                            }
                            id = keys.getInt(1);
                        }
                    }
                } else {
                    JdbcSupport.update(con, SQL_UPDATE, m.getCourseID(), m.getDescription(), m.getNotes(),
                            m.getReferenceText(), id);
                }
                try (PreparedStatement ps = con.prepareStatement(SQL_SAVE_FILE)) {
                    ps.setInt(1, id);
                    ps.setString(2, fileName);
                    ps.setLong(3, data.length);
                    ps.setBytes(4, data);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            if ("42S02".equals(e.getSQLState())) {
                throw new DatabaseException("File storage is not set up yet. Run "
                        + "database/migrations/011_course_material_files.sql once, then try again.", e);
            }
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("max_allowed_packet")) {
                throw new DatabaseException("The database refuses a file this large (max_allowed_packet). "
                        + "Choose a smaller file.", e);
            }
            throw JdbcSupport.wrap(e);
        }
    }

    /** Uploaded files of one lecturer's materials (material id to file); empty when file storage is not set up. */
    public Map<Integer, FileInfo> findFileInfoByLecturer(int lecturerId) {
        Map<Integer, FileInfo> map = new HashMap<>();
        try {
            for (Object[] row : JdbcSupport.list(SQL_FILE_NAMES_BY_LECTURER,
                    rs -> new Object[]{rs.getInt(1), rs.getString(2), rs.getLong(3)}, lecturerId)) {
                map.put((Integer) row[0], new FileInfo((String) row[1], (Long) row[2]));
            }
        } catch (DatabaseException e) {
            // table 011 not created yet: the list simply shows no attached files
        }
        return map;
    }

    public boolean deleteMaterial(int materialId) throws DatabaseException {
        return JdbcSupport.update(SQL_DELETE, materialId) > 0;
    }
}
