package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;

/** Counts recently added materials/assignments and newly graded submissions for enrolled courses. */
public class StudentContentNotificationDAO {

    public int countRecent(int studentId) throws DatabaseException {
        Integer count = StudentDaoSupport.queryOne(
                "SELECT "
                        + "(SELECT COUNT(*) FROM course_materials cm JOIN courses c ON c.course_id = cm.courseID "
                        + "JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = ? AND e.status = 'ENROLLED' "
                        + "WHERE cm.uploaded_at >= CURRENT_TIMESTAMP - INTERVAL 7 DAY) + "
                        + "(SELECT COUNT(*) FROM assignments a JOIN enrollments e ON e.course_id = a.course_id "
                        + "AND e.student_id = ? AND e.status = 'ENROLLED' "
                        + "WHERE a.created_at >= CURRENT_TIMESTAMP - INTERVAL 7 DAY) + "
                        + "(SELECT COUNT(*) FROM submissions s JOIN assignments a ON a.assignment_id = s.assignment_id "
                        + "JOIN enrollments e ON e.course_id = a.course_id AND e.student_id = s.student_id "
                        + "AND e.status = 'ENROLLED' WHERE s.student_id = ? AND s.status = 'GRADED' "
                        + "AND s.graded_at >= CURRENT_TIMESTAMP - INTERVAL 7 DAY)",
                rs -> rs.getInt(1), studentId, studentId, studentId);
        return count == null ? 0 : count;
    }
}
