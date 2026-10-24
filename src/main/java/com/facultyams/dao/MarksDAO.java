package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Marks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class MarksDAO {

    public MarksDAO() {
        // Connections are obtained when needed.
    }

    // Save marks and calculated results
    public void saveMarks(Marks marks) throws SQLException {

        String sql = "INSERT INTO marks " +
                "(student_id, course_id, obtainMarks, finalMarks, " +
                "grade, isEligible, overall_mark, grade_points) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, marks.getStudentId());
            statement.setInt(2, marks.getCourseId());
            statement.setDouble(3, marks.getObtainMarks());
            statement.setDouble(4, marks.getFinalMarks());
            statement.setString(5, marks.getGrade());
            statement.setBoolean(6, marks.isEligible());
            statement.setDouble(7, marks.getOverallMark());
            statement.setDouble(8, marks.getGradePoints());

            statement.executeUpdate();
        }
    }

    // Find a marks record by its database ID
    public Marks findMarksById(int marksID) throws SQLException {

        String sql = "SELECT marksid, student_id, course_id, " +
                "obtainMarks, finalMarks, grade, isEligible, " +
                "overall_mark, grade_points " +
                "FROM marks WHERE marksid = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, marksID);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Marks marks = new Marks(
                            rs.getInt("marksid"),
                            rs.getInt("student_id"),
                            rs.getInt("course_id"),
                            rs.getDouble("obtainMarks"),
                            rs.getDouble("finalMarks"),
                            rs.getString("grade"),
                            rs.getBoolean("isEligible")
                    );

                    marks.setOverallMark(rs.getDouble("overall_mark"));
                    marks.setGradePoints(rs.getDouble("grade_points"));

                    return marks;
                }
            }
        }

        return null;
    }

    // Find all saved marks for a student using their TG registration number
    public List<String> findMarksSummaryByRegistrationNo(
            String registrationNo) throws SQLException {

        String sql = "SELECT s.registration_no, u.full_name, " +
                "c.course_code, c.course_name, m.obtainMarks, " +
                "m.finalMarks, m.overall_mark, m.grade, " +
                "m.grade_points, m.isEligible " +
                "FROM marks m " +
                "JOIN students s ON m.student_id = s.user_id " +
                "JOIN users u ON s.user_id = u.user_id " +
                "JOIN courses c ON m.course_id = c.course_id " +
                "WHERE s.registration_no = ? " +
                "ORDER BY c.course_code";

        List<String> results = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, registrationNo.trim());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String result =
                            "Student: " + rs.getString("registration_no") +
                                    " - " + rs.getString("full_name") + "\n" +
                                    "Course: " + rs.getString("course_code") +
                                    " - " + rs.getString("course_name") + "\n" +
                                    "CA Marks: " + rs.getDouble("obtainMarks") + "\n" +
                                    "Final Exam Marks: " +
                                    rs.getDouble("finalMarks") + "\n" +
                                    "Overall Mark: " +
                                    rs.getDouble("overall_mark") + "/100\n" +
                                    "Grade: " + rs.getString("grade") + "\n" +
                                    "Grade Points: " +
                                    rs.getDouble("grade_points") + "\n" +
                                    "CA Eligible: " +
                                    rs.getBoolean("isEligible") + "\n" +
                                    "--------------------------------";

                    results.add(result);
                }
            }
        }

        return results;
    }

    // Calculate SGPA for each semester of a student
    public Map<Integer, Double> findSGPAByRegistrationNo(
            String registrationNo) throws SQLException {

        String sql = "SELECT c.semester, " +
                "SUM(m.grade_points * c.credits) / " +
                "NULLIF(SUM(c.credits), 0) AS sgpa " +
                "FROM marks m " +
                "JOIN students s ON m.student_id = s.user_id " +
                "JOIN courses c ON m.course_id = c.course_id " +
                "WHERE s.registration_no = ? " +
                "AND m.grade_points IS NOT NULL " +
                "GROUP BY c.semester " +
                "ORDER BY c.semester";

        Map<Integer, Double> results = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, registrationNo.trim());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.put(
                            rs.getInt("semester"),
                            rs.getDouble("sgpa")
                    );
                }
            }
        }

        return results;
    }

    // Calculate CGPA across all currently stored course results
    public double findCGPAByRegistrationNo(
            String registrationNo) throws SQLException {

        String sql = "SELECT COALESCE(" +
                "SUM(m.grade_points * c.credits) / " +
                "NULLIF(SUM(c.credits), 0), 0) AS cgpa " +
                "FROM marks m " +
                "JOIN students s ON m.student_id = s.user_id " +
                "JOIN courses c ON m.course_id = c.course_id " +
                "WHERE s.registration_no = ? " +
                "AND m.grade_points IS NOT NULL";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, registrationNo.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cgpa");
                }
            }
        }

        return 0.0;
    }
}