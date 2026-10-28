package com.facultyams.service;

import com.facultyams.dao.StudentAttendanceDAO;
import com.facultyams.dao.StudentAttendanceDAO.CourseRow;
import com.facultyams.dao.StudentAttendanceDAO.SessionRow;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.ClassSession;
import com.facultyams.model.CourseAttendance;
import com.facultyams.model.SessionAttendance;
import com.facultyams.model.SessionStatus;
import com.facultyams.security.StudentAccess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Attendance from the real {@code session_attendance} rows. Percentage follows the project rule in
 * AttendanceService: 100 x (present + approved medical) / (15 sessions x components that have records).
 */
public class DbStudentAttendanceService implements StudentAttendanceService {
    public static final int SESSIONS_PER_COMPONENT = AttendanceService.TOTAL_SESSIONS;

    private final StudentAttendanceDAO dao = new StudentAttendanceDAO();
    private final List<AttendanceListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public List<CourseAttendance> summary(String studentId) {
        int me = StudentAccess.requireOwn(studentId);
        try {
            return dao.courseSummaries(me).stream().map(DbStudentAttendanceService::toSummary).toList();
        } catch (DatabaseException e) {
            throw new StudentServiceException("Attendance could not be loaded. Please try again.", e);
        }
    }

    @Override
    public List<SessionAttendance> sessionsBetween(String studentId, LocalDate from, LocalDate to) {
        int me = StudentAccess.requireOwn(studentId);
        try {
            return dao.findSessions(me, from, to, null).stream().map(DbStudentAttendanceService::toSession).toList();
        } catch (DatabaseException e) {
            throw new StudentServiceException("Attendance could not be loaded. Please try again.", e);
        }
    }

    @Override
    public List<SessionAttendance> sessionsForCourse(String studentId, String courseCode) {
        int me = StudentAccess.requireOwn(studentId);
        try {
            return dao.findSessions(me, null, null, courseCode).stream().map(DbStudentAttendanceService::toSession).toList();
        } catch (DatabaseException e) {
            throw new StudentServiceException("Attendance could not be loaded. Please try again.", e);
        }
    }

    @Override
    public void addAttendanceListener(AttendanceListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void notifyChanged() {
        for (AttendanceListener l : listeners) {
            try {
                l.onAttendanceUpdated();
            } catch (RuntimeException ignored) {
                // one broken listener must not stop the others
            }
        }
    }

    private static CourseAttendance toSummary(CourseRow r) {
        int denominator = SESSIONS_PER_COMPONENT * Math.max(1, r.components());
        return new CourseAttendance(r.code(), r.title(), denominator, r.present(), r.approvedMedical(), r.recorded());
    }

    private static SessionAttendance toSession(SessionRow r) {
        SessionStatus status;
        if ("present".equalsIgnoreCase(r.status())) {
            status = SessionStatus.PRESENT;
        } else if (r.medical() == 2) {
            status = SessionStatus.MEDICAL_APPROVED;
        } else if (r.medical() == 1) {
            status = SessionStatus.MEDICAL_PENDING;
        } else {
            status = SessionStatus.ABSENT;
        }
        // The database stores no clock time for a session; use the course's timetable slot for that weekday
        // when there is one, otherwise 00:00 (the UI hides times when start equals end).
        LocalTime start = r.start() == null ? LocalTime.MIDNIGHT : r.start();
        LocalTime end = r.end() == null ? LocalTime.MIDNIGHT : r.end();
        String type = "PRACTICAL".equalsIgnoreCase(r.componentType()) ? "Practical" : "Lecture";
        return new SessionAttendance(new ClassSession(String.valueOf(r.attendanceId()), r.code(), r.title(),
                r.date(), start, end, type, r.lecturer() == null ? "" : r.lecturer()), status);
    }
}
