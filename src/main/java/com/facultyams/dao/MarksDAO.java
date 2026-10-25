package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Marks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Component marks and calculated course results ({@code marks} table).
 * One result row per (student, course); saving never creates a duplicate.
 */
public class MarksDAO {

    /** A stored result with the student's identity (for a course result sheet). */
    public record ResultRow(int studentId, String registrationNo, String fullName,
                            double caPercent, double finalPercent, Double overall, String grade,
                            Double gradePoints, boolean eligible, boolean published) {
    }

    /** One course result of one student (for the student results / GPA screen). */
    public record StudentCourseResult(int courseId, String courseCode, String courseName, int credits,
                                      int level, int semester, double caPercent, double finalPercent,
                                      Double overall, String grade, Double gradePoints, boolean eligible,
                                      boolean published, boolean ownedByLecturer) {
    }

    static final String SQL_FIND_RESULT =
            "SELECT marksid, student_id, course_id, obtainMarks, finalMarks, grade, isEligible, " +
                    "overall_mark, grade_points, published FROM marks WHERE student_id = ? AND course_id = ?";

    static final String SQL_RESULTS_BY_COURSE =
            "SELECT m.student_id, s.registration_no, u.full_name, m.obtainMarks, m.finalMarks, " +
                    "m.overall_mark, m.grade, m.grade_points, m.isEligible, m.published " +
                    "FROM marks m JOIN students s ON s.user_id = m.student_id " +
                    "JOIN users u ON u.user_id = s.user_id " +
                    "WHERE m.course_id = ? ORDER BY s.registration_no";

    static final String SQL_PUBLISHED_COUNT =
            "SELECT COUNT(*) FROM marks WHERE student_id = ? AND course_id = ? AND published = 1";

    static final String SQL_RESULT_EXISTS =
            "SELECT COUNT(*) FROM marks WHERE student_id = ? AND course_id = ?";

    static final String SQL_UPDATE_RESULT =
            "UPDATE marks SET obtainMarks = ?, finalMarks = ?, grade = ?, isEligible = ?, " +
                    "overall_mark = ?, grade_points = ? WHERE student_id = ? AND course_id = ?";

    static final String SQL_INSERT_RESULT =
            "INSERT INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, " +
                    "overall_mark, grade_points, published) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";

    static final String SQL_COMPONENT_MARK_EXISTS =
            "SELECT COUNT(*) FROM component_marks WHERE student_id = ? AND eval_component_id = ?";

    static final String SQL_UPDATE_COMPONENT_MARK =
            "UPDATE component_marks SET marks_obtained = ?, entered_by = ? " +
                    "WHERE student_id = ? AND eval_component_id = ?";

    static final String SQL_INSERT_COMPONENT_MARK =
            "INSERT INTO component_marks (student_id, eval_component_id, marks_obtained, entered_by) " +
                    "VALUES (?, ?, ?, ?)";

    static final String SQL_PUBLISH =
            "UPDATE marks SET published = 1, published_at = CURRENT_TIMESTAMP " +
                    "WHERE course_id = ? AND published = 0 AND overall_mark IS NOT NULL";

    static final String SQL_UNPUBLISH =
            "UPDATE marks SET published = 0, published_at = NULL WHERE course_id = ? AND published = 1";

    static final String SQL_COUNT_PUBLISHED = "SELECT COUNT(*) FROM marks WHERE course_id = ? AND published = 1";

    static final String SQL_COUNT_RESULTS = "SELECT COUNT(*) FROM marks WHERE course_id = ?";

    static final String SQL_STUDENT_RESULTS =
            "SELECT c.course_id, c.course_code, c.course_name, c.credits, c.academic_level, c.semester, " +
                    "m.obtainMarks, m.finalMarks, m.overall_mark, m.grade, m.grade_points, m.isEligible, " +
                    "m.published, CASE WHEN c.lecturer_id = ? THEN 1 ELSE 0 END AS mine " +
                    "FROM marks m JOIN courses c ON c.course_id = m.course_id " +
                    "WHERE m.student_id = ? ORDER BY c.academic_level, c.semester, c.course_code";

    private static Double nullableDouble(ResultSet rs, int column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    /** @return the stored result, or null when the student has none for this course */
    public Marks findResult(int studentId, int courseId) throws DatabaseException {
        List<Marks> rows = JdbcSupport.list(SQL_FIND_RESULT, rs -> {
            Marks marks = new Marks(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                    rs.getDouble(4), rs.getDouble(5), rs.getString(6), rs.getBoolean(7));
            Double overall = nullableDouble(rs, 8);
            Double points = nullableDouble(rs, 9);
            marks.setOverallMark(overall == null ? 0 : overall);
            marks.setGradePoints(points == null ? 0 : points);
            marks.setPublished(rs.getBoolean(10));
            return marks;
        }, studentId, courseId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ResultRow> findResultsByCourse(int courseId) throws DatabaseException {
        return JdbcSupport.list(SQL_RESULTS_BY_COURSE, rs -> new ResultRow(
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getDouble(5),
                nullableDouble(rs, 6), rs.getString(7), nullableDouble(rs, 8),
                rs.getBoolean(9), rs.getBoolean(10)), courseId);
    }

    /**
     * Saves the entered component marks and (when all marks are present) the calculated result in
     * ONE transaction: either everything is stored or nothing is. A published result is locked.
     *
     * @param componentMarks componentId -> mark to insert or update (may be empty)
     * @param result         the calculated result to insert or update, or null if still incomplete
     */
    public void saveStudentMarks(int courseId, int studentId, int enteredBy,
                                 Map<Integer, Double> componentMarks, Marks result) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (JdbcSupport.count(con, SQL_PUBLISHED_COUNT, studentId, courseId) > 0) {
                    throw new DatabaseException(
                            "This result is already published and locked. Unpublish it before changing marks.");
                }
                for (Map.Entry<Integer, Double> entry : componentMarks.entrySet()) {
                    boolean exists = JdbcSupport.count(con, SQL_COMPONENT_MARK_EXISTS,
                            studentId, entry.getKey()) > 0;
                    if (exists) {
                        JdbcSupport.update(con, SQL_UPDATE_COMPONENT_MARK,
                                entry.getValue(), enteredBy, studentId, entry.getKey());
                    } else {
                        JdbcSupport.update(con, SQL_INSERT_COMPONENT_MARK,
                                studentId, entry.getKey(), entry.getValue(), enteredBy);
                    }
                }
                if (result != null) {
                    boolean exists = JdbcSupport.count(con, SQL_RESULT_EXISTS, studentId, courseId) > 0;
                    if (exists) {
                        JdbcSupport.update(con, SQL_UPDATE_RESULT, result.getObtainMarks(), result.getFinalMarks(),
                                result.getGrade(), result.isEligible(), result.getOverallMark(),
                                result.getGradePoints(), studentId, courseId);
                    } else {
                        JdbcSupport.update(con, SQL_INSERT_RESULT, studentId, courseId, result.getObtainMarks(),
                                result.getFinalMarks(), result.getGrade(), result.isEligible(),
                                result.getOverallMark(), result.getGradePoints());
                    }
                }
                con.commit();
            } catch (SQLException | DatabaseException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcSupport.wrap(e);
        }
    }

    /** Publishes every calculated, unpublished result of the course. @return rows published */
    public int publish(int courseId) throws DatabaseException {
        return JdbcSupport.update(SQL_PUBLISH, courseId);
    }

    /** Takes every published result of the course back to draft. @return rows unpublished */
    public int unpublish(int courseId) throws DatabaseException {
        return JdbcSupport.update(SQL_UNPUBLISH, courseId);
    }

    public int countPublished(int courseId) throws DatabaseException {
        return JdbcSupport.count(SQL_COUNT_PUBLISHED, courseId);
    }

    public int countResults(int courseId) throws DatabaseException {
        return JdbcSupport.count(SQL_COUNT_RESULTS, courseId);
    }

    /**
     * Every course result of one student; {@code ownedByLecturer} marks the lecturer's own courses
     * (only those may be shown in detail, but all published ones count towards GPA).
     */
    public List<StudentCourseResult> findStudentResults(int studentId, int lecturerId) throws DatabaseException {
        return JdbcSupport.list(SQL_STUDENT_RESULTS, rs -> new StudentCourseResult(
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),
                rs.getDouble(7), rs.getDouble(8), nullableDouble(rs, 9), rs.getString(10),
                nullableDouble(rs, 11), rs.getBoolean(12), rs.getBoolean(13), rs.getInt(14) == 1),
                lecturerId, studentId);
    }
}
