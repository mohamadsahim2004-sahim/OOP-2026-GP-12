package com.srms.service;

import com.srms.model.CourseAttendance;
import com.srms.model.SessionAttendance;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for querying attendance and class sessions.
 */
public interface AttendanceService {
    List<CourseAttendance> summary(String studentId);
    List<SessionAttendance> sessionsBetween(String studentId, LocalDate from, LocalDate to);
    List<SessionAttendance> sessionsForCourse(String studentId, String courseCode);
    void markSessionsMedicalApproved(List<String> sessionIds);
    void addAttendanceListener(AttendanceListener listener);

    @FunctionalInterface
    interface AttendanceListener {
        void onAttendanceUpdated();
    }
}
