package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.ComponentInfo;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.StudentInfo;
import com.facultyams.util.SqlFilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-mostly lookups (courses, components, batches, students) used by the Technical Officer screens. */
public class TechnicalLookupDAO {

    public List<CourseInfo> courses() throws DatabaseException {
        String sql = "SELECT course_id, course_code, course_name, course_type FROM courses ORDER BY course_code";
        List<CourseInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CourseInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the courses.", e);
        }
        return list;
    }

    public List<String> batches() throws DatabaseException {
        List<String> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT batch FROM students ORDER BY batch");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the batches.", e);
        }
        return list;
    }

    public List<ComponentInfo> componentsOf(int courseId) throws DatabaseException {
        List<ComponentInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection()) {
            readComponents(con, courseId, list);
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the course components.", e);
        }
        return list;
    }

    /**
     * Returns the components (THEORY / PRACTICAL) of a course, first creating the ones its course type
     * requires but that do not exist yet, so a course can be marked without manual set-up.
     */
    public List<ComponentInfo> ensureComponents(int courseId) throws DatabaseException {
        List<ComponentInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection()) {
            String courseType = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT course_type FROM courses WHERE course_id = ?")) {
                ps.setInt(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        courseType = rs.getString(1);
                    }
                }
            }
            if (courseType == null) {
                throw new DatabaseException("Course " + courseId + " does not exist.");
            }
            String insert = "INSERT INTO course_component (course_id, type) SELECT ?, ? FROM DUAL WHERE NOT EXISTS "
                    + "(SELECT 1 FROM course_component WHERE course_id = ? AND type = ?)";
            for (String type : new String[] {"THEORY", "PRACTICAL"}) {
                if (courseType.equals("THEORY_AND_PRACTICAL") || courseType.equals(type)) {
                    try (PreparedStatement ps = con.prepareStatement(insert)) {
                        ps.setInt(1, courseId);
                        ps.setString(2, type);
                        ps.setInt(3, courseId);
                        ps.setString(4, type);
                        ps.executeUpdate();
                    }
                }
            }
            readComponents(con, courseId, list);
        } catch (SQLException e) {
            throw new DatabaseException("Could not prepare the course components.", e);
        }
        return list;
    }

    private void readComponents(Connection con, int courseId, List<ComponentInfo> into) throws SQLException {
        String sql = "SELECT component_id, course_id, type FROM course_component WHERE course_id = ? ORDER BY type DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    into.add(new ComponentInfo(rs.getInt(1), rs.getInt(2), rs.getString(3)));
                }
            }
        }
    }

    /** Active students of one batch (null or blank = every batch), ordered by registration number. */
    public List<StudentInfo> studentsInBatch(String batch) throws DatabaseException {
        SqlFilter f = new SqlFilter().and("u.is_active = TRUE").andIf(batch, "s.batch = ?");
        return queryStudents("SELECT s.user_id, u.full_name, s.registration_no, s.batch FROM students s "
                + "JOIN users u ON u.user_id = s.user_id" + f.where() + " ORDER BY s.registration_no", f, 0);
    }

    /** Active students whose name, registration number or username contains the text (at most {@code limit}). */
    public List<StudentInfo> searchStudents(String text, int limit) throws DatabaseException {
        SqlFilter f = new SqlFilter().and("u.is_active = TRUE")
                .containsAny(text, "u.full_name", "s.registration_no", "u.username");
        return queryStudents("SELECT s.user_id, u.full_name, s.registration_no, s.batch FROM students s "
                + "JOIN users u ON u.user_id = s.user_id" + f.where() + " ORDER BY s.registration_no LIMIT ?", f, limit);
    }

    private List<StudentInfo> queryStudents(String sql, SqlFilter f, int limit) throws DatabaseException {
        List<StudentInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int next = f.bind(ps, 1);
            if (limit > 0) {
                ps.setInt(next, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StudentInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the students.", e);
        }
        return list;
    }
}