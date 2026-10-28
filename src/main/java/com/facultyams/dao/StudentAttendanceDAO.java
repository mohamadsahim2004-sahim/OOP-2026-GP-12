package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Attendance read from {@code session_attendance} for the student's ENROLLED courses only. */
public class StudentAttendanceDAO {

    /** Per-course totals. {@code components} is the number of components (theory/practical) with records. */
    public record CourseRow(int courseId, String code, String title, int components, int recorded,
                            int present, int approvedMedical) { }

    /** One recorded session. {@code medical} is 0 none/rejected, 1 pending, 2 approved. */
    public record SessionRow(int attendanceId, String code, String title, String componentType, String lecturer,
                             LocalDate date, LocalTime start, LocalTime end, String status, int medical, int sessionNo) { }

    public List<CourseRow> courseSummaries(int studentId) throws DatabaseException {
        String sql = "SELECT c.course_id, c.course_code, c.course_name, "
                + "COUNT(DISTINCT CASE WHEN sa.attendance_id IS NOT NULL THEN cc.component_id END) AS comps, "
                + "COUNT(sa.attendance_id) AS recorded, "
                + "COALESCE(SUM(sa.status = 'present'), 0) AS present, "
                + "COALESCE(SUM(sa.status = 'absent' AND EXISTS (SELECT 1 FROM medical md "
                + "WHERE md.attendance_id = sa.attendance_id AND md.approval_status = 'approved')), 0) AS approved "
                + "FROM enrollments e JOIN courses c ON c.course_id = e.course_id "
                + "LEFT JOIN course_component cc ON cc.course_id = c.course_id "
                + "LEFT JOIN session_attendance sa ON sa.component_id = cc.component_id AND sa.user_id = e.student_id "
                + "WHERE e.student_id = ? AND e.status = 'ENROLLED' "
                + "GROUP BY c.course_id, c.course_code, c.course_name ORDER BY c.course_code";
        return StudentDaoSupport.query(sql, rs -> new CourseRow(rs.getInt("course_id"), rs.getString("course_code"),
                rs.getString("course_name"), rs.getInt("comps"), rs.getInt("recorded"), rs.getInt("present"),
                rs.getInt("approved")), studentId);
    }

    /** Sessions for the student, optionally limited by date range and/or course code (null = no limit). */
    public List<SessionRow> findSessions(int studentId, LocalDate from, LocalDate to, String courseCode)
            throws DatabaseException {
        StringBuilder sql = new StringBuilder(
                "SELECT sa.attendance_id, sa.session_no, sa.date, sa.status, cc.type, c.course_code, c.course_name, "
                        + "lu.full_name AS lecturer, "
                        + "(SELECT MIN(t.start_time) FROM timetables t WHERE t.course_id = c.course_id "
                        + "AND t.day_of_week = UPPER(DAYNAME(sa.date))) AS start_time, "
                        + "(SELECT MIN(t.end_time) FROM timetables t WHERE t.course_id = c.course_id "
                        + "AND t.day_of_week = UPPER(DAYNAME(sa.date))) AS end_time, "
                        + "COALESCE((SELECT MAX(CASE md.approval_status WHEN 'approved' THEN 2 WHEN 'pending' THEN 1 ELSE 0 END) "
                        + "FROM medical md WHERE md.attendance_id = sa.attendance_id), 0) AS medical "
                        + "FROM session_attendance sa JOIN course_component cc ON cc.component_id = sa.component_id "
                        + "JOIN courses c ON c.course_id = cc.course_id "
                        + "JOIN enrollments e ON e.course_id = c.course_id AND e.student_id = sa.user_id AND e.status = 'ENROLLED' "
                        + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id WHERE sa.user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (from != null && to != null) {
            sql.append("AND sa.date BETWEEN ? AND ? ");
            params.add(Date.valueOf(from));
            params.add(Date.valueOf(to));
        }
        if (courseCode != null) {
            sql.append("AND c.course_code = ? ");
            params.add(courseCode);
        }
        sql.append("ORDER BY sa.date, c.course_code, sa.session_no");
        return StudentDaoSupport.query(sql.toString(), rs -> {
            Time st = rs.getTime("start_time");
            Time en = rs.getTime("end_time");
            return new SessionRow(rs.getInt("attendance_id"), rs.getString("course_code"), rs.getString("course_name"),
                    rs.getString("type"), rs.getString("lecturer"), rs.getDate("date").toLocalDate(),
                    st == null ? null : st.toLocalTime(), en == null ? null : en.toLocalTime(),
                    rs.getString("status"), rs.getInt("medical"), rs.getInt("session_no"));
        }, params.toArray());
    }
}
