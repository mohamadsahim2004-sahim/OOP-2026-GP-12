package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;
import com.facultyams.model.CourseMaterial;

import java.util.List;

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
    public boolean deleteMaterial(int materialId) throws DatabaseException {
        return JdbcSupport.update(SQL_DELETE, materialId) > 0;
    }
}
