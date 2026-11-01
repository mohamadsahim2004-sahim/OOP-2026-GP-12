package com.facultyams;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Statement;

public class CreateUsersTable {

    public static void main(String[] args) {

        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(30) NOT NULL,
                    full_name VARCHAR(100),
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    profilePicture VARCHAR(255),
                    departmentID INT
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

            System.out.println("Users table created successfully.");

        } catch (Exception e) {
            System.out.println("Failed to create users table.");
            e.printStackTrace();
        }
    }
}

//user table created according to the srs