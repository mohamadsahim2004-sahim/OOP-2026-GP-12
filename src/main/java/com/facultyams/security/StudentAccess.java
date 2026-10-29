package com.facultyams.security;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.model.User;

/**
 * Ownership checks for the undergraduate module. Every student-facing service calls these on each
 * request so a student can only ever read or change their own records.
 */
public final class StudentAccess {

    private StudentAccess() { }

    /** Id of the logged-in student; refuses when nobody, or a non-student, is logged in. */
    public static int currentStudentId() {
        Session.requireRole(Role.STUDENT);
        User user = Session.getCurrentUser();
        return user.getUserId();
    }

    /**
     * Confirms that {@code studentId} is the logged-in student's own id and returns that id.
     *
     * @throws AuthorizationException if there is no student session or the id belongs to someone else
     */
    public static int requireOwn(String studentId) {
        int me = currentStudentId();
        if (studentId == null || studentId.isBlank()) {
            throw new AuthorizationException("A student id is required.");
        }
        int requested;
        try {
            requested = Integer.parseInt(studentId.trim());
        } catch (NumberFormatException e) {
            throw new AuthorizationException("You can only access your own records.", e);
        }
        if (requested != me) {
            throw new AuthorizationException("You can only access your own records.");
        }
        return me;
    }
}
