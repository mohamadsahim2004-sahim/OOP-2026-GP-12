package com.facultyams.config;

import com.facultyams.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections to the MySQL database.
 *
 * Settings are read from the classpath in this order (later wins):
 *   1. application.properties  (committed team defaults)
 *   2. db.properties           (your local override - NOT committed, see .gitignore)
 *   3. environment variables   FOTAMS_DB_URL, FOTAMS_DB_USER, FOTAMS_DB_PASSWORD
 *
 * Always use try-with-resources so connections are closed:
 * <pre>
 *   try (Connection con = DatabaseConnection.getConnection();
 *        PreparedStatement ps = con.prepareStatement(sql)) { ... }
 * </pre>
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/faculty_ams?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final Properties SETTINGS = loadSettings();

    private DatabaseConnection() {
        // utility class
    }

    private static Properties loadSettings() {
        Properties props = new Properties();
        loadFromClasspath(props, "application.properties");
        loadFromClasspath(props, "db.properties");

        overrideFromEnv(props, "db.url", "FOTAMS_DB_URL");
        overrideFromEnv(props, "db.user", "FOTAMS_DB_USER");
        overrideFromEnv(props, "db.password", "FOTAMS_DB_PASSWORD");
        return props;
    }

    private static void loadFromClasspath(Properties props, String fileName) {
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(fileName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read " + fileName + ": " + e.getMessage());
        }
    }

    private static void overrideFromEnv(Properties props, String key, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }

    /**
     * Opens a new connection. The caller must close it (try-with-resources).
     *
     * @throws DatabaseException if the driver is missing or MySQL cannot be reached
     */
    public static Connection getConnection() throws DatabaseException {
        String url = SETTINGS.getProperty("db.url", DEFAULT_URL);
        String user = SETTINGS.getProperty("db.user", DEFAULT_USER);
        String password = SETTINGS.getProperty("db.password", DEFAULT_PASSWORD);
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot connect to the database. Check that MySQL is running and "
                    + "that db.url / db.user / db.password are correct. (" + e.getMessage() + ")", e);
        }
    }

    /** Returns true if a connection can be opened. Used on start-up. */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con.isValid(3);
        } catch (DatabaseException | SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}