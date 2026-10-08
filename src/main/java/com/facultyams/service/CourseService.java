package com.facultyams.service;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

import java.util.List;

/** Course management rules. Changes are Admin-only; reading is open to logged-in users. */
public class CourseService {

    public static final int MIN_CREDITS = 1;
    public static final int MAX_CREDITS = 6;

    private final CourseDAO courseDAO;
    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;

    public CourseService() {
        this(new CourseDAO(), new DepartmentDAO(), new UserDAO());
    }

    public CourseService(CourseDAO courseDAO, DepartmentDAO departmentDAO, UserDAO userDAO) {
        this.courseDAO = courseDAO;
        this.departmentDAO = departmentDAO;
        this.userDAO = userDAO;
    }

    public List<Course> getAllCourses() throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return courseDAO.findAll();
    }

    /** Any filter may be null. */
    public List<Course> searchCourses(String keyword, Integer departmentId, Integer academicLevel, Integer semester)
            throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return courseDAO.search(keyword, departmentId, academicLevel, semester);
    }

    public List<Course> getCoursesByDepartment(int departmentId) throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return courseDAO.findByDepartment(departmentId);
    }

    public List<Course> getCoursesByLecturer(int lecturerId) throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return courseDAO.findByLecturer(lecturerId);
    }

    public Course getCourseById(int courseId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        ValidationUtil.requirePositiveId(courseId, "course id");
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new ValidationException("Course with id " + courseId + " was not found.");
        }
        return course;
    }

    public Course getCourseByCode(String courseCode)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        String code = ValidationUtil.requireNotBlank(courseCode, "Course code").toUpperCase();
        Course course = courseDAO.findByCode(code);
        if (course == null) {
            throw new ValidationException("Course " + code + " was not found.");
        }
        return course;
    }

    public Course addCourse(Course course) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (course == null) {
            throw new ValidationException("Course details are required.");
        }
        course.setCourseId(0);
        validate(course);
        courseDAO.insert(course);
        return course;
    }

    public void updateCourse(Course course) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (course == null) {
            throw new ValidationException("Course details are required.");
        }
        getCourseById(course.getCourseId());
        validate(course);
        courseDAO.update(course);
    }

    /** Fails with a clear message if other modules already store data for this course. */
    public void deleteCourse(int courseId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getCourseById(courseId);
        courseDAO.delete(courseId);
    }

    private void validate(Course course) throws ValidationException, DatabaseException {
        String code = ValidationUtil.requireNotBlank(course.getCourseCode(), "Course code").toUpperCase();
        if (!ValidationUtil.isValidCourseCode(code)) {
            throw new ValidationException("Course code must be 2-4 capital letters followed by 4 digits (e.g. ICT2132).");
        }
        if (courseDAO.codeExists(code, course.getCourseId())) {
            throw new ValidationException("Course code " + code + " already exists.");
        }
        course.setCourseCode(code);

        course.setCourseName(ValidationUtil.requireNotBlank(course.getCourseName(), "Course name"));
        ValidationUtil.requireMaxLength(course.getCourseName(), 100, "Course name");

        ValidationUtil.requireInRange(course.getCredits(), MIN_CREDITS, MAX_CREDITS, "Credits");
        ValidationUtil.requireInRange(course.getAcademicLevel(), 1, 4, "Level");
        ValidationUtil.requireInRange(course.getSemester(), 1, 2, "Semester");
        if (course.getCourseType() == null) {
            throw new ValidationException("Course type is required.");
        }

        if (course.getDepartmentId() <= 0 || departmentDAO.findById(course.getDepartmentId()) == null) {
            throw new ValidationException("Please select an existing department for the course.");
        }

        if (course.getLecturerId() != null) {
            User lecturer = userDAO.findById(course.getLecturerId());
            if (lecturer == null || lecturer.getRole() != Role.LECTURER) {
                throw new ValidationException("The selected lecturer does not exist.");
            }
            if (!lecturer.isActive()) {
                throw new ValidationException("The selected lecturer's account is deactivated.");
            }
        }
    }
}