package com.fot.ams.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection manages JDBC connections to MySQL database
 * as required by SRS Section 2.4 and Section 6.
 */
public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/fot_ams_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = ""; // Standard local default

    private static Connection connection = null;
    private static boolean driverChecked = false;
    private static boolean mysqlAvailable = false;

    private DBConnection() {
        // Private constructor for singleton connection management
    }

    /**
     * Checks whether the MySQL JDBC driver is present on the classpath.
     */
    public static boolean isDriverAvailable() {
        if (!driverChecked) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverChecked = true;
                mysqlAvailable = true;
            } catch (ClassNotFoundException e) {
                driverChecked = true;
                mysqlAvailable = false;
            }
        }
        return mysqlAvailable;
    }

    /**
     * Gets an active JDBC connection if MySQL server is available.
     *
     * @return Connection or null if offline
     */
    public static Connection getConnection() {
        if (!isDriverAvailable()) {
            return null;
        }
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
            return connection;
        } catch (SQLException e) {
            // Server offline or connection failed; will use DAO seed fallback
            return null;
        }
    }

    /**
     * Closes the active connection if open.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
