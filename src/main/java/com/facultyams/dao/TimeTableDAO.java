package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.TimeTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for timetable slots (table {@code timetables}, created by
 * database/migrations/001_notices_timetables.sql). The department and semester of a slot are those of
 * its course, so every read joins {@code courses} and {@code departments}.
 * SQL only - rules and permissions live in TimeTableService. Every statement is a PreparedStatement.
 */
public class TimeTableDAO {

    private static final String SELECT =
            "SELECT t.timetable_id, t.course_id, t.day_of_week, t.start_time, t.end_time, t.venue, "
                    + "c.course_code, c.course_name, c.academic_level, c.semester, c.department_id, d.department_name "
                    + "FROM timetables t "
                    + "JOIN courses c ON c.course_id = t.course_id "
                    + "JOIN departments d ON d.department_id = c.department_id ";

    /** Monday first, then by start time (same ordering idea as ReportDAO.timetableLoad). */
    private static final String ORDER =
            "ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), "
                    + "t.start_time, c.course_code";

    /** SQL state MySQL reports for "table doesn't exist". */
    private static final String SQL_STATE_NO_TABLE = "42S02";

    /** Slots of a department and/or semester (either may be null = no filter). */
    public List<TimeTable> findAll(Integer departmentId, Integer semester) throws DatabaseException {
        return search(null, departmentId, semester);
    }

    /**
     * Filtered list. The keyword (may be null/blank) matches course code, course name, venue or weekday;
     * department and semester (may be null) narrow it to one department / semester of the course.
     */
    public List<TimeTable> search(String keyword, Integer departmentId, Integer semester) throws DatabaseException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (c.course_code LIKE ? ESCAPE '!' OR c.course_name LIKE ? ESCAPE '!' "
                    + "OR t.venue LIKE ? ESCAPE '!' OR t.day_of_week LIKE ? ESCAPE '!') ");
            String like = likePattern(keyword);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (departmentId != null) {
            sql.append("AND c.department_id = ? ");
            params.add(departmentId);
        }
        if (semester != null) {
            sql.append("AND c.semester = ? ");
            params.add(semester);
        }
        sql.append(ORDER);
        return findMany(sql.toString(), params.toArray());
    }

    public TimeTable findById(int timetableId) throws DatabaseException {
        List<TimeTable> list = findMany(SELECT + "WHERE t.timetable_id = ?", timetableId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Slots that would clash with a slot on the same day between start and end: they overlap in time
     * AND either use the same venue or belong to the same course. Back-to-back slots (one ends exactly
     * when the next starts) do not overlap. Pass 0 as excludeTimetableId when adding a new slot.
     */
    public List<TimeTable> findConflicts(DayOfWeek day, LocalTime start, LocalTime end, String venue,
                                         int courseId, int excludeTimetableId) throws DatabaseException {
        return findMany(SELECT + "WHERE t.day_of_week = ? AND t.start_time < ? AND t.end_time > ? "
                        + "AND (t.venue = ? OR t.course_id = ?) AND t.timetable_id <> ? "
                        + "ORDER BY t.start_time",
                day.name(), end, start, venue, courseId, excludeTimetableId);
    }

    /** Inserts the slot, stores the generated id in it and returns that id. */
    public int insert(TimeTable slot) throws DatabaseException {
        String sql = "INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, slot);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    slot.setTimetableId(keys.getInt(1));
                }
            }
            return slot.getTimetableId();
        } catch (SQLException e) {
            throw writeFailure("add", e);
        }
    }

    public void update(TimeTable slot) throws DatabaseException {
        String sql = "UPDATE timetables SET course_id = ?, day_of_week = ?, start_time = ?, end_time = ?, "
                + "venue = ? WHERE timetable_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, slot);
            ps.setInt(6, slot.getTimetableId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw writeFailure("update", e);
        }
    }

    public void delete(int timetableId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM timetables WHERE timetable_id = ?")) {
            ps.setInt(1, timetableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("Failed to delete timetable slot: " + e.getMessage(), e);
        }
    }

    // ----- helpers -----

    private static void fill(PreparedStatement ps, TimeTable slot) throws SQLException {
        ps.setInt(1, slot.getCourseId());
        ps.setString(2, slot.getDayOfWeek().name());
        ps.setObject(3, slot.getStartTime());
        ps.setObject(4, slot.getEndTime());
        ps.setString(5, slot.getVenue());
    }

    /** "%keyword%" with LIKE wildcards in the keyword escaped, so typing "50%" or "_" searches for those characters. */
    private static String likePattern(String keyword) {
        String escaped = keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static DatabaseException writeFailure(String action, SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException) {
            // the only foreign key on this table is course_id
            return new DatabaseException("The timetable slot could not be saved because the selected course "
                    + "no longer exists.", e);
        }
        return failure("Failed to " + action + " timetable slot: " + e.getMessage(), e);
    }

    /** A DatabaseException that tells the user to run the migration when the timetables table is missing. */
    private static DatabaseException failure(String message, SQLException e) {
        if (SQL_STATE_NO_TABLE.equals(e.getSQLState())) {
            return new DatabaseException("The timetables table is missing. Run "
                    + "database/migrations/001_notices_timetables.sql first. (" + e.getMessage() + ")", e);
        }
        return new DatabaseException(message, e);
    }

    private List<TimeTable> findMany(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TimeTable> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw failure("Failed to read the timetable.", e);
        }
    }

    private static TimeTable mapRow(ResultSet rs) throws SQLException {
        TimeTable slot = new TimeTable(
                rs.getInt("timetable_id"),
                rs.getInt("course_id"),
                DayOfWeek.valueOf(rs.getString("day_of_week").toUpperCase()),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class),
                rs.getString("venue"));
        slot.setCourseCode(rs.getString("course_code"));
        slot.setCourseName(rs.getString("course_name"));
        slot.setAcademicLevel(rs.getInt("academic_level"));
        slot.setSemester(rs.getInt("semester"));
        slot.setDepartmentId(rs.getInt("department_id"));
        slot.setDepartmentName(rs.getString("department_name"));
        return slot;
    }
}