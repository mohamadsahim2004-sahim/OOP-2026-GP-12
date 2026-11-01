package com.facultyams.config;

import com.facultyams.exception.DatabaseException;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Checks for how the database settings are resolved. No MySQL is needed.
 *
 * <pre>
 *   java -cp target/classes:target/test-classes com.facultyams.config.DatabaseConfigChecks
 * </pre>
 *
 * Part 1 tests the merge rules directly (file values, environment overrides, no default user).
 * Part 2 checks that an unconfigured application refuses to connect instead of trying "root"; it only runs
 * when this JVM really has no user configured (no db.properties on the classpath, no FOTAMS_DB_USER).
 */
public final class DatabaseConfigChecks {

    private static final AtomicInteger CONNECT_ATTEMPTS = new AtomicInteger();
    private static int passed;
    private static int failed;
    private static int skipped;

    private DatabaseConfigChecks() {
    }

    public static void main(String[] args) throws Exception {
        resolveRules();
        notConfiguredBehaviour();
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed, " + skipped + " skipped");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    private static Properties file(String... keyValues) {
        Properties p = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            p.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return p;
    }

    private static Function<String, String> env(String... keyValues) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(keyValues[i], keyValues[i + 1]);
        }
        return m::get;
    }

    private static void resolveRules() {
        System.out.println("[1] Settings: db.properties + environment");

        DatabaseConnection.Settings none = DatabaseConnection.resolveSettings(file(), env());
        check("nothing configured: there is no user (no default 'root')", !none.hasUser() && none.user() == null);
        check("nothing configured: the password is empty, not a built-in secret", "".equals(none.password()));
        check("nothing configured: the URL is the local database and carries no credentials",
                none.url().startsWith("jdbc:mysql://localhost:3306/faculty_ams")
                        && !none.url().toLowerCase().contains("password")
                        && !none.url().toLowerCase().contains("user="));

        DatabaseConnection.Settings fromFile = DatabaseConnection.resolveSettings(
                file("db.url", "jdbc:mysql://dbhost:3307/x", "db.user", "faculty_user", "db.password", "s3cret pass "),
                env());
        check("db.properties values are used", "jdbc:mysql://dbhost:3307/x".equals(fromFile.url())
                && "faculty_user".equals(fromFile.user()) && fromFile.hasUser());
        check("a password is kept exactly as written (spaces are not trimmed)", "s3cret pass ".equals(fromFile.password()));

        DatabaseConnection.Settings legacy = DatabaseConnection.resolveSettings(
                file("db.username", "old_style_user", "db.password", "p"), env());
        check("the older key db.username is still accepted", "old_style_user".equals(legacy.user()));

        DatabaseConnection.Settings both = DatabaseConnection.resolveSettings(
                file("db.user", "preferred", "db.username", "legacy"), env());
        check("db.user wins over db.username", "preferred".equals(both.user()));

        DatabaseConnection.Settings overridden = DatabaseConnection.resolveSettings(
                file("db.url", "jdbc:mysql://filehost/x", "db.user", "file_user", "db.password", "file_pw"),
                env("FOTAMS_DB_URL", "jdbc:mysql://envhost/y", "FOTAMS_DB_USER", "env_user", "FOTAMS_DB_PASSWORD", "env_pw"));
        check("environment variables override the file (url, user, password)",
                "jdbc:mysql://envhost/y".equals(overridden.url()) && "env_user".equals(overridden.user())
                        && "env_pw".equals(overridden.password()));

        DatabaseConnection.Settings blankEnv = DatabaseConnection.resolveSettings(
                file("db.user", "file_user", "db.password", "file_pw"),
                env("FOTAMS_DB_USER", "  ", "FOTAMS_DB_PASSWORD", ""));
        check("blank environment values are ignored, the file is used", "file_user".equals(blankEnv.user())
                && "file_pw".equals(blankEnv.password()));

        DatabaseConnection.Settings blankUser = DatabaseConnection.resolveSettings(file("db.user", "   "), env());
        check("a blank db.user counts as not configured", !blankUser.hasUser());

        DatabaseConnection.Settings trimmed = DatabaseConnection.resolveSettings(file("db.user", "  faculty_user  "), env());
        check("the user name is trimmed", "faculty_user".equals(trimmed.user()));
    }

    private static void notConfiguredBehaviour() throws Exception {
        System.out.println("\n[2] An unconfigured application refuses to connect");
        if (!"(not set)".equals(DatabaseConnection.getConnectionInfo().user())) {
            skipped++;
            System.out.println("  SKIP  this JVM has a database user configured (db.properties or FOTAMS_DB_USER)");
            return;
        }
        DriverManager.registerDriver(new RecordingDriver());

        DatabaseException error = null;
        try (Connection c = DatabaseConnection.getConnection()) {
            check("getConnection() must not succeed without a configured user", false);
        } catch (DatabaseException e) {
            error = e;
        }
        check("getConnection() throws DatabaseException when no user is configured", error != null);
        String message = error == null ? "" : error.getMessage();
        check("the message explains how to configure (db.properties.example and the environment variables)",
                message.contains("db.properties.example") && message.contains("FOTAMS_DB_USER") && message.length() < 220);
        check("the message never contains a password or user name", !message.toLowerCase().contains("root"));
        check("no connection was attempted, so there is no silent fallback to 'root'", CONNECT_ATTEMPTS.get() == 0);
        check("testConnection() reports false instead of throwing", !DatabaseConnection.testConnection());
        check("the Settings screen shows '(not set)' for the user", "(not set)".equals(
                DatabaseConnection.getConnectionInfo().user()));
    }

    /** Counts connection attempts for any jdbc:mysql: URL and always refuses. */
    private static final class RecordingDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            CONNECT_ATTEMPTS.incrementAndGet();
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mysql:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return null;
        }
    }
}
