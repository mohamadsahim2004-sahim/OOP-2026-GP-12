package com.facultyams.service;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.LecturerAccessDAO;
import com.facultyams.dao.LecturerAccessDAO.AttendanceAgg;
import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The gatekeeper of the Lecturer module. Every lecturer operation goes through it:
 * <ol>
 *   <li>the caller must be logged in with the LECTURER role (taken from the session, never from a
 *       value the screen supplies),</li>
 *   <li>a course may only be used if the lecturer is its assigned lecturer,</li>
 *   <li>a student may only be used if the student belongs to one of the lecturer's courses.</li>
 * </ol>
 * A course or student that does not exist and one that belongs to someone else give the same
 * answer, so ids cannot be probed.
 */
public class LecturerAccessService {

    private final LecturerAccessDAO dao;
    private final CourseDAO courseDAO;

    public LecturerAccessService() {
        this(new LecturerAccessDAO(), new CourseDAO());
    }

    /** Constructor injection so the rules can be checked with fake DAOs. */
    public LecturerAccessService(LecturerAccessDAO dao, CourseDAO courseDAO) {
        this.dao = dao;
        this.courseDAO = courseDAO;
    }

    /** @throws AuthorizationException unless a lecturer is logged in */
    public int currentLecturerId() {
        Session.requireRole(Role.LECTURER);
        return Session.getCurrentUser().getUserId();
    }

    public List<Course> getAssignedCourses() throws DatabaseException {
        return courseDAO.findByLecturer(currentLecturerId());
    }

    /** @throws AuthorizationException when the course is not assigned to the logged-in lecturer */
    public Course requireOwnedCourse(int courseId) throws DatabaseException {
        int lecturerId = currentLecturerId();
        if (courseId <= 0 || !dao.ownsCourse(lecturerId, courseId)) {
            throw new AuthorizationException("You are not assigned to this course.");
        }
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new AuthorizationException("You are not assigned to this course.");
        }
        return course;
    }

    public List<StudentRow> getStudents(int courseId) throws DatabaseException {
        requireOwnedCourse(courseId);
        return dao.findStudentsOfCourse(currentLecturerId(), courseId);
    }

    /** @throws AuthorizationException when the student is not in this (owned) course */
    public void requireStudentInCourse(int courseId, int studentId) throws DatabaseException {
        requireOwnedCourse(courseId);
        if (studentId <= 0 || !dao.isStudentInCourse(currentLecturerId(), courseId, studentId)) {
            throw new AuthorizationException("This student is not in your course.");
        }
    }

    /** @throws AuthorizationException when the student is in none of the lecturer's courses */
    public void requireStudentAuthorized(int studentId) throws DatabaseException {
        if (studentId <= 0 || !dao.isStudentAuthorized(currentLecturerId(), studentId)) {
            throw new AuthorizationException("You are not authorized to view this student's records.");
        }
    }

    /** Students of the lecturer's courses whose registration number or name contains the text. */
    public List<StudentRow> searchStudents(String text) throws DatabaseException, ValidationException {
        int lecturerId = currentLecturerId();
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Enter a registration number or part of a student's name.");
        }
        return dao.searchStudents(lecturerId, text);
    }

    /**
     * Attendance of every student who has attendance rows in the course, over all attendance
     * components of the course. A student missing from the map has no attendance recorded.
     */
    public Map<Integer, AcademicRules.AttendanceInfo> attendanceByStudent(int courseId) throws DatabaseException {
        requireOwnedCourse(courseId);
        int totalSessions = AcademicRules.SESSIONS_PER_COMPONENT * dao.countAttendanceComponents(courseId);
        Map<Integer, AcademicRules.AttendanceInfo> map = new HashMap<>();
        for (AttendanceAgg a : dao.attendanceByStudent(courseId)) {
            map.put(a.studentId(), new AcademicRules.AttendanceInfo(
                    a.present(), a.absent(), a.approvedMedical(), totalSessions));
        }
        return map;
    }

    public AcademicRules.AttendanceInfo attendanceOf(Map<Integer, AcademicRules.AttendanceInfo> map, int studentId) {
        return map.getOrDefault(studentId, AcademicRules.AttendanceInfo.none());
    }
}
