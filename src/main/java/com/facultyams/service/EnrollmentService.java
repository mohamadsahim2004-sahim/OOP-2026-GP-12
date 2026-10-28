package com.facultyams.service;

import com.facultyams.dao.EnrollmentDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.CourseOffering;
import com.facultyams.security.StudentAccess;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Course enrollment for the logged-in student. All rules are enforced here (the back end), not only in the UI:
 * <ul>
 *   <li>the account must be an active student and the course must belong to the student's department;</li>
 *   <li>no duplicate enrollment (also guaranteed by the database UNIQUE key);</li>
 *   <li>a course that was already passed cannot be taken again (a failed course may be repeated);</li>
 *   <li>total enrolled credits may not exceed {@value #MAX_CREDITS};</li>
 *   <li>the course's weekly timetable slots may not overlap an enrolled course;</li>
 *   <li>a course with marks or attendance recorded cannot be dropped.</li>
 * </ul>
 */
public final class EnrollmentService {
    public static final int MAX_CREDITS = 20;

    private final EnrollmentDAO dao = new EnrollmentDAO();
    private final List<EnrollmentListener> listeners = new ArrayList<>();

    public void addEnrollmentListener(EnrollmentListener listener) {
        if (listener != null) listeners.add(listener);
    }

    /** Courses of the student's department, enrolled or not. */
    public List<CourseOffering> getOffered() {
        int me = StudentAccess.currentStudentId();
        try {
            return dao.findOfferedCourses(me);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Courses could not be loaded. Please try again.", e);
        }
    }

    public List<CourseOffering> getEnrolled() {
        return getOffered().stream().filter(CourseOffering::isEnrolled).toList();
    }

    public List<CourseOffering> getAvailable() {
        return getOffered().stream().filter(c -> !c.isEnrolled()).toList();
    }

    public int getTotalCredits() {
        int me = StudentAccess.currentStudentId();
        try {
            return dao.enrolledCredits(me);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Credits could not be loaded. Please try again.", e);
        }
    }

    public EnrollmentResult validateEnrollment(CourseOffering course) {
        int me = StudentAccess.currentStudentId();
        if (course == null || course.getCode() == null) {
            return EnrollmentResult.failure("Select a course first.");
        }
        try {
            if (!dao.isActiveStudent(me)) {
                return EnrollmentResult.failure("Your account is not active, so you cannot enroll.");
            }
            Integer courseId = dao.findOfferedCourseId(me, course.getCode());
            CourseOffering stored = dao.findOffered(me, course.getCode());
            if (courseId == null || stored == null) {
                return EnrollmentResult.failure(course.getCode() + " is not offered to your department.");
            }
            if (stored.isEnrolled()) {
                return EnrollmentResult.failure("You are already enrolled in " + course.getCode() + ".");
            }
            if (dao.hasPassed(me, courseId)) {
                return EnrollmentResult.failure("You have already passed " + course.getCode() + ".");
            }
            if (dao.enrolledCredits(me) + stored.getCredits() > MAX_CREDITS) {
                return EnrollmentResult.failure("This course would exceed the " + MAX_CREDITS + "-credit limit.");
            }
            String clash = findClash(dao.courseSlots(courseId), dao.enrolledSlots(me));
            if (clash != null) {
                return EnrollmentResult.failure("This course clashes with " + clash + ".");
            }
            return EnrollmentResult.success("Course is available to enroll.");
        } catch (DatabaseException e) {
            return EnrollmentResult.failure("The database could not be reached. Please try again.");
        }
    }

    public EnrollmentResult enroll(CourseOffering course) {
        EnrollmentResult validation = validateEnrollment(course);
        if (!validation.success()) return validation;
        int me = StudentAccess.currentStudentId();
        try {
            Integer courseId = dao.findOfferedCourseId(me, course.getCode());
            if (courseId == null || !dao.enroll(me, courseId)) {
                return EnrollmentResult.failure("You are already enrolled in " + course.getCode() + ".");
            }
        } catch (DatabaseException e) {
            return EnrollmentResult.failure("Could not enroll in " + course.getCode() + ". Please try again.");
        }
        notifyListeners();
        return EnrollmentResult.success("Enrolled in " + course.getCode() + " successfully.");
    }

    public EnrollmentResult drop(CourseOffering course) {
        int me = StudentAccess.currentStudentId();
        if (course == null || course.getCode() == null) {
            return EnrollmentResult.failure("Select a course first.");
        }
        try {
            Integer courseId = dao.findOfferedCourseId(me, course.getCode());
            if (courseId == null) {
                return EnrollmentResult.failure(course.getCode() + " is not one of your courses.");
            }
            if (dao.hasRecords(me, courseId)) {
                return EnrollmentResult.failure("You cannot drop " + course.getCode()
                        + " because marks or attendance have already been recorded.");
            }
            if (!dao.drop(me, courseId)) {
                return EnrollmentResult.failure("This course is not currently enrolled.");
            }
        } catch (DatabaseException e) {
            return EnrollmentResult.failure("Could not drop " + course.getCode() + ". Please try again.");
        }
        notifyListeners();
        return EnrollmentResult.success("Dropped " + course.getCode() + " successfully.");
    }

    /** Returns "CODE on DAY hh:mm-hh:mm" for the first overlapping slot, or null when there is no clash. */
    static String findClash(List<String[]> requested, List<String[]> enrolled) {
        for (String[] a : requested) {
            LocalTime aStart = LocalTime.parse(a[2]);
            LocalTime aEnd = LocalTime.parse(a[3]);
            for (String[] b : enrolled) {
                if (!a[1].equals(b[1])) continue;
                LocalTime bStart = LocalTime.parse(b[2]);
                LocalTime bEnd = LocalTime.parse(b[3]);
                if (aStart.isBefore(bEnd) && bStart.isBefore(aEnd)) {
                    String day = b[1].substring(0, 1) + b[1].substring(1).toLowerCase();
                    return b[0] + " on " + day + " " + bStart.toString() + "-" + bEnd;
                }
            }
        }
        return null;
    }

    private void notifyListeners() {
        for (EnrollmentListener listener : List.copyOf(listeners)) listener.enrollmentChanged();
    }
}
