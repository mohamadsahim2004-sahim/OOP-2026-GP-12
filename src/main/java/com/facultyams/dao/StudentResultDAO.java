package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;

import java.util.List;

/**
 * A student's results: the latest {@code marks} row per course (the table has no unique key, so the highest
 * marksid wins) for courses the student is enrolled in or has marks for. Grade and grade points are the values
 * the lecturer module stored with MarksService (overall = 40% CA + 60% final).
 */
public class StudentResultDAO {

    /** One course line; the mark columns are null when no marks have been entered yet. */
    public record ResultRow(int courseId, String code, String name, int credits, int level, int semester,
                            String lecturer, boolean enrolled,
                            Double caMarks, Double finalMarks, Double overall, String grade, Double gradePoints,
                            Boolean eligible) {
        public boolean hasMarks() {
            return overall != null && grade != null;
        }
    }

    public List<ResultRow> findResults(int studentId) throws DatabaseException {
        String sql = "SELECT c.course_id, c.course_code, c.course_name, c.credits, c.academic_level, c.semester, "
                + "lu.full_name AS lecturer, (e.status = 'ENROLLED') AS enrolled, "
                + "m.obtainMarks, m.finalMarks, m.overall_mark, m.grade, m.grade_points, m.isEligible "
                + "FROM courses c "
                + "LEFT JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = ? "
                + "LEFT JOIN marks m ON m.student_id = ? AND m.course_id = c.course_id AND m.marksid = "
                + "(SELECT MAX(m2.marksid) FROM marks m2 WHERE m2.student_id = ? AND m2.course_id = c.course_id) "
                + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id "
                + "WHERE m.marksid IS NOT NULL OR e.status = 'ENROLLED' "
                + "ORDER BY c.academic_level, c.semester, c.course_code";
        return StudentDaoSupport.query(sql, rs -> new ResultRow(
                rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_name"),
                rs.getInt("credits"), rs.getInt("academic_level"), rs.getInt("semester"),
                rs.getString("lecturer"), rs.getBoolean("enrolled"),
                nullableDouble(rs, "obtainMarks"), nullableDouble(rs, "finalMarks"), nullableDouble(rs, "overall_mark"),
                rs.getString("grade"), nullableDouble(rs, "grade_points"),
                rs.getObject("isEligible") == null ? null : rs.getBoolean("isEligible")),
                studentId, studentId, studentId);
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
