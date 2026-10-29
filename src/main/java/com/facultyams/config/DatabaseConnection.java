package com.facultyams.config;

import com.facultyams.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Creates JDBC connections to the MySQL database.
 *
 * Settings are read from the classpath in this order (later wins):
 *   1. application.properties  (committed team defaults)
 *   2. db.properties           (your local override - NOT committed, see .gitignore)
 *   3. environment variables   FOTAMS_DB_URL, FOTAMS_DB_USER, FOTAMS_DB_PASSWORD
 *
 * Connections are reused: {@link #getConnection()} hands out a lightweight wrapper whose close()
 * returns the real connection to a small idle pool (it is validated before it is handed out again,
 * and any open transaction is rolled back when it is returned). DAO code does not change.
 *
 * Always use try-with-resources so connections are returned:
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

    /** How many idle connections are kept for reuse. */
    private static final int MAX_IDLE = 4;
    private static final Deque<Connection> IDLE = new ArrayDeque<>();

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
     * Returns a connection to the database, reusing an idle one when possible. Closing it (try-with-resources)
     * returns it to the pool.
     *
     * @throws DatabaseException with a readable message if MySQL cannot be reached
     */
    public static Connection getConnection() throws DatabaseException {
        Connection real = takeIdle();
        if (real == null) {
            real = openNew();
        }
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, new PooledHandle(real));
    }

    private static Connection openNew() throws DatabaseException {
        String url = SETTINGS.getProperty("db.url", DEFAULT_URL);
        String user = SETTINGS.getProperty("db.user", DEFAULT_USER);
        String password = SETTINGS.getProperty("db.password", DEFAULT_PASSWORD);
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
                throw new DatabaseException("The MySQL driver (mysql-connector-j) is not on the classpath, so the "
                        + "app was started without its libraries. Close the app and start it with \"mvn exec:java\" "
                        + "(see the Build and run section of README.md).", e);
            }
            throw new DatabaseException("Cannot connect to the database. Check that MySQL is running and "
                    + "that db.url / db.user / db.password are correct. (" + e.getMessage() + ")", e);
        }
    }

    /** Takes a still-valid idle connection, discarding dead ones; null if none is available. */
    private static Connection takeIdle() {
        while (true) {
            Connection candidate;
            synchronized (IDLE) {
                candidate = IDLE.pollFirst();
            }
            if (candidate == null) {
                return null;
            }
            try {
                if (!candidate.isClosed() && candidate.isValid(2)) {
                    return candidate;
                }
            } catch (SQLException e) {
                // fall through: discard it and try the next one
            }
            closeQuietly(candidate);
        }
    }

    /** Puts a connection back into the idle pool (or closes it if it is unusable or the pool is full). */
    private static void giveBack(Connection real) {
        try {
            if (real.isClosed()) {
                return;
            }
            if (!real.getAutoCommit()) {
                real.rollback();
                real.setAutoCommit(true);
            }
            real.clearWarnings();
        } catch (SQLException e) {
            closeQuietly(real);
            return;
        }
        synchronized (IDLE) {
            if (IDLE.size() < MAX_IDLE) {
                IDLE.addFirst(real);
                return;
            }
        }
        closeQuietly(real);
    }

    /** Closes every idle connection (e.g. when the application exits). */
    public static void closeAll() {
        synchronized (IDLE) {
            Connection c;
            while ((c = IDLE.pollFirst()) != null) {
                closeQuietly(c);
            }
        }
    }

    private static void closeQuietly(Connection c) {
        try {
            c.close();
        } catch (SQLException e) {
            // nothing useful to do
        }
    }

    /** The object a caller actually holds: forwards everything to the real connection except close(). */
    private static final class PooledHandle implements InvocationHandler {
        private final Connection real;
        private boolean returned;

        PooledHandle(Connection real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "close" -> {
                    synchronized (this) {
                        if (!returned) {
                            returned = true;
                            giveBack(real);
                        }
                    }
                    return null;
                }
                case "isClosed" -> {
                    return isReturned() || real.isClosed();
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return "PooledConnection[" + (isReturned() ? "returned" : real) + "]";
                }
                default -> {
                    if (isReturned()) {
                        throw new SQLException("This connection has already been closed.");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        }

        private synchronized boolean isReturned() {
            return returned;
        }
    }

    public record ConnectionInfo(String host, String port, String database, String user) {
    }

    /** Parses db.url ("jdbc:mysql://host:port/database?...") for display on the Settings screen. */
    public static ConnectionInfo getConnectionInfo() {
        String url = SETTINGS.getProperty("db.url", DEFAULT_URL);
        String user = SETTINGS.getProperty("db.user", DEFAULT_USER);
        String rest = url.contains("://") ? url.substring(url.indexOf("://") + 3) : url;
        int queryStart = rest.indexOf('?');
        if (queryStart >= 0) {
            rest = rest.substring(0, queryStart);
        }
        String hostPort = rest;
        String database = "";
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            hostPort = rest.substring(0, slash);
            database = rest.substring(slash + 1);
        }
        String host = hostPort;
        String port = "3306";
        int colon = hostPort.lastIndexOf(':');
        if (colon >= 0) {
            host = hostPort.substring(0, colon);
            port = hostPort.substring(colon + 1);
        }
        return new ConnectionInfo(host, port, database, user);
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