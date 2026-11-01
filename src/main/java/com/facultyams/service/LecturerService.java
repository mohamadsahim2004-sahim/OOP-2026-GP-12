package com.facultyams.service;

import com.facultyams.dao.EvaluationComponentDAO;
import com.facultyams.dao.LecturerAccessDAO;
import com.facultyams.dao.LecturerAccessDAO.AttendanceRecord;
import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.LecturerDAO;
import com.facultyams.dao.MarksDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.Lecturer;
import com.facultyams.util.ValidationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Middle layer between the lecturer screens and the database: profile, dashboard figures and
 * attendance viewing. (Dashboard -> LecturerService -> DAOs -> MySQL)
 */
public class LecturerService {

    /** One course on the dashboard. */
    public record CourseLine(Course course, int students, boolean schemeComplete, double totalWeight,
                             int results, int published) {
    }

    public record Summary(Lecturer lecturer, int courses, int students, int coursesWithoutScheme,
                          int resultsToPublish, int publishedResults, int pendingMedical,
                          List<CourseLine> courseLines) {
    }

    public record AttendanceRow(StudentRow student, AcademicRules.AttendanceInfo info) {
    }

    public record AttendanceView(Course course, int sessionsExpected, List<AttendanceRow> rows) {
    }

    private final LecturerDAO lecturerDAO;
    private final LecturerAccessService access;
    private final LecturerAccessDAO accessDAO;
    private final EvaluationComponentDAO componentDAO;
    private final MarksDAO marksDAO;

    public LecturerService() {
        this(new LecturerDAO(), new LecturerAccessService(), new LecturerAccessDAO(),
                new EvaluationComponentDAO(), new MarksDAO());
    }

    public LecturerService(LecturerDAO lecturerDAO, LecturerAccessService access, LecturerAccessDAO accessDAO,
                           EvaluationComponentDAO componentDAO, MarksDAO marksDAO) {
        this.lecturerDAO = lecturerDAO;
        this.access = access;
        this.accessDAO = accessDAO;
        this.componentDAO = componentDAO;
        this.marksDAO = marksDAO;
    }

    // ------------------------------------------------------------------ profile

    public Lecturer findByLecturerID(int lecturerID) {
        if (lecturerID <= 0) {
            throw new IllegalArgumentException("Lecturer ID must be greater than 0");
        }
        Lecturer lecturer = lecturerDAO.findByLecturerID(lecturerID);
        if (lecturer == null) {
            throw new IllegalArgumentException("Lecturer not found");
        }
        return lecturer;
    }

    /** The profile of the logged-in lecturer (taken from the session, not from the screen). */
    public Lecturer getCurrentLecturer() {
        return findByLecturerID(access.currentLecturerId());
    }

    /** A lecturer can only change his or her own profile. */
    public void updateProfile(int userID, String fullName, String email, String phone, String profilePicture)
            throws ValidationException {
        if (userID != access.currentLecturerId()) {
            throw new AuthorizationException("You can only edit your own profile.");
        }
        String name = ValidationUtil.requireNotBlank(fullName, "Name");
        ValidationUtil.requireMaxLength(name, 100, "Name");
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new ValidationException("Enter a valid phone number.");
        }
        try {
            lecturerDAO.updateProfile(userID, name, email.trim(), phone.trim(), profilePicture);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                throw new ValidationException("That email address is already used by another account.");
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------ dashboard

    public Summary getDashboardSummary() throws DatabaseException {
        int lecturerId = access.currentLecturerId();
        List<CourseLine> lines = new ArrayList<>();
        int withoutScheme = 0;
        int toPublish = 0;
        int published = 0;
        for (Course course : access.getAssignedCourses()) {
            AcademicRules.SchemeCheck check = AcademicRules.checkScheme(componentDAO.findByCourse(course.getCourseId()));
            int results = marksDAO.countResults(course.getCourseId());
            int pub = marksDAO.countPublished(course.getCourseId());
            lines.add(new CourseLine(course, accessDAO.findStudentsOfCourse(lecturerId, course.getCourseId()).size(),
                    check.complete(), check.totalWeight(), results, pub));
            if (!check.complete()) {
                withoutScheme++;
            }
            toPublish += results - pub;
            published += pub;
        }
        return new Summary(getCurrentLecturer(), lines.size(), accessDAO.countStudents(lecturerId),
                withoutScheme, toPublish, published, accessDAO.countPendingMedical(lecturerId), lines);
    }

    // ------------------------------------------------------------------ attendance viewing

    /** Attendance of every student of an assigned course (students with no records are included). */
    public AttendanceView getAttendance(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        Map<Integer, AcademicRules.AttendanceInfo> byStudent = access.attendanceByStudent(courseId);
        int expected = AcademicRules.SESSIONS_PER_COMPONENT * accessDAO.countAttendanceComponents(courseId);
        List<AttendanceRow> rows = new ArrayList<>();
        for (StudentRow s : access.getStudents(courseId)) {
            rows.add(new AttendanceRow(s, access.attendanceOf(byStudent, s.studentId())));
        }
        return new AttendanceView(course, expected, rows);
    }

    /** Session-by-session attendance of one student of an assigned course. */
    public List<AttendanceRecord> getAttendanceRecords(int courseId, int studentId) throws DatabaseException {
        access.requireStudentInCourse(courseId, studentId);
        return accessDAO.attendanceRecords(courseId, studentId);
    }

    /** Students of the lecturer's assigned courses, optionally for one course. */
    public List<StudentRow> getStudents(Integer courseId) throws DatabaseException {
        if (courseId != null) {
            return access.getStudents(courseId);
        }
        Map<Integer, StudentRow> unique = new HashMap<>();
        List<StudentRow> ordered = new ArrayList<>();
        for (Course c : access.getAssignedCourses()) {
            for (StudentRow s : access.getStudents(c.getCourseId())) {
                if (unique.put(s.studentId(), s) == null) {
                    ordered.add(s);
                }
            }
        }
        ordered.sort((a, b) -> a.registrationNo().compareTo(b.registrationNo()));
        return ordered;
    }
}
