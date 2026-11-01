package com.facultyams;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseStructureTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            System.out.println("USERS TABLE:");

            ResultSet users = connection.getMetaData()
                    .getColumns(null, null, "users", null);

            while (users.next()) {
                System.out.println(users.getString("COLUMN_NAME"));
            }

            System.out.println("\nLECTURERS TABLE:");

            ResultSet lecturers = connection.getMetaData()
                    .getColumns(null, null, "lecturers", null);

            while (lecturers.next()) {
                System.out.println(lecturers.getString("COLUMN_NAME"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}