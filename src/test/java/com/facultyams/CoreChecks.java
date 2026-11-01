package com.facultyams;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Admin;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Student;
import com.facultyams.model.TechnicalOfficer;
import com.facultyams.model.User;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.service.AuthService;
import com.facultyams.service.UserService;
import com.facultyams.ui.PlaceholderShell;
import com.facultyams.ui.admin.AdminShell;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Framework-free checks for the Admin/Core foundation (no JUnit, no MySQL server needed).
 * Run from the project root:
 * <pre>
 *   java -cp target/classes:target/test-classes com.facultyams.CoreChecks          (security, services, shell)
 *   FOTAMS_DB_URL=jdbc:fakeams:x FOTAMS_DB_USER=checks java -cp ... com.facultyams.CoreChecks pool   (connection reuse)
 * </pre>
 * Exit code 0 means every check passed. (The class name deliberately does not end in "Test" so Maven
 * Surefire does not try to run it as a unit test.)
 */
public final class CoreChecks {

    private static int passed;
    private static int failed;

    private CoreChecks() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0 && "pool".equals(args[0])) {
            poolChecks();
        } else {
            passwordChecks();
            seedDataChecks();
            authServiceChecks();
            sessionAndRoleChecks();
            servicePermissionChecks();
            hashExposureChecks();
            shellAccessChecks();
        }
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------- helpers

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private interface Thrower {
        void run() throws Exception;
    }

    private static boolean throwsA(Class<? extends Throwable> type, Thrower t) {
        try {
            t.run();
            return false;
        } catch (Throwable e) {
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            return type.isInstance(cause);
        }
    }

    private static User newUser(Role role) {
        User u = switch (role) {
            case ADMIN -> new Admin();
            case LECTURER -> new Lecturer();
            case TECHNICAL_OFFICER -> new TechnicalOfficer();
            case STUDENT -> new Student();
        };
        u.setUserId(1);
        u.setUsername("test." + role.name().toLowerCase());
        u.setFullName("Test " + role.getDisplayName());
        u.setActive(true);
        return u;
    }

    // ---------------------------------------------------------------- 1. password hashing

    private static void passwordChecks() {
        System.out.println("[1] PasswordUtil");
        String h1 = PasswordUtil.hashPassword("secret1");
        String h2 = PasswordUtil.hashPassword("secret1");
        check("hash is salted (same password -> different hash)", !h1.equals(h2));
        check("correct password verifies", PasswordUtil.verifyPassword("secret1", h1));
        check("wrong password rejected", !PasswordUtil.verifyPassword("secret2", h1));
        check("null / garbage / truncated hashes rejected",
                !PasswordUtil.verifyPassword("x", null) && !PasswordUtil.verifyPassword("x", "plaintext")
                        && !PasswordUtil.verifyPassword("x", "pbkdf2$abc$def$ghi") && !PasswordUtil.verifyPassword("x", "pbkdf2$1$2"));
        check("empty password cannot be hashed", throwsA(IllegalArgumentException.class, () -> PasswordUtil.hashPassword("")));
    }

    // ---------------------------------------------------------------- 2. seed data credentials

    private static void seedDataChecks() throws Exception {
        System.out.println("[2] Seed users in database/sample_data.sql");
        Path sql = Path.of("database", "sample_data.sql");
        if (!Files.exists(sql)) {
            check("sample_data.sql found (run from the project root)", false);
            return;
        }
        Pattern row = Pattern.compile("\\((\\d+),\\s*'([^']+)',\\s*'(pbkdf2\\$[^']+)'.*?'(ADMIN|LECTURER|TECHNICAL_OFFICER|STUDENT)'");
        int users = 0;
        int bad = 0;
        for (String line : Files.readAllLines(sql)) {
            Matcher m = row.matcher(line);
            if (!m.find()) {
                continue;
            }
            users++;
            String password = switch (Role.fromDatabaseValue(m.group(4))) {
                case ADMIN -> "admin123";
                case LECTURER -> "lecturer123";
                case TECHNICAL_OFFICER -> "officer123";
                case STUDENT -> "student123";
            };
            if (!PasswordUtil.verifyPassword(password, m.group(3))
                    || PasswordUtil.verifyPassword("wrong-" + password, m.group(3))) {
                bad++;
                System.out.println("        bad hash for " + m.group(2));
            }
        }
        check("found the 30 seeded users (found " + users + ")", users == 30);
        check("every seeded hash matches the documented role password and rejects a wrong one", bad == 0);
    }

    // ---------------------------------------------------------------- 3. AuthService (fake DAO = polymorphism)

    /** A UserDAO that never touches MySQL: overrides the lookup (polymorphism). */
    private static final class FakeUserDAO extends UserDAO {
        private final User user;
        private final boolean dbDown;

        FakeUserDAO(User user, boolean dbDown) {
            this.user = user;
            this.dbDown = dbDown;
        }

        @Override
        public User findByUsername(String username) throws DatabaseException {
            if (dbDown) {
                throw new DatabaseException("Cannot connect to the database.");
            }
            return user != null && user.getUsername().equals(username) ? user : null;
        }
    }

    private static void authServiceChecks() {
        System.out.println("[3] AuthService");
        Session.end();
        User lec = newUser(Role.LECTURER);
        lec.setPasswordHash(PasswordUtil.hashPassword("lecturer123"));
        AuthService auth = new AuthService(new FakeUserDAO(lec, false));

        check("blank username -> ValidationException", throwsA(ValidationException.class, () -> auth.login("  ", "x")));
        check("empty password -> ValidationException", throwsA(ValidationException.class, () -> auth.login("test.lecturer", "")));
        check("unknown user -> AuthenticationException", throwsA(AuthenticationException.class, () -> auth.login("nobody", "lecturer123")));
        check("wrong password -> AuthenticationException", throwsA(AuthenticationException.class, () -> auth.login("test.lecturer", "nope")));
        check("failed logins leave no session", !Session.isLoggedIn());

        String m1 = messageOf(() -> auth.login("nobody", "lecturer123"));
        String m2 = messageOf(() -> auth.login("test.lecturer", "nope"));
        check("same message for unknown user and wrong password (no username guessing)", m1 != null && m1.equals(m2));

        User ok = null;
        try {
            ok = auth.login("  test.lecturer ", "lecturer123");
        } catch (Exception e) {
            System.out.println("        unexpected: " + e);
        }
        check("valid login returns the Lecturer subclass", ok instanceof Lecturer && ok.getRole() == Role.LECTURER);
        check("valid login starts the session", Session.isLoggedIn() && Session.getCurrentUser() == ok && Session.hasRole(Role.LECTURER));
        auth.logout();
        check("logout clears the session", !Session.isLoggedIn() && Session.getCurrentUser() == null);

        lec.setActive(false);
        check("deactivated account cannot log in", throwsA(AuthenticationException.class, () -> auth.login("test.lecturer", "lecturer123")));
        check("deactivated login leaves no session", !Session.isLoggedIn());

        AuthService down = new AuthService(new FakeUserDAO(lec, true));
        check("database failure surfaces as DatabaseException", throwsA(DatabaseException.class, () -> down.login("test.lecturer", "x")));
        Session.end();
    }

    private static String messageOf(Thrower t) {
        try {
            t.run();
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    // ---------------------------------------------------------------- 4. Session / Role

    private static void sessionAndRoleChecks() {
        System.out.println("[4] Session and roles");
        Session.end();
        check("logged out: requireLogin denied", throwsA(AuthorizationException.class, Session::requireLogin));
        check("logged out: requireRole denied", throwsA(AuthorizationException.class, () -> Session.requireRole(Role.ADMIN)));
        for (Role role : Role.values()) {
            Session.start(newUser(role));
            boolean exact = true;
            for (Role other : Role.values()) {
                exact &= Session.hasRole(other) == (other == role);
            }
            check(role + ": hasRole true only for its own role", exact);
            check(role + ": requireAdmin " + (role == Role.ADMIN ? "allowed" : "denied"),
                    role == Role.ADMIN ? !throwsA(AuthorizationException.class, Session::requireAdmin)
                            : throwsA(AuthorizationException.class, Session::requireAdmin));
        }
        check("Role.fromDatabaseValue maps all four stored values",
                Role.fromDatabaseValue("TECHNICAL_OFFICER") == Role.TECHNICAL_OFFICER && Role.fromString("Technical Officer") == Role.TECHNICAL_OFFICER);
        check("unknown role value rejected", throwsA(IllegalArgumentException.class, () -> Role.fromDatabaseValue("DEAN")));
        Session.end();
    }

    // ---------------------------------------------------------------- 5. service-level permission checks (reflection)

    /** Methods any logged-in user may call (read access shared with other modules, own-profile update). */
    private static final Set<String> LOGIN_ONLY = Set.of(
            "UserService.getUsersByRole", "UserService.getUserById", "UserService.updateOwnProfile",
            "DepartmentService.getAllDepartments", "DepartmentService.searchDepartments",
            "DepartmentService.getDepartmentById", "DepartmentService.getDepartmentHeads",
            "CourseService.getAllCourses", "CourseService.searchCourses", "CourseService.getCoursesByDepartment",
            "CourseService.getCoursesByLecturer", "CourseService.getCourseById", "CourseService.getCourseByCode",
            "NoticeService.getAllNotices", "NoticeService.searchNotices", "NoticeService.getNoticeById",
            "TimeTableService.getTimeTable", "TimeTableService.searchTimeTable", "TimeTableService.getSlotById");

    private static Object defaultFor(Class<?> t) {
        if (t == int.class) {
            return 0;
        }
        if (t == long.class) {
            return 0L;
        }
        if (t == boolean.class) {
            return false;
        }
        if (t == double.class) {
            return 0d;
        }
        return null;
    }

    /** Outcome of calling a service method with blank arguments: true if it was refused by the permission check. */
    private static boolean denied(Object service, Method m) throws Exception {
        Object[] args = new Object[m.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = defaultFor(m.getParameterTypes()[i]);
        }
        try {
            m.invoke(service, args);
            return false;
        } catch (InvocationTargetException e) {
            return e.getCause() instanceof AuthorizationException;
        }
    }

    private static void servicePermissionChecks() throws Exception {
        System.out.println("[5] Service-level permission checks (every public service method)");
        String[] services = {"UserService", "DepartmentService", "CourseService", "ReportService",
                "NoticeService", "TimeTableService", "AdminService"};
        int methods = 0;
        List<String> problems = new ArrayList<>();
        for (String name : services) {
            Class<?> type = Class.forName("com.facultyams.service." + name);
            Object service = type.getDeclaredConstructor().newInstance();
            for (Method m : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                methods++;
                String id = name + "." + m.getName();
                boolean loginOnly = LOGIN_ONLY.contains(id);

                Session.end();
                if (!denied(service, m)) {
                    problems.add(id + " allowed while logged out");
                }
                for (Role role : new Role[] {Role.LECTURER, Role.TECHNICAL_OFFICER, Role.STUDENT}) {
                    Session.start(newUser(role));
                    boolean d = denied(service, m);
                    if (loginOnly && d) {
                        problems.add(id + " wrongly denied for " + role);
                    }
                    if (!loginOnly && !d) {
                        problems.add(id + " NOT admin-only: allowed for " + role);
                    }
                }
                Session.start(newUser(Role.ADMIN));
                if (denied(service, m)) {
                    problems.add(id + " wrongly denied for ADMIN");
                }
            }
        }
        Session.end();
        problems.forEach(p -> System.out.println("        " + p));
        check(methods + " public service methods: logged-out denied, non-admin limited to read/own-profile, admin allowed",
                problems.isEmpty());
    }

    /** Read methods open to every logged-in user must not hand out stored password hashes to non-admins. */
    private static void hashExposureChecks() throws Exception {
        System.out.println("[5b] Password hashes are not exposed to non-admin callers");
        UserDAO dao = new UserDAO() {
            @Override
            public User findById(int userId) {
                return withHash();
            }

            @Override
            public List<User> findByRole(Role role) {
                List<User> list = new ArrayList<>();
                list.add(withHash());
                return list;
            }

            private User withHash() {
                User u = newUser(Role.LECTURER);
                u.setPasswordHash("pbkdf2$65536$salt$hash");
                return u;
            }
        };
        UserService users = new UserService(dao, new DepartmentDAO());
        Session.start(newUser(Role.STUDENT));
        check("getUserById: student gets the user without the hash", users.getUserById(5).getPasswordHash() == null);
        check("getUsersByRole: student gets users without hashes", users.getUsersByRole(Role.LECTURER).get(0).getPasswordHash() == null);
        Session.start(newUser(Role.ADMIN));
        check("getUserById: admin still receives the full record", users.getUserById(5).getPasswordHash() != null);
        Session.end();
    }

    // ---------------------------------------------------------------- 6. shell access (headless: panels only)

    private static void collect(Component c, List<String> texts) {
        if (c instanceof javax.swing.JLabel l && l.getText() != null) {
            texts.add(l.getText());
        }
        if (c instanceof javax.swing.AbstractButton b && b.getText() != null) {
            texts.add(b.getText());
        }
        if (c instanceof Container k) {
            for (Component child : k.getComponents()) {
                collect(child, texts);
            }
        }
    }

    private static void shellAccessChecks() {
        System.out.println("[6] Application shell and role guard");
        User student = newUser(Role.STUDENT);
        Session.start(student);
        check("AdminShell refuses a Student session", throwsA(IllegalStateException.class, () -> new AdminShell(null, student)));
        Session.end();
        User admin = newUser(Role.ADMIN);
        check("AdminShell refuses when nobody is logged in", throwsA(IllegalStateException.class, () -> new AdminShell(null, admin)));
        check("AdminShell refuses a null user", throwsA(IllegalStateException.class, () -> new AdminShell(null, null)));
        Session.start(admin);
        User other = newUser(Role.ADMIN);
        check("AdminShell refuses a user that is not the session user", throwsA(IllegalStateException.class, () -> new AdminShell(null, other)));

        for (Role role : new Role[] {Role.LECTURER, Role.TECHNICAL_OFFICER, Role.STUDENT}) {
            User u = newUser(role);
            Session.start(u);
            try {
                PlaceholderShell shell = new PlaceholderShell(null, u, role.getDisplayName() + " module");
                List<String> texts = new ArrayList<>();
                collect(shell, texts);
                check(role + " shell shows role badge, welcome text, Dashboard and Logout",
                        texts.contains(role.getDisplayName()) && texts.contains("Welcome, Test " + role.getDisplayName())
                                && texts.contains("Dashboard") && texts.contains("Logout"));
                check(role + " shell has no admin navigation",
                        texts.stream().noneMatch(t -> t.contains("User Management") || t.contains("Settings")));
            } catch (RuntimeException e) {
                check(role + " shell builds (" + e + ")", false);
            }
        }
        Session.end();
    }

    // ---------------------------------------------------------------- 7. connection reuse (fake JDBC driver)

    private static final class FakeState {
        boolean valid = true;
        boolean closed;
        boolean autoCommit = true;
        int rollbacks;
    }

    private static final List<FakeState> STATES = new ArrayList<>();
    private static final AtomicInteger OPENS = new AtomicInteger();
    private static volatile boolean failConnect;

    private static final class FakeDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            if (failConnect) {
                throw new SQLException("Connection refused");
            }
            FakeState st = new FakeState();
            synchronized (STATES) {
                STATES.add(st);
            }
            OPENS.incrementAndGet();
            return (Connection) Proxy.newProxyInstance(CoreChecks.class.getClassLoader(), new Class<?>[] {Connection.class},
                    (proxy, method, a) -> {
                        switch (method.getName()) {
                            case "close" -> st.closed = true;
                            case "isClosed" -> {
                                return st.closed;
                            }
                            case "isValid" -> {
                                return st.valid && !st.closed;
                            }
                            case "getAutoCommit" -> {
                                return st.autoCommit;
                            }
                            case "setAutoCommit" -> st.autoCommit = (Boolean) a[0];
                            case "rollback" -> st.rollbacks++;
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "equals" -> {
                                return proxy == a[0];
                            }
                            case "toString" -> {
                                return "FakeConnection";
                            }
                            default -> {
                                return null;
                            }
                        }
                        return null;
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fakeams:");
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
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }

    private static int closedCount() {
        synchronized (STATES) {
            return (int) STATES.stream().filter(s -> s.closed).count();
        }
    }

    private static void poolChecks() throws Exception {
        System.out.println("[7] DatabaseConnection reuse (fake JDBC driver)");
        String url = System.getenv("FOTAMS_DB_URL");
        if (url == null || !url.startsWith("jdbc:fakeams:")) {
            check("run with FOTAMS_DB_URL=jdbc:fakeams:x and FOTAMS_DB_USER=checks", false);
            return;
        }
        DriverManager.registerDriver(new FakeDriver());

        int base = OPENS.get();
        try (Connection c = DatabaseConnection.getConnection()) {
            check("first call opens a real connection", OPENS.get() == base + 1 && !c.isClosed());
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            check("second call after close reuses it (no new open)", OPENS.get() == base + 1 && !c.isClosed());
        }

        Connection a = DatabaseConnection.getConnection();
        Connection b = DatabaseConnection.getConnection();
        check("two connections held at once need two real ones", OPENS.get() == base + 2 && a != b);
        a.close();
        check("closed handle reports closed", a.isClosed());
        check("closed handle cannot be used", throwsA(SQLException.class, a::getAutoCommit));
        a.close();
        int before = OPENS.get();
        Connection x = DatabaseConnection.getConnection();
        Connection y = DatabaseConnection.getConnection();
        check("double close does not put the same connection in the pool twice", OPENS.get() == before + 1);
        x.close();
        y.close();
        b.close();

        Connection t = DatabaseConnection.getConnection();
        t.setAutoCommit(false);
        t.close();
        int rollbacks;
        synchronized (STATES) {
            rollbacks = STATES.stream().mapToInt(s -> s.rollbacks).sum();
        }
        try (Connection n = DatabaseConnection.getConnection()) {
            check("open transaction is rolled back and autocommit restored on close", rollbacks == 1 && n.getAutoCommit());
        }

        synchronized (STATES) {
            STATES.forEach(s -> s.valid = false);
        }
        int opensBefore = OPENS.get();
        int closedBefore = closedCount();
        try (Connection n = DatabaseConnection.getConnection()) {
            check("dead idle connections are discarded and a fresh one is opened",
                    OPENS.get() > opensBefore && closedCount() > closedBefore && n.isValid(1));
        }

        DatabaseConnection.closeAll();
        int open0 = OPENS.get();
        int closed0 = closedCount();
        List<Connection> held = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            held.add(DatabaseConnection.getConnection());
        }
        for (Connection c : held) {
            c.close();
        }
        check("pool keeps at most 4 idle connections (6 opened, 2 closed)", OPENS.get() - open0 == 6 && closedCount() - closed0 == 2);
        DatabaseConnection.closeAll();
        check("closeAll closes the remaining idle connections", closedCount() - closed0 == 6);

        AtomicInteger errors = new AtomicInteger();
        Thread[] threads = new Thread[8];
        int opens1 = OPENS.get();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int k = 0; k < 100; k++) {
                    try (Connection c = DatabaseConnection.getConnection()) {
                        if (c.isClosed()) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
            threads[i].start();
        }
        for (Thread th : threads) {
            th.join();
        }
        check("8 threads x 100 borrow/return: no errors, at most 8 real connections", errors.get() == 0 && OPENS.get() - opens1 <= 8);
        check("testConnection() succeeds through the pool", DatabaseConnection.testConnection());

        DatabaseConnection.closeAll();
        failConnect = true;
        String msg = messageOf(DatabaseConnection::getConnection);
        check("unreachable database -> DatabaseException with a clear message",
                throwsA(DatabaseException.class, DatabaseConnection::getConnection) && msg != null && msg.startsWith("Cannot connect to the database"));
        check("testConnection() returns false when the database is down", !DatabaseConnection.testConnection());
        failConnect = false;
        DatabaseConnection.closeAll();
    }
}