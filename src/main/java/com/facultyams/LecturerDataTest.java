package com.facultyams;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LecturerDataTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT * FROM lecturers")) {

            while (resultSet.next()) {

                System.out.println("Lecturer ID: "
                        + resultSet.getInt("lecturerID"));

                System.out.println("User ID: "
                        + resultSet.getInt("userID"));

                System.out.println("Designation: "
                        + resultSet.getString("designation"));

                System.out.println("Specialisation: "
                        + resultSet.getString("specialisation"));

                System.out.println("----------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}