package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;
import com.facultyams.model.profile.StudentStatus;
import com.facultyams.model.student.StudentProfile;

import java.sql.SQLException;

/**
 * Reads the logged-in student's own profile and updates only the fields a student may change.
 * Every statement is scoped by {@code user_id} AND {@code role = 'STUDENT'}.
 */
public class StudentProfileDAO {

    private static final String SELECT =
            "SELECT u.user_id, u.username, u.full_name, u.email, u.contact_number, u.profile_picture, u.is_active, "
                    + "s.registration_no, s.batch, s.student_status, d.department_name "
                    + "FROM users u JOIN students s ON s.user_id = u.user_id "
                    + "LEFT JOIN departments d ON d.department_id = u.department_id "
                    + "WHERE u.user_id = ? AND u.role = 'STUDENT'";

    public StudentProfile findProfile(int studentId) throws DatabaseException {
        return StudentDaoSupport.queryOne(SELECT, rs -> {
            StudentProfile p = new StudentProfile();
            p.setUserId(rs.getInt("user_id"));
            p.setUsername(rs.getString("username"));
            p.setFullName(rs.getString("full_name"));
            p.setEmail(rs.getString("email"));
            p.setPhoneNumber(rs.getString("contact_number"));
            p.setProfileImageUrl(rs.getString("profile_picture"));
            p.setStatus(rs.getBoolean("is_active") ? StudentStatus.ACTIVE : StudentStatus.SUSPENDED);
            p.setRegNo(rs.getString("registration_no"));
            p.setBatch(rs.getString("batch"));
            p.setStudentCategory(rs.getString("student_status"));
            p.setDepartment(rs.getString("department_name"));
            return p;
        }, studentId);
    }

    /** Updates the contact number and profile picture only (the two fields a student may edit). */
    public void updateContactAndPicture(int studentId, String contactNumber, String profilePicture)
            throws DatabaseException {
        int rows = StudentDaoSupport.update(
                "UPDATE users SET contact_number = ?, profile_picture = ? WHERE user_id = ? AND role = 'STUDENT'",
                contactNumber, profilePicture, studentId);
        if (rows == 0) {
            throw new DatabaseException("Student account not found.");
        }
    }
}
