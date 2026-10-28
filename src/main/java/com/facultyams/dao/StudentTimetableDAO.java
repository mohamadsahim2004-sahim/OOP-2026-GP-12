package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;

import java.time.LocalTime;
import java.util.List;

/** Weekly timetable slots of the courses the student is currently ENROLLED in. */
public class StudentTimetableDAO {

    public record SlotRow(int timetableId, String code, String name, String lecturer, String courseType,
                          String day, LocalTime start, LocalTime end, String venue) { }

    public List<SlotRow> findForStudent(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(
                "SELECT t.timetable_id, c.course_code, c.course_name, lu.full_name, c.course_type, t.day_of_week, "
                        + "t.start_time, t.end_time, t.venue FROM enrollments e "
                        + "JOIN courses c ON c.course_id = e.course_id JOIN timetables t ON t.course_id = c.course_id "
                        + "LEFT JOIN users lu ON lu.user_id = c.lecturer_id "
                        + "WHERE e.student_id = ? AND e.status = 'ENROLLED' "
                        + "ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), t.start_time",
                rs -> new SlotRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getTime(7).toLocalTime(), rs.getTime(8).toLocalTime(), rs.getString(9)),
                studentId);
    }
}
