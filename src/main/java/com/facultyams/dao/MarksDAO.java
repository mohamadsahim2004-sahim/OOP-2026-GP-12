package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Marks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class MarksDAO {

    public MarksDAO() {
        // Database connections are obtained when needed.
    }

    public void saveMarks(Marks marks) throws SQLException {

        String sql = "INSERT INTO marks " +
                "(student_id, course_id, obtainMarks, finalMarks, grade, isEligible) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, marks.getStudentId());
            statement.setInt(2, marks.getCourseId());
            statement.setDouble(3, marks.getObtainMarks());
            statement.setDouble(4, marks.getFinalMarks());
            statement.setString(5, marks.getGrade());
            statement.setBoolean(6, marks.isEligible());

            statement.executeUpdate();
        }
    }

    public Marks findMarksById(int marksID) throws SQLException {

        String sql = "SELECT marksid, student_id, course_id, " +
                "obtainMarks, finalMarks, grade, isEligible " +
                "FROM marks WHERE marksid = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, marksID);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return new Marks(
                            resultSet.getInt("marksid"),
                            resultSet.getInt("student_id"),
                            resultSet.getInt("course_id"),
                            resultSet.getDouble("obtainMarks"),
                            resultSet.getDouble("finalMarks"),
                            resultSet.getString("grade"),
                            resultSet.getBoolean("isEligible")
                    );
                }
            }
        }

        return null;
    }

    public List<String> findMarksSummaryByRegistrationNo(
            String registrationNo) throws SQLException {

        String sql = "SELECT s.registration_no, u.full_name, " +
                "c.course_code, c.course_name, " +
                "m.obtainMarks, m.finalMarks, m.grade, m.isEligible " +
                "FROM marks m " +
                "JOIN students s ON m.student_id = s.user_id " +
                "JOIN users u ON s.user_id = u.user_id " +
                "JOIN courses c ON m.course_id = c.course_id " +
                "WHERE s.registration_no = ? " +
                "ORDER BY c.course_code";

        List<String> results = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, registrationNo.trim());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String result =
                            "Student: " + rs.getString("registration_no") +
                                    " - " + rs.getString("full_name") + "\n" +
                                    "Course: " + rs.getString("course_code") +
                                    " - " + rs.getString("course_name") + "\n" +
                                    "CA Marks: " + rs.getString("obtainMarks") + "\n" +
                                    "Final Marks: " + rs.getString("finalMarks") + "\n" +
                                    "Grade: " + rs.getString("grade") + "\n" +
                                    "CA Eligible: " + rs.getBoolean("isEligible") + "\n" +
                                    "------------------------------";

                    results.add(result);
                }
            }
        }

        return results;
    }
}