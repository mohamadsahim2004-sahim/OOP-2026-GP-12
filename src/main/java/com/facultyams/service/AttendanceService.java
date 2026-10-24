package com.facultyams.service;

import java.time.LocalDate;
import java.util.List;

import com.facultyams.model.CourseAttendance;
import com.facultyams.model.SessionAttendance;

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
