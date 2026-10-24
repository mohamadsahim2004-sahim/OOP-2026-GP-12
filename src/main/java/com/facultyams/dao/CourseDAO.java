package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CourseDAO {

    public Map<Integer, String> getAllCourses() throws SQLException {

        String sql = "SELECT course_id, course_code, course_name " +
                "FROM courses ORDER BY course_code";

        Map<Integer, String> courses = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int courseId = resultSet.getInt("course_id");

                String courseName =
                        resultSet.getString("course_code") + " - " +
                                resultSet.getString("course_name");

                courses.put(courseId, courseName);
            }
        }

        return courses;
    }
}

//this should retrieve courses such as ICT2122 – Object Oriented Programming and retain each course's database ID.