package com.facultyams.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

import com.facultyams.model.CourseAttendance;
import com.facultyams.model.SessionAttendance;
import com.facultyams.service.StudentAttendanceService;

/**
 * Controller for off-EDT attendance data loading.
 */
public class AttendanceController {
    private final StudentAttendanceService attendanceService;

    public AttendanceController(StudentAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    public void loadSummary(String studentId, Consumer<List<CourseAttendance>> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<List<CourseAttendance>, Void>() {
            @Override
            protected List<CourseAttendance> doInBackground() {
                return attendanceService.summary(studentId);
            }

            @Override
            protected void done() {
                try {
                    List<CourseAttendance> list = get();
                    if (onSuccess != null) onSuccess.accept(list);
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }

    public void loadSessionsBetween(String studentId, LocalDate from, LocalDate to,
                                    Consumer<List<SessionAttendance>> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<List<SessionAttendance>, Void>() {
            @Override
            protected List<SessionAttendance> doInBackground() {
                return attendanceService.sessionsBetween(studentId, from, to);
            }

            @Override
            protected void done() {
                try {
                    List<SessionAttendance> list = get();
                    if (onSuccess != null) onSuccess.accept(list);
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }
}
