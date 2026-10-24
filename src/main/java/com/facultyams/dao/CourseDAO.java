
package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Database access for courses. */
public class CourseDAO {

    private static final String SELECT =
            "SELECT c.course_id, c.course_code, c.course_name, " +
                    "c.credits, c.academic_level, c.semester, " +
                    "c.course_type, c.department_id, c.lecturer_id, " +
                    "d.department_name, u.full_name AS lecturer_name " +
                    "FROM courses c " +
                    "JOIN departments d ON d.department_id = c.department_id " +
                    "LEFT JOIN users u ON u.user_id = c.lecturer_id ";

    private static final String DUPLICATE_MESSAGE =
            "Another course already uses this course code.";

    public List<Course> findAll() throws DatabaseException {
        return findMany(
                SELECT + "ORDER BY c.academic_level, c.semester, c.course_code");
    }

    public Course findById(int courseId) throws DatabaseException {
        List<Course> list =
                findMany(SELECT + "WHERE c.course_id = ?", courseId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Course findByCode(String courseCode) throws DatabaseException {
        List<Course> list =
                findMany(SELECT + "WHERE c.course_code = ?", courseCode);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Course> findByDepartment(int departmentId)
            throws DatabaseException {
        return findMany(
                SELECT + "WHERE c.department_id = ? ORDER BY c.course_code",
                departmentId);
    }

    public List<Course> findByLecturer(int lecturerId)
            throws DatabaseException {
        return findMany(
                SELECT + "WHERE c.lecturer_id = ? ORDER BY c.course_code",
                lecturerId);
    }

    public List<Course> search(
            String keyword,
            Integer departmentId,
            Integer academicLevel,
            Integer semester) throws DatabaseException {

        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (c.course_code LIKE ? OR c.course_name LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }

        if (departmentId != null) {
            sql.append("AND c.department_id = ? ");
            params.add(departmentId);
        }

        if (academicLevel != null) {
            sql.append("AND c.academic_level = ? ");
            params.add(academicLevel);
        }

        if (semester != null) {
            sql.append("AND c.semester = ? ");
            params.add(semester);
        }

        sql.append("ORDER BY c.academic_level, c.semester, c.course_code");
        return findMany(sql.toString(), params.toArray());
    }

    public boolean codeExists(String courseCode, int excludeCourseId)
            throws DatabaseException {

        String sql =
                "SELECT COUNT(*) FROM courses " +
                        "WHERE course_code = ? AND course_id <> ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, courseCode);
            ps.setInt(2, excludeCourseId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check course code.", e);
        }
    }

    public int countTimetableSlots(int courseId)
            throws DatabaseException {

        String sql =
                "SELECT COUNT(*) FROM timetables WHERE course_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, courseId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            if ("42S02".equals(e.getSQLState())) {
                return 0;
            }
            throw new DatabaseException(
                    "Could not check the timetable of this course.", e);
        }
    }

    public int countAll() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM courses";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count courses.", e);
        }
    }

    public int insert(Course course) throws DatabaseException {
        String sql =
                "INSERT INTO courses (course_code, course_name, credits, " +
                        "academic_level, semester, course_type, department_id, " +
                        "lecturer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            fill(ps, course);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    course.setCourseId(keys.getInt(1));
                }
            }

            return course.getCourseId();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(DUPLICATE_MESSAGE, e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add course: " +
                    e.getMessage(), e);
        }
    }

    public void update(Course course) throws DatabaseException {
        String sql =
                "UPDATE courses SET course_code = ?, course_name = ?, " +
                        "credits = ?, academic_level = ?, semester = ?, " +
                        "course_type = ?, department_id = ?, lecturer_id = ? " +
                        "WHERE course_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            fill(ps, course);
            ps.setInt(9, course.getCourseId());
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(DUPLICATE_MESSAGE, e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update course: " +
                    e.getMessage(), e);
        }
    }

    public void delete(int courseId) throws DatabaseException {
        String sql = "DELETE FROM courses WHERE course_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(
                    "This course cannot be deleted because other records " +
                            "(marks, attendance, enrollments, etc.) still use it.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete course: " +
                    e.getMessage(), e);
        }
    }

    /**
     * Returns course IDs and names for the Lecturer Dashboard dropdown.
     */
    public Map<Integer, String> getAllCourses()
            throws SQLException, DatabaseException {  String sql =
                "SELECT course_id, course_code, course_name " +
                        "FROM courses ORDER BY course_code";

        Map<Integer, String> courses = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int courseId = resultSet.getInt("course_id");

                String courseName =
                        resultSet.getString("course_code") + " - " +
                                resultSet.getString("course_name");

                courses.put(courseId, courseName);
            }
        }

        return courses;
    }

    private static void fill(PreparedStatement ps, Course course)
            throws SQLException {

        ps.setString(1, course.getCourseCode());
        ps.setString(2, course.getCourseName());
        ps.setInt(3, course.getCredits());
        ps.setInt(4, course.getAcademicLevel());
        ps.setInt(5, course.getSemester());
        ps.setString(6, course.getCourseType().name());
        ps.setInt(7, course.getDepartmentId());

        if (course.getLecturerId() == null) {
            ps.setNull(8, Types.INTEGER);
        } else {
            ps.setInt(8, course.getLecturerId());
        }
    }

    private List<Course> findMany(String sql, Object... params)
            throws DatabaseException {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Course> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(mapRow(rs));
                }

                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read courses.", e);
        }
    }

    private static Course mapRow(ResultSet rs) throws SQLException {
        Course course = new Course(
                rs.getInt("course_id"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getInt("credits"),
                rs.getInt("academic_level"),
                rs.getInt("semester"),
                Course.CourseType.valueOf(rs.getString("course_type")),
                rs.getInt("department_id"));

        int lecturerId = rs.getInt("lecturer_id");
        course.setLecturerId(rs.wasNull() ? null : lecturerId);
        course.setDepartmentName(rs.getString("department_name"));
        course.setLecturerName(rs.getString("lecturer_name"));

        return course;
    }
}
