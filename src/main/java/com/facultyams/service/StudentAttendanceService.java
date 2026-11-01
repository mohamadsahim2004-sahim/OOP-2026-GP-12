package com.facultyams.service;

import com.facultyams.model.CourseAttendance;
import com.facultyams.model.SessionAttendance;

import java.time.LocalDate;
import java.util.List;

/** Read access to the logged-in student's attendance. The student id argument must be the student's own. */
public interface StudentAttendanceService {
    List<CourseAttendance> summary(String studentId);
    List<SessionAttendance> sessionsBetween(String studentId, LocalDate from, LocalDate to);
    List<SessionAttendance> sessionsForCourse(String studentId, String courseCode);
    void addAttendanceListener(AttendanceListener listener);
    /** Tells listeners that attendance-related data (for example a medical request) changed. */
    void notifyChanged();

    @FunctionalInterface
    interface AttendanceListener {
        void onAttendanceUpdated();
    }
}
