package com.facultyams.dao;
import com.facultyams.exception.DatabaseException;
import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public Map<Integer, String> getAllStudents()
            throws SQLException, DatabaseException {
        String sql = "SELECT s.user_id, s.registration_no, u.full_name " +
                "FROM students s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "ORDER BY s.registration_no";

        Map<Integer, String> students = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int studentId = resultSet.getInt("user_id");

                String studentName =
                        resultSet.getString("registration_no") + " - " +
                                resultSet.getString("full_name");

                students.put(studentId, studentName);
            }
        }

        return students;
    }

    public int findStudentIdByRegistrationNo(String registrationNo)
            throws java.sql.SQLException, DatabaseException {
        String sql = "SELECT user_id FROM students WHERE registration_no = ?";

        try (java.sql.Connection connection =
                     com.facultyams.config.DatabaseConnection.getConnection();
             java.sql.PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, registrationNo.trim());

            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("user_id");
                }
            }
        }

        return -1;
    }

    public List<String> getUndergraduateDetails()
            throws SQLException, DatabaseException {
        String sql = "SELECT s.registration_no, u.full_name, " +
                "s.batch, s.student_status " +
                "FROM students s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "ORDER BY s.registration_no";

        List<String> details = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String student =
                        "Registration No: " +
                                resultSet.getString("registration_no") + "\n" +
                                "Name: " +
                                resultSet.getString("full_name") + "\n" +
                                "Batch: " +
                                resultSet.getString("batch") + "\n" +
                                "Enrollment Status: " +
                                resultSet.getString("student_status") + "\n" +
                                "----------------------------------------";

                details.add(student);
            }
        }

        return details;
    }
}