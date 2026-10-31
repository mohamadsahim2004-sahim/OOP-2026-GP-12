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
import java.util.function.Function;

/**
 * Creates JDBC connections to the MySQL database.
 *
 * Settings come from (later wins):
 *   1. db.properties on the classpath (src/main/resources/db.properties - YOUR local file, git-ignored;
 *      copy db.properties.example). Keys: db.url, db.user (db.username is accepted too), db.password.
 *   2. environment variables FOTAMS_DB_URL, FOTAMS_DB_USER, FOTAMS_DB_PASSWORD
 *
 * There is deliberately NO default user or password: the application never falls back to the MySQL
 * "root" account. If no user is configured, {@link #getConnection()} fails with a message that says how
 * to configure one.
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

    /** Local development URL; it contains no credentials. */
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/faculty_ams?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo";

    private static final String NOT_CONFIGURED = "The database is not configured. Copy db.properties.example to "
            + "db.properties and enter your MySQL user and password (or set FOTAMS_DB_USER and FOTAMS_DB_PASSWORD), "
            + "then restart. See README.md, \"Database configuration\".";

    /** Fail fast when MySQL is not running, instead of leaving the window waiting for the operating system. */
    private static final String CONNECT_TIMEOUT_MS = "5000";
    /** Never wait forever for an answer to a query. */
    private static final String SOCKET_TIMEOUT_MS = "30000";

    /** Connection settings after merging db.properties with the environment. */
    record Settings(String url, String user, String password) {
        boolean hasUser() {
            return user != null && !user.isBlank();
        }
    }

    private static final Settings SETTINGS = resolveSettings(loadFile(), System::getenv);

    /** How many idle connections are kept for reuse. */
    private static final int MAX_IDLE = 4;
    private static final Deque<Connection> IDLE = new ArrayDeque<>();

    private DatabaseConnection() {
        // utility class
    }

    private static Properties loadFile() {
        Properties props = new Properties();
        loadFromClasspath(props, "db.properties");
        return props;
    }

    /**
     * Merges the db.properties values with the environment (the environment wins). Package-private so the
     * rules can be tested without a database. There is no default user or password.
     */
    static Settings resolveSettings(Properties file, Function<String, String> env) {
        String url = firstNonBlank(env.apply("FOTAMS_DB_URL"), file.getProperty("db.url"));
        String user = firstNonBlank(env.apply("FOTAMS_DB_USER"), file.getProperty("db.user"),
                file.getProperty("db.username"));
        String password = env.apply("FOTAMS_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            password = file.getProperty("db.password");
        }
        return new Settings(url == null ? DEFAULT_URL : url.trim(), user == null ? null : user.trim(),
                password == null ? "" : password);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
        if (!SETTINGS.hasUser()) {
            throw new DatabaseException(NOT_CONFIGURED);
        }
        Properties info = new Properties();
        info.setProperty("user", SETTINGS.user());
        info.setProperty("password", SETTINGS.password());
        info.setProperty("connectTimeout", CONNECT_TIMEOUT_MS);
        info.setProperty("socketTimeout", SOCKET_TIMEOUT_MS);
        try {
            return DriverManager.getConnection(SETTINGS.url(), info);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
                throw new DatabaseException("The MySQL driver (mysql-connector-j) is not on the classpath, so the "
                        + "app was started without its libraries. Close the app and start it with Maven or the "
                        + "VS Code \"Run Faculty AMS\" launch configuration (see README.md, \"Run the application\").", e);
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
        String url = SETTINGS.url();
        String user = SETTINGS.hasUser() ? SETTINGS.user() : "(not set)";
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