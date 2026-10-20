package com.facultyams;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.NoticeDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Admin;
import com.facultyams.model.Student;
import com.facultyams.model.User;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.service.AdminService;
import com.facultyams.service.UserService;
import com.facultyams.ui.DashboardRouter;
import com.facultyams.ui.admin.AdminShell;
import com.facultyams.ui.admin.DashboardPanel;
import com.facultyams.ui.admin.ProfilePanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Phase 6 checks: Admin dashboard and Admin profile. No MySQL server and no JUnit are needed.
 *
 * <pre>
 *   java -cp target/classes com.facultyams.AdminDashboardChecks
 *   java -Drender.dir=/tmp/shots -cp target/classes com.facultyams.AdminDashboardChecks   (also saves PNGs)
 * </pre>
 *
 * Part A runs here with fake DAOs injected into the real services and panels (service rules, dashboard
 * behaviour, profile behaviour, concurrency). Part B re-launches this class in a child JVM with a fake JDBC
 * driver ({@code FOTAMS_DB_URL=jdbc:fakeadmin:test}) so the real DAOs, SQL, Admin shell, navigation and
 * database-failure handling run end to end. The exit code is 0 only if every check passed.
 */
public final class AdminDashboardChecks {

    private static int passed;
    private static int failed;

    private AdminDashboardChecks() {
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0 && "jdbc".equals(args[0])) {
            partB();
            System.out.println("\nRESULT (part B): " + passed + " passed, " + failed + " failed");
            System.exit(failed == 0 ? 0 : 1);
        }
        partA();
        System.out.println("\nRESULT (part A): " + passed + " passed, " + failed + " failed");
        int childExit = runJdbcPart();
        System.out.println(childExit == 0 ? "\nPart B passed." : "\nPart B FAILED (exit " + childExit + ").");
        System.exit(failed == 0 && childExit == 0 ? 0 : 1);
    }

    private static int runJdbcPart() throws Exception {
        System.out.println("\n[B] Re-launching with a fake JDBC driver (real DAOs, SQL, shell and panels)");
        String java = System.getProperty("java.home") + "/bin/java";
        List<String> cmd = new ArrayList<>(List.of(java, "-Djava.awt.headless=true"));
        if (System.getProperty("render.dir") != null) {
            cmd.add("-Drender.dir=" + System.getProperty("render.dir"));
        }
        cmd.addAll(List.of("-cp", System.getProperty("java.class.path"), AdminDashboardChecks.class.getName(), "jdbc"));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("FOTAMS_DB_URL", "jdbc:fakeadmin:test");
        pb.inheritIO();
        return pb.start().waitFor();
    }

    // ============================================================================= helpers

    private static Admin admin(int id, String name) {
        Admin a = new Admin();
        a.setUserId(id);
        a.setUsername("admin" + id);
        a.setFullName(name);
        a.setEmail("admin" + id + "@fot.edu");
        a.setContactNumber("0771234567");
        a.setActive(true);
        a.setPasswordHash(PasswordUtil.hashPassword("OldPass#1"));
        return a;
    }

    private static Student student(int id) {
        Student s = new Student();
        s.setUserId(id);
        s.setUsername("stu" + id);
        s.setFullName("Sam Student");
        s.setEmail("sam@fot.edu");
        s.setActive(true);
        return s;
    }

    private static Component find(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container child) {
                Component hit = find(child, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                out.add(type.cast(c));
            }
            if (c instanceof Container child) {
                out.addAll(all(child, type));
            }
        }
        return out;
    }

    private static String value(Container root, String card) {
        return ((JLabel) find(root, "card." + card + ".value")).getText();
    }

    private static String fieldText(Container root, String name) {
        return ((javax.swing.text.JTextComponent) find(root, name)).getText();
    }

    /** True when the component exists and neither it nor any ancestor below the root is hidden. */
    private static boolean visible(Container root, String name) {
        Component c = find(root, name);
        while (c != null && c != root) {
            if (!c.isVisible()) {
                return false;
            }
            c = c.getParent();
        }
        return c == root;
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> job) throws Exception {
        Object[] box = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                box[0] = job.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        @SuppressWarnings("unchecked")
        T result = (T) box[0];
        return result;
    }

    /** Waits (pumping the Swing thread) until the dashboard has finished loading. */
    private static void awaitLoaded(DashboardPanel panel) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            if (!onEdt(panel::isLoading)) {
                return;
            }
            Thread.sleep(10);
        }
        throw new IllegalStateException("dashboard still loading after 5 s");
    }

    /** Runs a job while capturing what is written to System.err. */
    private static String captureErr(Runnable job) {
        PrintStream old = System.err;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buf, true));
        try {
            job.run();
        } finally {
            System.setErr(old);
        }
        return buf.toString();
    }

    // ============================================================================= fakes (part A)

    private static class FakeUserDAO extends UserDAO {
        Map<Role, Integer> counts = roles(2, 5, 3, 21);
        DatabaseException fail;
        CountDownLatch gate;          // when set, the next countByRole blocks until released
        java.util.function.Supplier<User> row;
        User updatedProfile;
        Integer passwordUserId;
        String passwordHash;

        @Override
        public Map<Role, Integer> countByRole() throws DatabaseException {
            if (fail != null) {
                throw fail;
            }
            Map<Role, Integer> snapshot = new EnumMap<>(counts);
            CountDownLatch g = gate;
            if (g != null) {
                gate = null;
                try {
                    g.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return snapshot;
        }

        @Override
        public User findById(int userId) throws DatabaseException {
            if (fail != null) {
                throw fail;
            }
            return row == null ? null : row.get();
        }

        boolean emailTaken;

        @Override
        public boolean emailExists(String email, int excludeUserId) {
            return emailTaken;
        }

        @Override
        public boolean usernameExists(String username, int excludeUserId) {
            return false;
        }

        @Override
        public void updateProfile(User user) {
            updatedProfile = user;
        }

        @Override
        public void updatePassword(int userId, String hash) {
            passwordUserId = userId;
            passwordHash = hash;
        }
    }

    private static Map<Role, Integer> roles(int admins, int lecturers, int officers, int students) {
        Map<Role, Integer> m = new EnumMap<>(Role.class);
        m.put(Role.ADMIN, admins);
        m.put(Role.LECTURER, lecturers);
        m.put(Role.TECHNICAL_OFFICER, officers);
        m.put(Role.STUDENT, students);
        return m;
    }

    private static final class FakeDepartmentDAO extends DepartmentDAO {
        int count = 4;
        DatabaseException fail;

        @Override
        public int countAll() throws DatabaseException {
            if (fail != null) {
                throw fail;
            }
            return count;
        }
    }

    private static final class FakeCourseDAO extends CourseDAO {
        int count = 9;
        DatabaseException fail;

        @Override
        public int countAll() throws DatabaseException {
            if (fail != null) {
                throw fail;
            }
            return count;
        }
    }

    private static final class FakeNoticeDAO extends NoticeDAO {
        int count = 6;
        DatabaseException fail;

        @Override
        public int countAll() throws DatabaseException {
            if (fail != null) {
                throw fail;
            }
            return count;
        }
    }

    // ============================================================================= part A

    private static void partA() throws Exception {
        System.out.println("=== PART A: services and panels with injected fake DAOs ===");
        serviceChecks();
        dashboardChecks();
        profileChecks();
        logoutChecks();
    }

    private static void serviceChecks() throws Exception {
        System.out.println("\n[A1] AdminService counts and permissions");
        FakeUserDAO users = new FakeUserDAO();
        FakeDepartmentDAO departments = new FakeDepartmentDAO();
        FakeCourseDAO courses = new FakeCourseDAO();
        FakeNoticeDAO notices = new FakeNoticeDAO();
        AdminService service = new AdminService(users, departments, courses, notices);

        Session.start(admin(1, "Ada Admin"));
        check("countUsers adds up every role (2+5+3+21 = 31)", service.countUsers() == 31);
        check("countDepartments comes from the department DAO (4)", service.countDepartments() == 4);
        check("countCourses comes from the course DAO (9)", service.countCourses() == 9);
        check("countNotices comes from the notice DAO (6)", service.countNotices() == 6);
        notices.count = 7;
        users.counts = roles(1, 1, 1, 1);
        check("counts are read fresh on every call (no caching): notices 7, users 4",
                service.countNotices() == 7 && service.countUsers() == 4);
        check("getDashboardStatistics keeps its six existing entries",
                service.getDashboardStatistics().size() == 6);

        for (User denied : List.of(student(5), lecturer())) {
            Session.start(denied);
            String who = denied.getRole().getDisplayName();
            check(who + " is refused all four counts",
                    refused(service::countUsers) && refused(service::countDepartments)
                            && refused(service::countCourses) && refused(service::countNotices));
        }
        Session.end();
        check("nobody logged in: all four counts are refused",
                refused(service::countUsers) && refused(service::countDepartments)
                        && refused(service::countCourses) && refused(service::countNotices));

        Session.start(admin(1, "Ada Admin"));
        notices.fail = new DatabaseException("The notices table is missing.", null);
        boolean propagated = false;
        try {
            service.countNotices();
        } catch (DatabaseException e) {
            propagated = true;
        }
        check("a database failure is passed up as DatabaseException (not swallowed or turned into 0)", propagated);
        Session.end();

        System.out.println("\n[A2] UserService.changeOwnPassword");
        FakeUserDAO dao = new FakeUserDAO();
        dao.row = () -> admin(1, "Ada Admin");
        UserService userService = new UserService(dao, new FakeDepartmentDAO());
        Session.start(admin(1, "Ada Admin"));
        check("wrong current password is rejected and nothing is saved",
                rejected(() -> userService.changeOwnPassword("nope", "NewPass#9")) && dao.passwordHash == null);
        check("blank current password is rejected",
                rejected(() -> userService.changeOwnPassword("", "NewPass#9")) && dao.passwordHash == null);
        check("weak new password is rejected and nothing is saved",
                rejected(() -> userService.changeOwnPassword("OldPass#1", "abc")) && dao.passwordHash == null);
        check("new password equal to the current one is rejected",
                rejected(() -> userService.changeOwnPassword("OldPass#1", "OldPass#1")) && dao.passwordHash == null);
        userService.changeOwnPassword("OldPass#1", "NewPass#9");
        check("correct current + valid new password saves a hash for the logged-in user only",
                dao.passwordUserId != null && dao.passwordUserId == 1);
        check("the saved value is a hash that verifies the new password, not the plain text",
                dao.passwordHash != null && !dao.passwordHash.equals("NewPass#9")
                        && PasswordUtil.verifyPassword("NewPass#9", dao.passwordHash));
        Session.start(student(5));
        boolean studentRefused = false;
        try {
            userService.changeOwnPassword("x", "NewPass#9");
        } catch (AuthorizationException e) {
            studentRefused = true;
        } catch (Exception e) {
            // wrong exception type
        }
        check("a Student cannot use changeOwnPassword (passwords are not self-editable for other roles)", studentRefused);
        Session.end();
    }

    private static User lecturer() {
        com.facultyams.model.Lecturer l = new com.facultyams.model.Lecturer();
        l.setUserId(7);
        l.setUsername("lec7");
        l.setFullName("Lee Lecturer");
        l.setActive(true);
        return l;
    }

    private interface Call {
        void run() throws Exception;
    }

    private interface IntCall {
        int get() throws Exception;
    }

    private static boolean refused(IntCall c) {
        try {
            c.get();
            return false;
        } catch (AuthorizationException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean rejected(Call c) {
        try {
            c.run();
            return false;
        } catch (com.facultyams.exception.ValidationException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void dashboardChecks() throws Exception {
        System.out.println("\n[A3] DashboardPanel with fake DAOs");
        FakeUserDAO users = new FakeUserDAO();
        FakeDepartmentDAO departments = new FakeDepartmentDAO();
        FakeCourseDAO courses = new FakeCourseDAO();
        FakeNoticeDAO notices = new FakeNoticeDAO();
        AdminService service = new AdminService(users, departments, courses, notices);
        Session.start(admin(1, "Ada Admin"));

        List<String> opened = new ArrayList<>();
        DashboardPanel panel = new DashboardPanel(opened::add, service);
        layoutAll(panel, 900, 500);

        panel.refresh();
        check("while loading every card shows '...' and isLoading() is true",
                onEdt(panel::isLoading) && "...".equals(value(panel, "users")) && "...".equals(value(panel, "notices")));
        awaitLoaded(panel);
        check("cards show the database figures: users 31, departments 4, courses 9, notices 6",
                "31".equals(value(panel, "users")) && "4".equals(value(panel, "departments"))
                        && "9".equals(value(panel, "courses")) && "6".equals(value(panel, "notices")));
        check("no error message or Retry button when everything loaded",
                !visible(panel, "dashboard.error") && !visible(panel, "dashboard.retry"));

        users.counts = roles(3, 8, 4, 40);
        departments.count = 12;
        courses.count = 30;
        notices.count = 2;
        panel.refresh();
        awaitLoaded(panel);
        check("changing the data and refreshing changes the cards (users 55, depts 12, courses 30, notices 2)",
                "55".equals(value(panel, "users")) && "12".equals(value(panel, "departments"))
                        && "30".equals(value(panel, "courses")) && "2".equals(value(panel, "notices")));

        // one table unreadable: the others must still show real numbers
        notices.fail = new DatabaseException("The notices table is missing. Run database/migrations/001_notices_timetables.sql first.", null);
        String err = captureErr(() -> {
            try {
                panel.refresh();
                awaitLoaded(panel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("notices failing: the other three cards keep their real numbers (55 / 12 / 30)",
                "55".equals(value(panel, "users")) && "12".equals(value(panel, "departments"))
                        && "30".equals(value(panel, "courses")));
        check("notices failing: its card shows '-' (not a fake number)", "-".equals(value(panel, "notices")));
        check("notices failing: message names Notices and the migration, shown inline",
                visible(panel, "dashboard.error") && fieldText(panel, "dashboard.error").contains("Notices:")
                        && fieldText(panel, "dashboard.error").contains("001_notices_timetables.sql"));
        check("notices failing: Retry button is visible", visible(panel, "dashboard.retry"));
        check("notices failing: error is logged to stderr for debugging", err.contains("Database error"));

        notices.fail = null;
        notices.count = 5;
        ((JButton) find(panel, "dashboard.retry")).doClick();
        awaitLoaded(panel);
        check("clicking Retry after the problem is fixed shows notices 5 and hides the message",
                "5".equals(value(panel, "notices")) && !visible(panel, "dashboard.error"));

        // everything down
        DatabaseException down = new DatabaseException("Cannot connect to the database. Check that MySQL is running.", null);
        users.fail = down;
        departments.fail = down;
        courses.fail = down;
        notices.fail = down;
        captureErr(() -> {
            try {
                panel.refresh();
                awaitLoaded(panel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("database down: all four cards show '-'",
                "-".equals(value(panel, "users")) && "-".equals(value(panel, "departments"))
                        && "-".equals(value(panel, "courses")) && "-".equals(value(panel, "notices")));
        String msg = fieldText(panel, "dashboard.error");
        check("database down: one message (the reason appears once, not four times)",
                msg.contains("Cannot connect") && msg.indexOf("Cannot connect") == msg.lastIndexOf("Cannot connect"));
        check("database down: panel is not left on '...' and is no longer loading", !onEdt(panel::isLoading));
        layoutAll(panel, 900, 500);
        Component errorBox = find(panel, "dashboard.error");
        Component firstQuick = find(panel, "quick.users");
        int errorBottom = SwingUtilities.convertPoint(errorBox, 0, errorBox.getHeight(), panel).y;
        int quickTop = SwingUtilities.convertPoint(firstQuick, 0, 0, panel).y;
        int quickBottom = SwingUtilities.convertPoint(firstQuick, 0, firstQuick.getHeight(), panel).y;
        check("database down: the wrapped message does not push Quick Actions away (below the message, inside the page)",
                quickTop >= errorBottom && quickBottom <= 500);

        // session lost while the dashboard is open
        users.fail = departments.fail = courses.fail = notices.fail = null;
        Session.end();
        panel.refresh();
        awaitLoaded(panel);
        check("logged out: cards show '-' and a message instead of crashing or showing numbers",
                "-".equals(value(panel, "users")) && visible(panel, "dashboard.error"));
        Session.start(admin(1, "Ada Admin"));

        // out-of-date load must not overwrite a newer one
        users.counts = roles(1, 1, 1, 1);          // total 4, held back by the gate
        users.gate = new CountDownLatch(1);
        CountDownLatch gate = users.gate;
        panel.refresh();                              // slow load: will read 4 and then wait
        Thread.sleep(150);
        users.counts = roles(10, 10, 10, 10);        // total 40
        panel.refresh();                              // fast load
        awaitLoaded(panel);
        gate.countDown();                             // now let the old load finish
        Thread.sleep(300);
        onEdt(() -> null);
        check("a slow older load that finishes late does not overwrite the newer figures (shows 40, not 4)",
                "40".equals(value(panel, "users")));

        // quick actions
        String[][] expected = {
                {"users", "User Management"}, {"departments", "Department Management"},
                {"courses", "Course Management"}, {"notices", "Notice Management"},
                {"timetables", "Timetable Management"}};
        List<JButton> quick = new ArrayList<>();
        for (JButton b : all(panel, JButton.class)) {
            if (b.getName() != null && b.getName().startsWith("quick.")) {
                quick.add(b);
            }
        }
        check("exactly five quick actions are shown", quick.size() == 5);
        for (String[] e : expected) {
            JButton b = (JButton) find(panel, "quick." + e[0]);
            opened.clear();
            if (b != null) {
                b.doClick();
            }
            check("quick action '" + e[1] + "' is enabled and opens page '" + e[0] + "'",
                    b != null && b.isEnabled() && e[1].equals(b.getAccessibleContext().getAccessibleName())
                            && opened.equals(List.of(e[0])));
        }
        Session.end();
    }

    private static void profileChecks() throws Exception {
        System.out.println("\n[A4] ProfilePanel with a fake UserDAO");
        FakeUserDAO dao = new FakeUserDAO();
        dao.row = () -> {
            Admin a = admin(1, "Ada From Database");
            a.setEmail("ada.db@fot.edu");
            a.setContactNumber("0711111111");
            a.setDepartmentName("ICT");
            return a;
        };
        UserService userService = new UserService(dao, new FakeDepartmentDAO());
        Session.start(admin(1, "Ada Stale Session"));   // the session copy is out of date on purpose
        int[] changed = {0};
        int[] loggedOut = {0};
        ProfilePanel panel = new ProfilePanel(() -> changed[0]++, () -> loggedOut[0]++, userService);
        layoutAll(panel, 800, 600);
        panel.refresh();

        check("shows the details from the database, not the stale session copy",
                "Ada From Database".equals(fieldText(panel, "profile.name"))
                        && "ada.db@fot.edu".equals(fieldText(panel, "profile.email")));
        check("shows username, role and department",
                "admin1".equals(fieldText(panel, "profile.username"))
                        && "Administrator".equals(fieldText(panel, "profile.role"))
                        && "ICT".equals(fieldText(panel, "profile.department")));
        check("username, role and department are read-only",
                !((JTextField) find(panel, "profile.username")).isEditable()
                        && !((JTextField) find(panel, "profile.role")).isEditable()
                        && !((JTextField) find(panel, "profile.department")).isEditable());
        check("name, email and contact number are editable for an Admin",
                ((JTextField) find(panel, "profile.name")).isEditable()
                        && ((JTextField) find(panel, "profile.email")).isEditable()
                        && ((JTextField) find(panel, "profile.contact")).isEditable());
        check("no password hash or hash text is shown anywhere on the page",
                all(panel, JTextField.class).stream().noneMatch(f -> f.getText().contains("pbkdf2")
                        || f.getText().equals(Session.getCurrentUser().getPasswordHash())));

        ((JTextField) find(panel, "profile.email")).setText("not-an-email");
        String err = captureErr(() -> ((JButton) find(panel, "profile.update")).doClick());
        check("invalid email is refused with a validation message and nothing is saved",
                err.contains("ValidationException") && dao.updatedProfile == null && changed[0] == 0);

        ((JTextField) find(panel, "profile.name")).setText("Ada Renamed");
        ((JTextField) find(panel, "profile.email")).setText("taken@fot.edu");
        dao.emailTaken = true;
        err = captureErr(() -> ((JButton) find(panel, "profile.update")).doClick());
        check("an email that another account already uses is refused and nothing is saved",
                dao.updatedProfile == null && err.contains("ValidationException"));
        dao.emailTaken = false;
        ((JTextField) find(panel, "profile.email")).setText("ada.new@fot.edu");
        ((JTextField) find(panel, "profile.contact")).setText("0722222222");
        err = captureErr(() -> ((JButton) find(panel, "profile.update")).doClick());
        check("valid edit saves name, email and contact for the logged-in user",
                dao.updatedProfile != null && "Ada Renamed".equals(dao.updatedProfile.getFullName())
                        && "ada.new@fot.edu".equals(dao.updatedProfile.getEmail())
                        && "0722222222".equals(dao.updatedProfile.getContactNumber())
                        && dao.updatedProfile.getUserId() == 1);
        check("after saving the session user has the new name and the header callback ran",
                "Ada Renamed".equals(Session.getCurrentUser().getFullName()) && changed[0] == 1);
        check("success is reported to the user", err.contains("Profile updated"));

        // password change through the panel
        dao.passwordHash = null;
        ((JPasswordField) find(panel, "profile.currentPassword")).setText("wrong");
        ((JPasswordField) find(panel, "profile.newPassword")).setText("NewPass#9");
        ((JPasswordField) find(panel, "profile.confirmPassword")).setText("NewPass#9");
        err = captureErr(() -> ((JButton) find(panel, "profile.changePassword")).doClick());
        check("wrong current password: refused, nothing saved, message shown",
                dao.passwordHash == null && err.contains("current password is incorrect"));
        ((JPasswordField) find(panel, "profile.currentPassword")).setText("OldPass#1");
        ((JPasswordField) find(panel, "profile.confirmPassword")).setText("Different#9");
        err = captureErr(() -> ((JButton) find(panel, "profile.changePassword")).doClick());
        check("confirmation that does not match: refused, nothing saved",
                dao.passwordHash == null && err.contains("do not match"));
        ((JPasswordField) find(panel, "profile.confirmPassword")).setText("NewPass#9");
        captureErr(() -> ((JButton) find(panel, "profile.changePassword")).doClick());
        check("correct current password + matching new password is saved as a verifiable hash",
                dao.passwordHash != null && PasswordUtil.verifyPassword("NewPass#9", dao.passwordHash));
        check("password fields are cleared after a successful change",
                ((JPasswordField) find(panel, "profile.currentPassword")).getPassword().length == 0
                        && ((JPasswordField) find(panel, "profile.newPassword")).getPassword().length == 0
                        && ((JPasswordField) find(panel, "profile.confirmPassword")).getPassword().length == 0);

        // database unreachable while opening the page
        dao.fail = new DatabaseException("Cannot connect to the database. Check that MySQL is running.", null);
        Session.start(admin(1, "Ada Session Copy"));
        panel.refresh();
        check("database down: page still opens with the session details and shows an inline message",
                "Ada Session Copy".equals(fieldText(panel, "profile.name")) && visible(panel, "profile.error")
                        && fieldText(panel, "profile.error").contains("Cannot connect"));
        dao.fail = null;
        panel.refresh();
        check("database back: the inline message disappears", !visible(panel, "profile.error"));

        // Logout button
        ((JButton) find(panel, "profile.logout")).doClick();
        check("the Logout button on the profile page calls the shell's logout", loggedOut[0] == 1);
        ProfilePanel noLogout = new ProfilePanel(() -> { }, null, userService);
        check("without a logout handler the page shows no Logout button (nothing is wired to nothing)",
                find(noLogout, "profile.logout") == null);

        // a non-admin: only what the model allows
        System.out.println("\n[A5] ProfilePanel authorisation follows the model (Student)");
        dao.row = () -> {
            Student s = student(5);
            s.setContactNumber("0700000000");
            return s;
        };
        Session.start(student(5));
        ProfilePanel studentPanel = new ProfilePanel(() -> { }, null, userService);
        studentPanel.refresh();
        check("Student: name and email are read-only, contact number is editable",
                !((JTextField) find(studentPanel, "profile.name")).isEditable()
                        && !((JTextField) find(studentPanel, "profile.email")).isEditable()
                        && ((JTextField) find(studentPanel, "profile.contact")).isEditable());
        check("Student: change-photo allowed, password controls disabled",
                find(studentPanel, "profile.photo").isEnabled()
                        && !find(studentPanel, "profile.changePassword").isEnabled()
                        && !find(studentPanel, "profile.newPassword").isEnabled());
        Session.end();
    }

    private static void logoutChecks() throws Exception {
        System.out.println("\n[A6] Logout / session cleanup");
        FakeUserDAO users = new FakeUserDAO();
        AdminService service = new AdminService(users, new FakeDepartmentDAO(), new FakeCourseDAO(), new FakeNoticeDAO());
        Session.start(admin(1, "Ada Admin"));
        check("logged in: the dashboard figures can be read", service.countUsers() == 31);
        boolean windowFailed = false;
        try {
            DashboardRouter.logout(null);   // headless JVM: cannot build the login window, but must end the session first
        } catch (java.awt.HeadlessException e) {
            windowFailed = true;
        }
        check("DashboardRouter.logout ends the session before it touches any window", Session.getCurrentUser() == null);
        check("(headless only) building the login window is what failed, nothing else", windowFailed);
        check("after logout every admin figure is refused", refused(service::countUsers) && refused(service::countNotices));
        ProfilePanel panel = new ProfilePanel(() -> { }, () -> { }, new UserService(users, new FakeDepartmentDAO()));
        boolean quiet = true;
        try {
            panel.refresh();
        } catch (RuntimeException e) {
            quiet = false;
        }
        check("a profile page refreshed after logout does nothing instead of crashing", quiet);
    }

    // ============================================================================= layout / render helpers

    /** Several passes, like a live window: wrapped text only gets its final height after its width is known. */
    private static void layoutAll(Container c, int w, int h) {
        c.setSize(w, h);
        for (int pass = 0; pass < 3; pass++) {
            layoutTree(c);
        }
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container cc) {
                layoutTree(cc);
            }
        }
    }

    private static void render(Component c, int w, int h, File file) throws Exception {
        if (c instanceof Container cc) {
            layoutAll(cc, w, h);
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", file);
    }

    // ============================================================================= part B (child JVM, fake JDBC)

    private record Exec(String sql, TreeMap<Integer, Object> params) {
    }

    private static final List<Exec> EXEC = new ArrayList<>();
    private static volatile boolean dbDown;
    private static volatile boolean noticesMissing;
    private static final Map<String, Integer> TABLE = new LinkedHashMap<>();
    private static final Map<String, Object> USER_ROW = new LinkedHashMap<>();

    private static void partB() throws Exception {
        System.out.println("=== PART B: real DAOs, SQL, Admin shell and navigation over a fake JDBC driver ===");
        DriverManager.registerDriver(new FakeDriver());
        TABLE.put("admins", 2);
        TABLE.put("lecturers", 5);
        TABLE.put("technical_officers", 3);
        TABLE.put("students", 21);
        TABLE.put("departments", 4);
        TABLE.put("courses", 9);
        TABLE.put("notices", 6);
        USER_ROW.putAll(Map.ofEntries(
                Map.entry("role", "ADMIN"), Map.entry("user_id", 1), Map.entry("username", "admin1"),
                Map.entry("password_hash", PasswordUtil.hashPassword("OldPass#1")),
                Map.entry("full_name", "Ada Database"), Map.entry("email", "ada.db@fot.edu"),
                Map.entry("contact_number", "0711111111"), Map.entry("department_name", "ICT"),
                Map.entry("is_active", true)));

        Admin me = admin(1, "Ada Session");
        Session.start(me);
        AdminShell shell = new AdminShell(null, me);
        layoutAll(shell, 1200, 760);
        DashboardPanel dashboard = all(shell, DashboardPanel.class).get(0);
        awaitLoaded(dashboard);

        System.out.println("\n[B1] Dashboard figures come from SQL, through the real DAOs");
        check("dashboard is the first page", AdminShell.PAGE_DASHBOARD.equals(shell.getCurrentPage()));
        check("cards show the fake database's figures: users 31, departments 4, courses 9, notices 6",
                "31".equals(value(shell, "users")) && "4".equals(value(shell, "departments"))
                        && "9".equals(value(shell, "courses")) && "6".equals(value(shell, "notices")));
        List<String> sql = new ArrayList<>();
        EXEC.forEach(e -> sql.add(e.sql()));
        check("the four figures were read with the DAOs' own COUNT statements (users, departments, courses, notices)",
                sql.stream().anyMatch(s -> s.contains("FROM users GROUP BY role"))
                        && sql.contains("SELECT COUNT(*) FROM departments")
                        && sql.contains("SELECT COUNT(*) FROM courses")
                        && sql.contains("SELECT COUNT(*) FROM notices"));
        TABLE.put("notices", 8);
        TABLE.put("students", 25);
        SwingUtilities.invokeAndWait(dashboard::refresh);
        awaitLoaded(dashboard);
        check("changing the database and refreshing changes the cards (users 35, notices 8)",
                "35".equals(value(shell, "users")) && "8".equals(value(shell, "notices")));
        TABLE.put("notices", 6);
        TABLE.put("students", 21);

        System.out.println("\n[B2] Navigation");
        String[][] sidebar = {
                {"Dashboard", AdminShell.PAGE_DASHBOARD}, {"User Management", AdminShell.PAGE_USERS},
                {"Department Management", AdminShell.PAGE_DEPARTMENTS}, {"Course Management", AdminShell.PAGE_COURSES},
                {"Notices", AdminShell.PAGE_NOTICES}, {"Timetables", AdminShell.PAGE_TIMETABLES},
                {"Reports", AdminShell.PAGE_REPORTS}, {"Profile", AdminShell.PAGE_PROFILE},
                {"System Settings", AdminShell.PAGE_SETTINGS}};
        for (String[] item : sidebar) {
            JButton b = button(shell, item[0]);
            if (b != null) {
                b.doClick();
                awaitIfDashboard(shell);
            }
            check("sidebar '" + item[0] + "' opens page '" + item[1] + "'", b != null && item[1].equals(shell.getCurrentPage()));
        }
        JButton logout = button(shell, "Logout");
        check("sidebar has a Logout button with a handler", logout != null && logout.isEnabled()
                && logout.getActionListeners().length > 0);

        button(shell, "Dashboard").doClick();
        awaitLoaded(dashboard);
        String[][] quick = {{"users", AdminShell.PAGE_USERS}, {"departments", AdminShell.PAGE_DEPARTMENTS},
                {"courses", AdminShell.PAGE_COURSES}, {"notices", AdminShell.PAGE_NOTICES},
                {"timetables", AdminShell.PAGE_TIMETABLES}};
        for (String[] q : quick) {
            button(shell, "Dashboard").doClick();
            JButton b = (JButton) find(shell, "quick." + q[0]);
            b.doClick();
            check("dashboard quick action '" + b.getActionCommand() + "' opens '" + q[1] + "'", q[1].equals(shell.getCurrentPage()));
        }

        System.out.println("\n[B3] Every visible button works or is disabled");
        int audited = 0;
        List<String> dead = new ArrayList<>();
        for (JButton b : all(shell, JButton.class)) {
            if (insideLookAndFeelPart(b)) {
                continue;   // scrollbar / combo-box arrows work through the Look-and-Feel, not an ActionListener
            }
            audited++;
            if (b.isEnabled() && b.getActionListeners().length == 0) {
                dead.add("[" + b.getText() + "] in " + describeParents(b));
            }
        }
        check("all " + audited + " push buttons in the Admin shell have a handler or are disabled (dead: " + dead + ")",
                audited > 20 && dead.isEmpty());

        System.out.println("\n[B4] Profile through the shell");
        button(shell, "Profile").doClick();
        check("profile shows the database row, not the session copy", "Ada Database".equals(fieldText(shell, "profile.name")));
        EXEC.clear();
        ((JTextField) find(shell, "profile.contact")).setText("0799999999");
        captureErr(() -> ((JButton) find(shell, "profile.update")).doClick());
        Exec update = EXEC.stream().filter(e -> e.sql().startsWith("UPDATE users SET full_name")).findFirst().orElse(null);
        check("Update Profile runs one UPDATE for user 1 with the edited contact number",
                update != null && "0799999999".equals(String.valueOf(update.params().get(3)))
                        && Integer.valueOf(1).equals(update.params().get(5)));
        check("the Admin shell has a Logout button on the profile page too", find(shell, "profile.logout") != null);

        System.out.println("\n[B5] Database failure handling");
        button(shell, "Dashboard").doClick();
        awaitLoaded(dashboard);
        dbDown = true;
        PrintStream keep = System.err;
        String err = captureErr(() -> {
            try {
                SwingUtilities.invokeAndWait(dashboard::refresh);
                awaitLoaded(dashboard);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("database down: dashboard shows '-' on every card and one readable message (no crash)",
                "-".equals(value(shell, "users")) && "-".equals(value(shell, "notices"))
                        && fieldText(shell, "dashboard.error").contains("Cannot connect to the database"));
        check("database down: the cause is logged", err.contains("Database error"));
        String navErr = captureErr(() -> {
            for (String page : List.of("Users", "Departments", "Courses", "Notices", "Timetables", "Reports", "Profile")) {
                JButton b = button(shell, page.equals("Users") ? "User Management"
                        : page.equals("Departments") ? "Department Management"
                        : page.equals("Courses") ? "Course Management" : page);
                if (b != null) {
                    b.doClick();
                }
            }
        });
        check("database down: opening every other page does not throw", navErr != null && !navErr.contains("Exception in thread"));
        check("database down: profile still opens with the session details and an inline message",
                Session.getCurrentUser().getFullName().equals(fieldText(shell, "profile.name"))
                        && visible(shell, "profile.error"));
        dbDown = false;
        button(shell, "Dashboard").doClick();
        ((JButton) find(shell, "dashboard.retry")).doClick();
        awaitLoaded(dashboard);
        check("database back: Retry restores the live figures", "31".equals(value(shell, "users"))
                && "6".equals(value(shell, "notices")) && !visible(shell, "dashboard.error"));

        noticesMissing = true;
        captureErr(() -> {
            try {
                SwingUtilities.invokeAndWait(dashboard::refresh);
                awaitLoaded(dashboard);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("notices migration not run: users/departments/courses stay live, notices shows '-' with the migration hint",
                "31".equals(value(shell, "users")) && "4".equals(value(shell, "departments"))
                        && "9".equals(value(shell, "courses")) && "-".equals(value(shell, "notices"))
                        && fieldText(shell, "dashboard.error").contains("001_notices_timetables.sql"));
        noticesMissing = false;
        System.setErr(keep);

        System.out.println("\n[B5b] Department form (the Add/Edit dialog behind Department Management)");
        Class<?> formClass = Class.forName("com.facultyams.ui.admin.DepartmentFormPanel");
        var ctor = formClass.getDeclaredConstructor(com.facultyams.service.DepartmentService.class, UserService.class,
                com.facultyams.model.Department.class, com.facultyams.dao.DepartmentDAO.DepartmentHead.class, boolean.class);
        ctor.setAccessible(true);
        Object form = ctor.newInstance(new com.facultyams.service.DepartmentService(), new UserService(), null, null, true);
        var codeField = formClass.getDeclaredField("codeField");
        var nameField = formClass.getDeclaredField("nameField");
        var headCombo = formClass.getDeclaredField("headCombo");
        var save = formClass.getDeclaredMethod("save");
        codeField.setAccessible(true);
        nameField.setAccessible(true);
        headCombo.setAccessible(true);
        save.setAccessible(true);
        check("Add Department: the head-of-department choice is disabled (a new department has no lecturers yet)",
                !((javax.swing.JComboBox<?>) headCombo.get(form)).isEnabled());
        ((JTextField) codeField.get(form)).setText("dict");
        ((JTextField) nameField.get(form)).setText("Information and Communication Technology");
        EXEC.clear();
        boolean saved = (Boolean) save.invoke(form);
        Exec insert = EXEC.stream().filter(e -> e.sql().startsWith("INSERT INTO departments")).findFirst().orElse(null);
        check("Add Department: save() inserts the department through the service", saved && insert != null);
        check("Add Department: the code is normalised to upper case by the service",
                insert != null && insert.params().containsValue("DICT"));
        ((JTextField) codeField.get(form)).setText("");
        ((JTextField) nameField.get(form)).setText("");
        EXEC.clear();
        String formErr = captureErr(() -> {
            try {
                check("Add Department: blank code and name are refused (save() returns false)", !(Boolean) save.invoke(form));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        check("Add Department: a refused save writes nothing and explains why",
                EXEC.stream().noneMatch(e -> e.sql().startsWith("INSERT")) && formErr.contains("ValidationException"));

        System.out.println("\n[B6] Access control and session");
        Session.start(student(5));
        boolean blocked = false;
        try {
            new AdminShell(null, Session.getCurrentUser());
        } catch (RuntimeException e) {
            blocked = true;
        }
        check("a Student cannot open the Admin shell", blocked);
        Session.end();

        String dir = System.getProperty("render.dir");
        if (dir != null) {
            Session.start(me);
            new File(dir).mkdirs();
            button(shell, "Dashboard").doClick();
            awaitLoaded(dashboard);
            render(shell, 1200, 760, new File(dir, "dashboard-1200.png"));
            render(shell, 900, 600, new File(dir, "dashboard-900.png"));
            button(shell, "Profile").doClick();
            render(shell, 1200, 760, new File(dir, "profile-1200.png"));
            dbDown = true;
            captureErr(() -> {
                try {
                    button(shell, "Dashboard").doClick();
                    SwingUtilities.invokeAndWait(dashboard::refresh);
                    awaitLoaded(dashboard);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            render(shell, 1200, 760, new File(dir, "dashboard-db-down.png"));
            dbDown = false;
            noticesMissing = true;
            captureErr(() -> {
                try {
                    SwingUtilities.invokeAndWait(dashboard::refresh);
                    awaitLoaded(dashboard);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            render(shell, 1200, 760, new File(dir, "dashboard-notices-missing.png"));
            for (String page : List.of("User Management", "Department Management", "Course Management", "Notices", "Timetables")) {
                button(shell, page).doClick();
                render(shell, 1200, 760, new File(dir, "page-" + page.toLowerCase().replace(' ', '-') + ".png"));
            }
            System.out.println("  (screenshots written to " + dir + ")");
        }
    }

    private static boolean insideLookAndFeelPart(Component c) {
        for (Component p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof javax.swing.JScrollBar || p instanceof javax.swing.JComboBox
                    || p instanceof javax.swing.JSpinner || p instanceof javax.swing.JTabbedPane) {
                return true;
            }
        }
        return false;
    }

    private static String describeParents(Component c) {
        StringBuilder sb = new StringBuilder();
        for (Component p = c.getParent(); p != null && sb.length() < 80; p = p.getParent()) {
            sb.append(p.getClass().getSimpleName()).append('<');
        }
        return sb.toString();
    }

    private static void awaitIfDashboard(AdminShell shell) throws Exception {
        if (AdminShell.PAGE_DASHBOARD.equals(shell.getCurrentPage())) {
            awaitLoaded(all(shell, DashboardPanel.class).get(0));
        }
    }

    private static JButton button(Container root, String text) {
        for (JButton b : all(root, JButton.class)) {
            if (text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    // ----- fake driver ----------------------------------------------------------------------------

    private static Object defaultFor(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }

    private static List<Map<String, Object>> answer(String sql) throws SQLException {
        if (noticesMissing && sql.contains("notices")) {
            throw new SQLException("Table 'fot.notices' doesn't exist", "42S02", 1146);
        }
        if (sql.contains("FROM users GROUP BY role")) {
            return List.of(roleRow("ADMIN", "admins"), roleRow("LECTURER", "lecturers"),
                    roleRow("TECHNICAL_OFFICER", "technical_officers"), roleRow("STUDENT", "students"));
        }
        if (sql.equals("SELECT COUNT(*) FROM departments")) {
            return List.of(Map.of("#1", TABLE.get("departments")));
        }
        if (sql.equals("SELECT COUNT(*) FROM courses")) {
            return List.of(Map.of("#1", TABLE.get("courses")));
        }
        if (sql.equals("SELECT COUNT(*) FROM notices")) {
            return List.of(Map.of("#1", TABLE.get("notices")));
        }
        if (sql.contains("WHERE u.user_id = ?")) {
            return List.of(USER_ROW);
        }
        return List.of();
    }

    private static Map<String, Object> roleRow(String role, String key) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("total", TABLE.get(key));
        return m;
    }

    private static final class FakeDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            if (dbDown) {
                throw new SQLException("Communications link failure: connection refused");
            }
            boolean[] closed = {false};
            return (Connection) Proxy.newProxyInstance(AdminDashboardChecks.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, (proxy, method, a) -> switch (method.getName()) {
                        case "prepareStatement" -> statement((String) a[0]);
                        case "close" -> {
                            closed[0] = true;
                            yield null;
                        }
                        case "isClosed" -> closed[0];
                        case "isValid" -> !closed[0] && !dbDown;
                        case "getAutoCommit" -> true;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == a[0];
                        case "toString" -> "FakeConnection";
                        default -> defaultFor(method.getReturnType());
                    });
        }

        private static Object statement(String sql) {
            TreeMap<Integer, Object> params = new TreeMap<>();
            return Proxy.newProxyInstance(AdminDashboardChecks.class.getClassLoader(),
                    new Class<?>[] {java.sql.PreparedStatement.class}, (proxy, method, a) -> {
                        String name = method.getName();
                        if (name.startsWith("set") && a != null && a.length >= 2 && a[0] instanceof Integer idx) {
                            params.put(idx, a[1]);
                            return null;
                        }
                        switch (name) {
                            case "executeQuery" -> {
                                synchronized (EXEC) {
                                    EXEC.add(new Exec(sql, params));
                                }
                                return resultSet(answer(sql));
                            }
                            case "executeUpdate" -> {
                                synchronized (EXEC) {
                                    EXEC.add(new Exec(sql, params));
                                }
                                return 1;
                            }
                            case "getGeneratedKeys" -> {
                                return resultSet(List.of(Map.of("#1", 42)));
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "equals" -> {
                                return proxy == a[0];
                            }
                            default -> {
                                return defaultFor(method.getReturnType());
                            }
                        }
                    });
        }

        private static Object resultSet(List<Map<String, Object>> rows) {
            int[] pos = {-1};
            boolean[] wasNull = {false};
            return Proxy.newProxyInstance(AdminDashboardChecks.class.getClassLoader(),
                    new Class<?>[] {java.sql.ResultSet.class}, (proxy, method, a) -> {
                        String name = method.getName();
                        if ("next".equals(name)) {
                            return ++pos[0] < rows.size();
                        }
                        if ("wasNull".equals(name)) {
                            return wasNull[0];
                        }
                        if (name.startsWith("get") && a != null && a.length == 1
                                && (a[0] instanceof String || a[0] instanceof Integer)) {
                            Object key = a[0] instanceof Integer i ? "#" + i : a[0];
                            Object v = rows.get(pos[0]).get(key);
                            wasNull[0] = v == null;
                            return switch (name) {
                                case "getInt" -> v == null ? 0 : ((Number) v).intValue();
                                case "getBoolean" -> v != null && (Boolean) v;
                                case "getString" -> v == null ? null : v.toString();
                                case "getTimestamp" -> null;
                                default -> v;
                            };
                        }
                        return defaultFor(method.getReturnType());
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fakeadmin:");
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
}
