package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;
import com.facultyams.model.student.StudentMaterial;

import java.util.List;

/** Course materials, always joined to the student's ENROLLED courses so other courses are never returned. */
public class StudentMaterialDAO {

    private static final String SELECT =
            "SELECT cm.materialID, c.course_id, c.course_code, c.course_name, lu.full_name AS lecturer, "
                    + "cm.description, cm.notes, cm.referenceText "
                    + "FROM course_materials cm JOIN courses c ON c.course_id = cm.courseID "
                    + "JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = ? AND e.status = 'ENROLLED' "
                    + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id ";

    private static final StudentDaoSupport.RowMapper<StudentMaterial> MAPPER = rs -> new StudentMaterial(
            rs.getInt("materialID"), rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_name"),
            rs.getString("lecturer"), rs.getString("description"), rs.getString("notes"), rs.getString("referenceText"));

    public List<StudentMaterial> findForEnrolledCourses(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(SELECT + "ORDER BY c.course_code, cm.materialID", MAPPER, studentId);
    }

    /** One material, only when the student is enrolled in its course. */
    public StudentMaterial findOne(int studentId, int materialId) throws DatabaseException {
        return StudentDaoSupport.queryOne(SELECT + "WHERE cm.materialID = ?", MAPPER, studentId, materialId);
    }
}
