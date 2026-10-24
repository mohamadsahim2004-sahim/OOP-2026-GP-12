package com.facultyams;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseTablesTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet =
                     statement.executeQuery("SELECT * FROM users")) {

            while (resultSet.next()) {

                System.out.println("User ID: "
                        + resultSet.getInt("user_id"));

                System.out.println("Username: "
                        + resultSet.getString("username"));

                System.out.println("Name: "
                        + resultSet.getString("full_name"));

                System.out.println("Email: "
                        + resultSet.getString("email"));

                System.out.println("Phone: "
                        + resultSet.getString("phone"));

                System.out.println("Role: "
                        + resultSet.getString("role"));

                System.out.println("----------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}