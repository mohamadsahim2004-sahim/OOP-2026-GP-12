
package com.facultyams.dao;
import com.facultyams.exception.DatabaseException;
import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttendanceMedicalDAO {

    public List<String> getAttendanceRecords()
            throws SQLException, DatabaseException {        List<String> records = new ArrayList<>();

        String sql =
                "SELECT s.registration_no, u.full_name, " +
                        "c.course_code, c.course_name, cc.type, " +
                        "sa.session_no, sa.date AS attendance_date, sa.status " +
                        "FROM session_attendance sa " +
                        "JOIN students s ON s.user_id = sa.user_id " +
                        "JOIN users u ON u.user_id = s.user_id " +
                        "JOIN course_component cc ON cc.component_id = sa.component_id " +
                        "JOIN courses c ON c.course_id = cc.course_id " +
                        "ORDER BY s.registration_no, c.course_code, sa.session_no";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String record =
                        "Registration No: " + rs.getString("registration_no") +
                                "\nStudent: " + rs.getString("full_name") +
                                "\nCourse: " + rs.getString("course_code") +
                                " - " + rs.getString("course_name") +
                                "\nComponent: " + rs.getString("type") +
                                "\nSession: " + rs.getInt("session_no") +
                                "\nDate: " + rs.getDate("attendance_date") +
                                "\nStatus: " + rs.getString("status");

                records.add(record);
            }
        }

        return records;
    }

        List<String> records = new ArrayList<>();
    public List<String> getMedicalRecords()
            throws SQLException, DatabaseException {
        String sql =
                "SELECT m.medical_id, s.registration_no, u.full_name, " +
                        "c.course_code, cc.type, sa.session_no, " +
                        "m.submitted_date, m.reason, m.approval_status, " +
                        "reviewer.full_name AS reviewer_name " +
                        "FROM medical m " +
                        "JOIN session_attendance sa " +
                        "ON sa.attendance_id = m.attendance_id " +
                        "JOIN students s ON s.user_id = sa.user_id " +
                        "JOIN users u ON u.user_id = s.user_id " +
                        "JOIN course_component cc " +
                        "ON cc.component_id = sa.component_id " +
                        "JOIN courses c ON c.course_id = cc.course_id " +
                        "LEFT JOIN users reviewer ON reviewer.user_id = m.reviewed_by " +
                        "ORDER BY m.submitted_date DESC, m.medical_id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String record =
                        "Medical ID: " + rs.getInt("medical_id") +
                                "\nRegistration No: " + rs.getString("registration_no") +
                                "\nStudent: " + rs.getString("full_name") +
                                "\nCourse: " + rs.getString("course_code") +
                                "\nComponent: " + rs.getString("type") +
                                "\nSession: " + rs.getInt("session_no") +
                                "\nSubmitted Date: " + rs.getDate("submitted_date") +
                                "\nReason: " + rs.getString("reason") +
                                "\nApproval Status: " + rs.getString("approval_status") +
                                "\nReviewed By: " + rs.getString("reviewer_name");

                records.add(record);
            }
        }

        return records;
    }
}
