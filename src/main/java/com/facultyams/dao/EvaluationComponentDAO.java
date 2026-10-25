package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.EvaluationComponent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Evaluation components of a course and the marks entered against them. */
public class EvaluationComponentDAO {

    private static final String SELECT =
            "SELECT eval_component_id, course_id, component_name, category, max_marks, weightage " +
                    "FROM evaluation_component ";

    static final String SQL_BY_COURSE = SELECT + "WHERE course_id = ? ORDER BY eval_component_id";
    static final String SQL_BY_ID = SELECT + "WHERE eval_component_id = ?";

    static final String SQL_NAME_EXISTS =
            "SELECT COUNT(*) FROM evaluation_component " +
                    "WHERE course_id = ? AND LOWER(component_name) = LOWER(?) AND eval_component_id <> ?";

    static final String SQL_INSERT =
            "INSERT INTO evaluation_component (course_id, component_name, category, max_marks, weightage) " +
                    "VALUES (?, ?, ?, ?, ?)";

    static final String SQL_UPDATE =
            "UPDATE evaluation_component SET component_name = ?, category = ?, max_marks = ?, weightage = ? " +
                    "WHERE eval_component_id = ?";

    static final String SQL_DELETE = "DELETE FROM evaluation_component WHERE eval_component_id = ?";

    static final String SQL_COUNT_MARKS_FOR_COMPONENT =
            "SELECT COUNT(*) FROM component_marks WHERE eval_component_id = ?";

    static final String SQL_COUNT_MARKS_FOR_COURSE =
            "SELECT COUNT(*) FROM component_marks cm " +
                    "JOIN evaluation_component ec ON ec.eval_component_id = cm.eval_component_id " +
                    "WHERE ec.course_id = ?";

    static final String SQL_COURSE_MARKS =
            "SELECT cm.student_id, cm.eval_component_id, cm.marks_obtained FROM component_marks cm " +
                    "JOIN evaluation_component ec ON ec.eval_component_id = cm.eval_component_id " +
                    "WHERE ec.course_id = ?";

    static final String SQL_STUDENT_MARKS = SQL_COURSE_MARKS + " AND cm.student_id = ?";

    private static EvaluationComponent map(ResultSet rs) throws SQLException {
        return new EvaluationComponent(rs.getInt(1), rs.getInt(2), rs.getString(3),
                rs.getString(4), rs.getDouble(5), rs.getDouble(6));
    }

    public List<EvaluationComponent> findByCourse(int courseId) throws DatabaseException {
        return JdbcSupport.list(SQL_BY_COURSE, EvaluationComponentDAO::map, courseId);
    }

    /** @return the component, or null when no component has this id */
    public EvaluationComponent findById(int componentId) throws DatabaseException {
        List<EvaluationComponent> rows = JdbcSupport.list(SQL_BY_ID, EvaluationComponentDAO::map, componentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** True when another component of the course already has this name (case-insensitive). */
    public boolean nameExists(int courseId, String name, int excludeComponentId) throws DatabaseException {
        return JdbcSupport.count(SQL_NAME_EXISTS, courseId, name, excludeComponentId) > 0;
    }

    public int insert(EvaluationComponent c) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            JdbcSupport.bind(ps, c.getCourseId(), c.getComponentName(), c.getCategory(),
                    c.getMaxMarks(), c.getWeightage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw JdbcSupport.wrap(e);
        }
    }

    public void update(EvaluationComponent c) throws DatabaseException {
        JdbcSupport.update(SQL_UPDATE, c.getComponentName(), c.getCategory(),
                c.getMaxMarks(), c.getWeightage(), c.getEvalComponentId());
    }

    public void delete(int componentId) throws DatabaseException {
        JdbcSupport.update(SQL_DELETE, componentId);
    }

    public int countMarksForComponent(int componentId) throws DatabaseException {
        return JdbcSupport.count(SQL_COUNT_MARKS_FOR_COMPONENT, componentId);
    }

    public int countMarksForCourse(int courseId) throws DatabaseException {
        return JdbcSupport.count(SQL_COUNT_MARKS_FOR_COURSE, courseId);
    }

    /** studentId -> (componentId -> marks obtained) for every mark entered in the course. */
    public Map<Integer, Map<Integer, Double>> findMarksByCourse(int courseId) throws DatabaseException {
        Map<Integer, Map<Integer, Double>> result = new HashMap<>();
        JdbcSupport.list(SQL_COURSE_MARKS, rs -> {
            result.computeIfAbsent(rs.getInt(1), k -> new LinkedHashMap<>()).put(rs.getInt(2), rs.getDouble(3));
            return null;
        }, courseId);
        return result;
    }

    /** componentId -> marks obtained, for one student in one course. */
    public Map<Integer, Double> findMarksOfStudent(int courseId, int studentId) throws DatabaseException {
        Map<Integer, Double> result = new LinkedHashMap<>();
        JdbcSupport.list(SQL_STUDENT_MARKS, rs -> {
            result.put(rs.getInt(2), rs.getDouble(3));
            return null;
        }, courseId, studentId);
        return result;
    }
}
