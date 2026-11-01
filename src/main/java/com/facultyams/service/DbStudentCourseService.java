package com.facultyams.service;

import com.facultyams.model.Activity;
import com.facultyams.model.CourseOffering;
import com.facultyams.model.CourseSummary;
import com.facultyams.model.Section;
import com.facultyams.security.StudentAccess;

import com.facultyams.model.CourseAttendance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Enrolled courses of the logged-in student, read from the {@code enrollments} table. */
public class DbStudentCourseService implements StudentCourseService {
    private final EnrollmentService enrollmentService = new EnrollmentService();

    @Override
    public List<CourseSummary> findCourses(String studentId) {
        int me = StudentAccess.requireOwn(studentId);
        List<CourseSummary> result = new ArrayList<>();
        // "Progress" = share of the course's sessions that have been held and recorded so far
        Map<String, Integer> progress = new HashMap<>();
        for (CourseAttendance a : new DbStudentAttendanceService().summary(String.valueOf(me))) {
            progress.put(a.courseCode(), a.totalSessions() <= 0 ? 0 : Math.min(100, a.recorded() * 100 / a.totalSessions()));
        }
        for (CourseOffering c : enrollmentService.getEnrolled()) {
            result.add(new CourseSummary(c.getCode(), c.getCode(), c.getTitle(), c.getSemester(), c.getCode(),
                    progress.getOrDefault(c.getCode(), 0), null, c.getCredits(), c.getLecturerName()));
        }
        return result;
    }

    @Override
    public List<Section> sections(String courseId) {
        StudentAccess.currentStudentId();
        return List.of();
    }

    @Override
    public List<Activity> activities(String sectionId) {
        StudentAccess.currentStudentId();
        return List.of();
    }
}
