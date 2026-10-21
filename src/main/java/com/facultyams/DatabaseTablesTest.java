package com.facultyams;

import com.facultyams.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseTablesTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            ResultSet resultSet =
                    connection.getMetaData().getTables(
                            connection.getCatalog(),
                            null,
                            "%",
                            new String[]{"TABLE"}
                    );

            System.out.println("TABLES IN facultyams_temp:");

            while (resultSet.next()) {
                System.out.println(resultSet.getString("TABLE_NAME"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}