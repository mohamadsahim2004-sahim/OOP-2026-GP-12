package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.CourseOffering;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;

/**
 * Enrollment data for one student at a time. The offered courses are those of the student's own
 * department (courses.department_id = users.department_id). The UNIQUE (student_id, course_id) key stops
 * duplicate enrollment even if two requests race.
 */
public class EnrollmentDAO {

    private static final String OFFERING =
            "SELECT c.course_id, c.course_code, c.course_name, c.credits, c.academic_level, c.semester, "
                    + "c.course_type, d.department_name, lu.full_name AS lecturer, "
                    + "(e.status = 'ENROLLED') AS enrolled, "
                    + "(SELECT GROUP_CONCAT(CONCAT(UPPER(LEFT(t.day_of_week, 1)), LOWER(SUBSTRING(t.day_of_week, 2, 2)), ' ', "
                    + "TIME_FORMAT(t.start_time, '%H:%i'), '-', TIME_FORMAT(t.end_time, '%H:%i')) "
                    + "ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), t.start_time "
                    + "SEPARATOR ', ') FROM timetables t WHERE t.course_id = c.course_id) AS schedule "
                    + "FROM courses c JOIN users me ON me.user_id = ? AND me.department_id = c.department_id "
                    + "JOIN departments d ON d.department_id = c.department_id "
                    + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id "
                    + "LEFT JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = me.user_id ";

    private static StudentDaoSupport.RowMapper<CourseOffering> offeringMapper() {
        return rs -> {
            CourseOffering o = new CourseOffering();
            o.setCode(rs.getString("course_code"));
            o.setTitle(rs.getString("course_name"));
            o.setCredits(rs.getInt("credits"));
            o.setDepartment(rs.getString("department_name"));
            o.setSemester("Level " + rs.getInt("academic_level") + " Semester " + rs.getInt("semester"));
            String lecturer = rs.getString("lecturer");
            o.setLecturerName(lecturer == null ? "Not assigned" : lecturer);
            o.setCourseType(typeLabel(rs.getString("course_type")));
            o.setEnrolled(rs.getBoolean("enrolled"));
            String schedule = rs.getString("schedule");
            o.setScheduleTime(schedule == null ? "Not scheduled" : schedule);
            return o;
        };
    }

    static String typeLabel(String dbValue) {
        if (dbValue == null) return "";
        return switch (dbValue) {
            case "THEORY" -> "Theory";
            case "PRACTICAL" -> "Practical";
            case "THEORY_AND_PRACTICAL" -> "Theory & Practical";
            default -> dbValue.substring(0, 1) + dbValue.substring(1).toLowerCase(Locale.ROOT);
        };
    }

    /** Every course of the student's department, flagged with whether this student is enrolled. */
    public List<CourseOffering> findOfferedCourses(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(OFFERING + "ORDER BY c.academic_level, c.semester, c.course_code",
                offeringMapper(), studentId);
    }

    /** One course by code, only if it is offered to this student's department. */
    public CourseOffering findOffered(int studentId, String courseCode) throws DatabaseException {
        return StudentDaoSupport.queryOne(OFFERING + "WHERE c.course_code = ?", offeringMapper(), studentId, courseCode);
    }

    public Integer findOfferedCourseId(int studentId, String courseCode) throws DatabaseException {
        return StudentDaoSupport.queryOne(
                "SELECT c.course_id FROM courses c JOIN users me ON me.user_id = ? AND me.department_id = c.department_id "
                        + "WHERE c.course_code = ?", rs -> rs.getInt(1), studentId, courseCode);
    }

    public int enrolledCredits(int studentId) throws DatabaseException {
        Integer total = StudentDaoSupport.queryOne(
                "SELECT COALESCE(SUM(c.credits), 0) FROM enrollments e JOIN courses c ON c.course_id = e.course_id "
                        + "WHERE e.student_id = ? AND e.status = 'ENROLLED'", rs -> rs.getInt(1), studentId);
        return total == null ? 0 : total;
    }

    public boolean isActiveStudent(int studentId) throws DatabaseException {
        Integer n = StudentDaoSupport.queryOne(
                "SELECT COUNT(*) FROM users WHERE user_id = ? AND role = 'STUDENT' AND is_active = 1",
                rs -> rs.getInt(1), studentId);
        return n != null && n > 0;
    }

    /** True when the student already holds a passing result (grade points above zero and eligible). */
    public boolean hasPassed(int studentId, int courseId) throws DatabaseException {
        Integer n = StudentDaoSupport.queryOne(
                "SELECT COUNT(*) FROM marks WHERE student_id = ? AND course_id = ? AND grade_points > 0 AND isEligible = 1",
                rs -> rs.getInt(1), studentId, courseId);
        return n != null && n > 0;
    }

    /** True when marks or attendance already exist, so dropping would orphan graded work. */
    public boolean hasRecords(int studentId, int courseId) throws DatabaseException {
        Integer n = StudentDaoSupport.queryOne(
                "SELECT (SELECT COUNT(*) FROM marks WHERE student_id = ? AND course_id = ?) "
                        + "+ (SELECT COUNT(*) FROM session_attendance sa JOIN course_component cc ON cc.component_id = sa.component_id "
                        + "WHERE sa.user_id = ? AND cc.course_id = ?)",
                rs -> rs.getInt(1), studentId, courseId, studentId, courseId);
        return n != null && n > 0;
    }

    /** Weekly slots of the student's enrolled courses: {courseCode, DAY, start, end}. */
    public List<String[]> enrolledSlots(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(
                "SELECT c.course_code, t.day_of_week, t.start_time, t.end_time FROM enrollments e "
                        + "JOIN courses c ON c.course_id = e.course_id JOIN timetables t ON t.course_id = c.course_id "
                        + "WHERE e.student_id = ? AND e.status = 'ENROLLED'",
                rs -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)}, studentId);
    }

    public List<String[]> courseSlots(int courseId) throws DatabaseException {
        return StudentDaoSupport.query(
                "SELECT c.course_code, t.day_of_week, t.start_time, t.end_time FROM timetables t "
                        + "JOIN courses c ON c.course_id = t.course_id WHERE t.course_id = ?",
                rs -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)}, courseId);
    }

    /**
     * Enrolls the student. Returns false when an ENROLLED row already exists (duplicate). A DROPPED row is
     * re-activated, which keeps one row per student and course.
     */
    public boolean enroll(int studentId, int courseId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)")) {
                ps.setInt(1, studentId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
                return true;
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE enrollments SET status = 'ENROLLED', dropped_at = NULL, enrolled_at = CURRENT_TIMESTAMP "
                                + "WHERE student_id = ? AND course_id = ? AND status = 'DROPPED'")) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, courseId);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not enroll: " + e.getMessage(), e);
        }
    }

    /** Marks the enrollment DROPPED; false when the student was not enrolled. */
    public boolean drop(int studentId, int courseId) throws DatabaseException {
        return StudentDaoSupport.update(
                "UPDATE enrollments SET status = 'DROPPED', dropped_at = CURRENT_TIMESTAMP "
                        + "WHERE student_id = ? AND course_id = ? AND status = 'ENROLLED'", studentId, courseId) > 0;
    }
}
