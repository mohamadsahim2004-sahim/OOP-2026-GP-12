package com.facultyams;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.NoticeDAO;
import com.facultyams.dao.TimeTableDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Admin;
import com.facultyams.model.Course;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Notice;
import com.facultyams.model.Student;
import com.facultyams.model.TechnicalOfficer;
import com.facultyams.model.TimeTable;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.service.NoticeService;
import com.facultyams.service.TimeTableService;
import com.facultyams.ui.admin.AdminShell;
import com.facultyams.ui.admin.NoticeManagementPanel;
import com.facultyams.ui.admin.TimeTableManagementPanel;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Framework-free checks for the shared Notice / Timetable module and its Admin screens
 * (same style as {@link CoreChecks}: no JUnit, no MySQL server needed). From the project root:
 * <pre>
 *   java -cp target/classes:target/test-classes com.facultyams.NoticeTimetableChecks
 * </pre>
 * Part A (this JVM) tests NoticeService / TimeTableService with in-memory fake DAOs: permissions for
 * every role, validation, clash detection and database failures.
 * Part B re-launches itself with a fake JDBC driver (FOTAMS_DB_URL=jdbc:fakenotice:...) and tests the real
 * NoticeDAO / TimeTableDAO (prepared statements, parameter binding, row mapping, error messages), the real
 * Admin panels, and the Admin shell navigation. Exit code 0 means every check passed.
 */
public final class NoticeTimetableChecks {

    private static int passed;
    private static int failed;

    private NoticeTimetableChecks() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0 && "jdbc".equals(args[0])) {
            daoChecks();
            panelChecks();
            shellChecks();
        } else {
            modelChecks();
            noticeServiceChecks();
            timeTableServiceChecks();
            int child = runJdbcPart();
            if (child != 0) {
                failed++;
                System.out.println("  FAIL  part B (fake JDBC) reported failures, exit code " + child);
            }
        }
        boolean child = args.length > 0 && "jdbc".equals(args[0]);
        System.out.println("\nRESULT" + (child ? " (part B)" : "") + ": " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ================================================================= helpers

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

    private static String messageOf(Thrower t) {
        try {
            t.run();
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private static User newUser(Role role) {
        User u = switch (role) {
            case ADMIN -> new Admin();
            case LECTURER -> new Lecturer();
            case TECHNICAL_OFFICER -> new TechnicalOfficer();
            case STUDENT -> new Student();
        };
        u.setUserId(7);
        u.setUsername("test." + role.name().toLowerCase());
        u.setFullName("Test " + role.getDisplayName());
        u.setActive(true);
        return u;
    }

    /** null = logged out. */
    private static void loginAs(Role role) {
        if (role == null) {
            Session.end();
        } else {
            Session.start(newUser(role));
        }
    }

    private static final Role[] WHO = {null, Role.ADMIN, Role.LECTURER, Role.TECHNICAL_OFFICER, Role.STUDENT};

    private static String who(Role r) {
        return r == null ? "logged out" : r.toString();
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    // ================================================================= A1. models

    private static void modelChecks() {
        System.out.println("[A1] Models");
        TimeTable t = new TimeTable(1, 1, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 30), "LT-01");
        t.setCourseCode("ICT2132");
        check("TimeTable.getTimeRange is HH:mm - HH:mm", "08:00 - 10:30".equals(t.getTimeRange()));
        check("TimeTable.getDayName is a readable English name", "Monday".equals(t.getDayName()));
        check("TimeTable.toString describes the slot", "Monday 08:00 - 10:30 ICT2132 (LT-01)".equals(t.toString()));
        TimeTable empty = new TimeTable();
        check("empty TimeTable does not throw from getTimeRange/getDayName/toString",
                "".equals(empty.getTimeRange()) && "".equals(empty.getDayName()) && empty.toString() != null);
        check("Notice.toString shows title and date",
                "Exam Notice (2026-09-20)".equals(new Notice(1, "Exam Notice", "x", LocalDate.of(2026, 9, 20)).toString()));
    }

    // ================================================================= fake DAOs (in memory)

    private static class FakeNoticeDAO extends NoticeDAO {
        final List<Notice> store = new ArrayList<>();
        int nextId = 1;
        int writes;
        boolean down;

        private void gate() throws DatabaseException {
            if (down) {
                throw new DatabaseException("Cannot connect to the database.");
            }
        }

        /** Lookup that bypasses the 'database down' switch and the service layer. */
        Notice findByIdQuiet(int id) {
            return store.stream().filter(n -> n.getNoticeId() == id).findFirst().orElse(null);
        }

        Notice seed(String title, String content, LocalDate date) {
            Notice n = new Notice(nextId++, title, content, date);
            n.setCreatedBy(1);
            store.add(n);
            return n;
        }

        @Override
        public List<Notice> findAll() throws DatabaseException {
            gate();
            return new ArrayList<>(store);
        }

        @Override
        public List<Notice> search(String keyword) throws DatabaseException {
            gate();
            if (keyword == null || keyword.isBlank()) {
                return findAll();
            }
            String k = keyword.trim().toLowerCase();
            return store.stream().filter(n -> n.getTitle().toLowerCase().contains(k)
                    || n.getContent().toLowerCase().contains(k)).toList();
        }

        @Override
        public Notice findById(int id) throws DatabaseException {
            gate();
            return store.stream().filter(n -> n.getNoticeId() == id).findFirst().orElse(null);
        }

        @Override
        public int insert(Notice n) throws DatabaseException {
            gate();
            writes++;
            n.setNoticeId(nextId++);
            store.add(n);
            return n.getNoticeId();
        }

        @Override
        public void update(Notice n) throws DatabaseException {
            gate();
            writes++;
            Notice stored = findById(n.getNoticeId());
            stored.setTitle(n.getTitle());           // like the real SQL: author is not touched
            stored.setContent(n.getContent());
            stored.setNoticeDate(n.getNoticeDate());
        }

        @Override
        public void delete(int id) throws DatabaseException {
            gate();
            writes++;
            store.removeIf(n -> n.getNoticeId() == id);
        }
    }

    private static final class FakeCourseDAO extends CourseDAO {
        @Override
        public Course findById(int courseId) {
            return switch (courseId) {
                case 1 -> new Course(1, "ICT2132", "Object Oriented Programming", 3, 2, 1, Course.CourseType.THEORY, 1);
                case 2 -> new Course(2, "ICT2113", "Data Structures", 3, 2, 1, Course.CourseType.THEORY, 1);
                default -> null;
            };
        }
    }

    private static final class FakeTimeTableDAO extends TimeTableDAO {
        final List<TimeTable> store = new ArrayList<>();
        int nextId = 1;
        int writes;
        boolean down;
        List<Object> lastConflictArgs;
        Integer lastDepartment;
        Integer lastSemester;
        String lastKeyword;

        private void gate() throws DatabaseException {
            if (down) {
                throw new DatabaseException("Cannot connect to the database.");
            }
        }

        TimeTable seed(int courseId, DayOfWeek day, String start, String end, String venue) {
            TimeTable t = new TimeTable(nextId++, courseId, day, LocalTime.parse(start), LocalTime.parse(end), venue);
            t.setCourseCode(courseId == 1 ? "ICT2132" : "ICT2113");
            store.add(t);
            return t;
        }

        @Override
        public List<TimeTable> findAll(Integer departmentId, Integer semester) throws DatabaseException {
            gate();
            lastDepartment = departmentId;
            lastSemester = semester;
            return new ArrayList<>(store);
        }

        @Override
        public List<TimeTable> search(String keyword, Integer departmentId, Integer semester) throws DatabaseException {
            gate();
            lastKeyword = keyword;
            lastDepartment = departmentId;
            lastSemester = semester;
            return new ArrayList<>(store);
        }

        @Override
        public TimeTable findById(int id) throws DatabaseException {
            gate();
            return store.stream().filter(t -> t.getTimetableId() == id).findFirst().orElse(null);
        }

        /** Same overlap rule as the real SQL: same day, start < otherEnd and end > otherStart, same venue or course. */
        @Override
        public List<TimeTable> findConflicts(DayOfWeek day, LocalTime start, LocalTime end, String venue,
                                             int courseId, int excludeId) throws DatabaseException {
            gate();
            lastConflictArgs = List.of(day, start, end, venue, courseId, excludeId);
            return store.stream().filter(t -> t.getTimetableId() != excludeId && t.getDayOfWeek() == day
                    && t.getStartTime().isBefore(end) && t.getEndTime().isAfter(start)
                    && (t.getVenue().equalsIgnoreCase(venue) || t.getCourseId() == courseId)).toList();
        }

        @Override
        public int insert(TimeTable slot) throws DatabaseException {
            gate();
            writes++;
            slot.setTimetableId(nextId++);
            slot.setCourseCode(slot.getCourseId() == 1 ? "ICT2132" : "ICT2113");
            store.add(slot);
            return slot.getTimetableId();
        }

        @Override
        public void update(TimeTable slot) throws DatabaseException {
            gate();
            writes++;
            store.removeIf(t -> t.getTimetableId() == slot.getTimetableId());
            slot.setCourseCode(slot.getCourseId() == 1 ? "ICT2132" : "ICT2113");   // the real SELECT joins this in
            store.add(slot);
        }

        @Override
        public void delete(int id) throws DatabaseException {
            gate();
            writes++;
            store.removeIf(t -> t.getTimetableId() == id);
        }
    }

    // ================================================================= A2. NoticeService

    private static Notice notice(String title, String content, LocalDate date) {
        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setNoticeDate(date);
        return n;
    }

    private static final LocalDate D = LocalDate.of(2026, 9, 20);

    private static void noticeServiceChecks() {
        System.out.println("[A2] NoticeService");
        FakeNoticeDAO dao = new FakeNoticeDAO();
        NoticeService svc = new NoticeService(dao);
        dao.seed("Exam Notice", "Mid exam schedule will be released next week.", D);
        dao.seed("Library Notice", "New books are available in the faculty library.", LocalDate.of(2026, 8, 28));

        for (Role role : WHO) {
            loginAs(role);
            boolean loggedIn = role != null;
            boolean admin = role == Role.ADMIN;
            String w = who(role);
            check(w + ": view notices (list/search/by id) " + (loggedIn ? "allowed" : "denied"),
                    loggedIn == (!throwsA(AuthorizationException.class, svc::getAllNotices)
                            && !throwsA(AuthorizationException.class, () -> svc.searchNotices("exam"))
                            && !throwsA(AuthorizationException.class, () -> svc.getNoticeById(1))));

            int before = dao.writes;
            boolean addDenied = throwsA(AuthorizationException.class, () -> svc.addNotice(notice("T", "C", D)));
            boolean updDenied = throwsA(AuthorizationException.class, () -> {
                Notice n = notice("T2", "C2", D);
                n.setNoticeId(1);
                svc.updateNotice(n);
            });
            boolean delDenied = throwsA(AuthorizationException.class, () -> svc.deleteNotice(2));
            if (admin) {
                check("ADMIN: add / update / delete allowed", !addDenied && !updDenied && !delDenied);
            } else {
                check(w + ": add / update / delete denied and nothing written",
                        addDenied && updDenied && delDenied && dao.writes == before);
            }
        }
        // the Admin pass above really added / changed / deleted records: start the next section from a clean fixture
        dao.store.clear();
        dao.nextId = 1;
        dao.seed("Exam Notice", "Mid exam schedule will be released next week.", D);
        dao.seed("Library Notice", "New books are available in the faculty library.", LocalDate.of(2026, 8, 28));

        loginAs(Role.ADMIN);
        check("addNotice(null) -> ValidationException", throwsA(ValidationException.class, () -> svc.addNotice(null)));
        check("blank title rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice("   ", "c", D))));
        check("null title rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice(null, "c", D))));
        check("title of 150 chars accepted", !throwsA(ValidationException.class, () -> svc.addNotice(notice(repeat('t', 150), "c", D))));
        check("title of 151 chars rejected (column is VARCHAR(150))",
                throwsA(ValidationException.class, () -> svc.addNotice(notice(repeat('t', 151), "c", D))));
        check("blank content rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice("t", " \n ", D))));
        check("content of 5000 chars accepted", !throwsA(ValidationException.class, () -> svc.addNotice(notice("t", repeat('c', 5000), D))));
        check("content of 5001 chars rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice("t", repeat('c', 5001), D))));
        check("missing date rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice("t", "c", null))));
        check("date before 2000 rejected (typo guard)",
                throwsA(ValidationException.class, () -> svc.addNotice(notice("t", "c", LocalDate.of(26, 9, 20)))));
        check("date after 2100 rejected", throwsA(ValidationException.class, () -> svc.addNotice(notice("t", "c", LocalDate.of(2101, 1, 1)))));
        check("a future publication date within range is accepted",
                !throwsA(ValidationException.class, () -> svc.addNotice(notice("t", "c", LocalDate.of(2027, 1, 5)))));

        int writesBefore = dao.writes;
        throwsA(ValidationException.class, () -> svc.addNotice(notice("", "c", D)));
        check("an invalid notice never reaches the DAO", dao.writes == writesBefore);

        Notice in = notice("  Padded title  ", "  body text \n second line  ", D);
        in.setNoticeId(999);
        Notice saved = null;
        try {
            saved = svc.addNotice(in);
        } catch (Exception e) {
            System.out.println("        unexpected: " + e);
        }
        check("addNotice returns the saved notice with a database-assigned id (caller's id ignored)",
                saved != null && saved.getNoticeId() != 999 && saved.getNoticeId() > 0);
        check("addNotice trims title and content", saved != null && "Padded title".equals(saved.getTitle())
                && saved.getContent().startsWith("body text") && saved.getContent().endsWith("second line"));
        check("addNotice records the logged-in admin as author",
                saved != null && Integer.valueOf(7).equals(saved.getCreatedBy()) && "Test Administrator".equals(saved.getCreatedByName()));

        check("getNoticeById(0) -> ValidationException", throwsA(ValidationException.class, () -> svc.getNoticeById(0)));
        check("getNoticeById(unknown) -> ValidationException", throwsA(ValidationException.class, () -> svc.getNoticeById(12345)));
        Notice found = null;
        try {
            found = svc.getNoticeById(1);
        } catch (Exception e) {
            System.out.println("        unexpected: " + e);
        }
        check("getNoticeById returns the notice", found != null && "Exam Notice".equals(found.getTitle()));

        Notice upd = notice("Exam Notice (revised)", "New date announced.", LocalDate.of(2026, 9, 25));
        upd.setNoticeId(1);
        upd.setCreatedBy(555);   // callers must not be able to change the author
        boolean updOk = !throwsA(Exception.class, () -> svc.updateNotice(upd));
        Notice after = dao.findByIdQuiet(1);
        check("updateNotice changes title, content and date", updOk && after != null
                && "Exam Notice (revised)".equals(after.getTitle()) && "New date announced.".equals(after.getContent())
                && LocalDate.of(2026, 9, 25).equals(after.getNoticeDate()));
        check("updateNotice keeps the original author", after != null && Integer.valueOf(1).equals(after.getCreatedBy()));
        Notice ghost = notice("t", "c", D);
        ghost.setNoticeId(4242);
        check("updateNotice on a missing notice -> ValidationException", throwsA(ValidationException.class, () -> svc.updateNotice(ghost)));
        Notice bad = notice("", "c", D);
        bad.setNoticeId(1);
        int w0 = dao.writes;
        check("updateNotice with invalid data -> ValidationException and no write",
                throwsA(ValidationException.class, () -> svc.updateNotice(bad)) && dao.writes == w0);
        check("updateNotice(null) -> ValidationException", throwsA(ValidationException.class, () -> svc.updateNotice(null)));

        check("deleteNotice on a missing notice -> ValidationException and no write",
                throwsA(ValidationException.class, () -> svc.deleteNotice(4242)) && dao.writes == w0);
        check("deleteNotice(0) -> ValidationException", throwsA(ValidationException.class, () -> svc.deleteNotice(0)));
        boolean delOk = !throwsA(Exception.class, () -> svc.deleteNotice(2));
        check("deleteNotice removes the notice", delOk && dao.findByIdQuiet(2) == null);

        try {
            check("searchNotices with blank keyword returns everything", svc.searchNotices("  ").size() == dao.store.size());
            check("searchNotices finds by title", svc.searchNotices("REVISED").size() == 1);
            check("searchNotices finds by content", svc.searchNotices("announced").size() == 1);
            check("searchNotices with no match returns an empty list", svc.searchNotices("zzzz").isEmpty());
        } catch (Exception e) {
            check("searchNotices threw " + e, false);
        }

        dao.down = true;
        check("database failure surfaces as DatabaseException (list)", throwsA(DatabaseException.class, svc::getAllNotices));
        check("database failure surfaces as DatabaseException (search)", throwsA(DatabaseException.class, () -> svc.searchNotices("x")));
        check("database failure surfaces as DatabaseException (add)", throwsA(DatabaseException.class, () -> svc.addNotice(notice("t", "c", D))));
        check("database failure surfaces as DatabaseException (update)", throwsA(DatabaseException.class, () -> {
            Notice n = notice("t", "c", D);
            n.setNoticeId(1);
            svc.updateNotice(n);
        }));
        check("database failure surfaces as DatabaseException (delete)", throwsA(DatabaseException.class, () -> svc.deleteNotice(1)));
        Session.end();
    }

    // ================================================================= A3. TimeTableService

    private static TimeTable slot(int courseId, DayOfWeek day, String start, String end, String venue) {
        TimeTable t = new TimeTable();
        t.setCourseId(courseId);
        t.setDayOfWeek(day);
        t.setStartTime(start == null ? null : LocalTime.parse(start));
        t.setEndTime(end == null ? null : LocalTime.parse(end));
        t.setVenue(venue);
        return t;
    }

    private static void timeTableServiceChecks() {
        System.out.println("[A3] TimeTableService");
        FakeTimeTableDAO dao = new FakeTimeTableDAO();
        TimeTableService svc = new TimeTableService(dao, new FakeCourseDAO());
        dao.seed(1, DayOfWeek.MONDAY, "08:00", "10:00", "LT-01");
        dao.seed(2, DayOfWeek.TUESDAY, "10:00", "12:00", "LT-02");

        for (Role role : WHO) {
            loginAs(role);
            boolean loggedIn = role != null;
            boolean admin = role == Role.ADMIN;
            String w = who(role);
            check(w + ": view timetable (list/search/by id) " + (loggedIn ? "allowed" : "denied"),
                    loggedIn == (!throwsA(AuthorizationException.class, () -> svc.getTimeTable(null, null))
                            && !throwsA(AuthorizationException.class, () -> svc.searchTimeTable("ict", 1, 1))
                            && !throwsA(AuthorizationException.class, () -> svc.getSlotById(1))));

            int before = dao.writes;
            boolean addDenied = throwsA(AuthorizationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "13:00", "15:00", "LT-09")));
            boolean updDenied = throwsA(AuthorizationException.class, () -> {
                TimeTable t = slot(1, DayOfWeek.MONDAY, "08:00", "10:00", "LT-01");
                t.setTimetableId(1);
                svc.updateSlot(t);
            });
            boolean delDenied = throwsA(AuthorizationException.class, () -> svc.deleteSlot(2));
            if (admin) {
                check("ADMIN: add / update / delete allowed", !addDenied && !updDenied && !delDenied);
            } else {
                check(w + ": add / update / delete denied and nothing written",
                        addDenied && updDenied && delDenied && dao.writes == before);
            }
        }

        // the Admin pass above really added / changed / deleted records: start from a clean fixture
        dao.store.clear();
        dao.nextId = 1;
        dao.seed(1, DayOfWeek.MONDAY, "08:00", "10:00", "LT-01");
        dao.seed(2, DayOfWeek.TUESDAY, "10:00", "12:00", "LT-02");

        loginAs(Role.ADMIN);
        check("addSlot(null) -> ValidationException", throwsA(ValidationException.class, () -> svc.addSlot(null)));
        check("Sunday rejected (table only has Mon-Sat)",
                throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.SUNDAY, "08:00", "10:00", "LT-05"))));
        check("missing day rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, null, "08:00", "10:00", "LT-05"))));
        check("missing start time rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, null, "10:00", "LT-05"))));
        check("missing end time rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "08:00", null, "LT-05"))));
        check("end == start rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "08:00", "08:00", "LT-05"))));
        check("end before start rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "10:00", "08:00", "LT-05"))));
        check("end only seconds after start (same minute) rejected after truncation", throwsA(ValidationException.class, () -> {
            TimeTable t = slot(1, DayOfWeek.FRIDAY, "08:00", "08:00", "LT-05");
            t.setEndTime(LocalTime.of(8, 0, 30));
            svc.addSlot(t);
        }));
        check("blank venue rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "08:00", "10:00", "  "))));
        check("null venue rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "08:00", "10:00", null))));
        check("venue of 50 chars accepted", !throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.SATURDAY, "08:00", "10:00", repeat('v', 50)))));
        check("venue of 51 chars rejected (column is VARCHAR(50))",
                throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.SATURDAY, "11:00", "12:00", repeat('v', 51)))));
        check("course id 0 rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(0, DayOfWeek.FRIDAY, "08:00", "10:00", "LT-05"))));
        check("unknown course rejected", throwsA(ValidationException.class, () -> svc.addSlot(slot(99, DayOfWeek.FRIDAY, "08:00", "10:00", "LT-05"))));

        int w0 = dao.writes;
        throwsA(ValidationException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "10:00", "08:00", "LT-05")));
        check("an invalid slot never reaches the DAO", dao.writes == w0);

        TimeTable raw = slot(2, DayOfWeek.WEDNESDAY, "14:00", "16:00", "  Lab 3  ");
        raw.setStartTime(LocalTime.of(14, 0, 45));
        TimeTable saved = null;
        try {
            saved = svc.addSlot(raw);
        } catch (Exception e) {
            System.out.println("        unexpected: " + e);
        }
        check("addSlot returns the saved slot with a new id", saved != null && saved.getTimetableId() > 2);
        check("addSlot trims the venue and cuts times to whole minutes", saved != null && "Lab 3".equals(saved.getVenue())
                && LocalTime.of(14, 0).equals(saved.getStartTime()));

        // clash detection
        String m1 = messageOf(() -> svc.addSlot(slot(2, DayOfWeek.MONDAY, "09:00", "11:00", "lt-01")));
        check("same venue overlapping another course is rejected with a venue message (case-insensitive venue)",
                m1 != null && m1.contains("venue") && m1.contains("ICT2132"));
        String m2 = messageOf(() -> svc.addSlot(slot(1, DayOfWeek.MONDAY, "09:00", "11:00", "LT-77")));
        check("same course overlapping in another venue is rejected with a course message",
                m2 != null && m2.contains("course already"));
        check("back-to-back slots in the same venue are allowed",
                !throwsA(ValidationException.class, () -> svc.addSlot(slot(2, DayOfWeek.MONDAY, "10:00", "12:00", "LT-01"))));
        check("same time and venue on a different day is allowed",
                !throwsA(ValidationException.class, () -> svc.addSlot(slot(2, DayOfWeek.THURSDAY, "08:00", "10:00", "LT-01"))));
        check("same time on the same day in a different venue and course is allowed",
                !throwsA(ValidationException.class, () -> svc.addSlot(slot(2, DayOfWeek.MONDAY, "08:00", "10:00", "LT-55"))));
        check("conflict query receives day, start, end, venue, course and exclude-id 0 for a new slot",
                dao.lastConflictArgs != null && dao.lastConflictArgs.get(5).equals(0));

        // update
        TimeTable first = dao.store.stream().filter(t -> t.getTimetableId() == 1).findFirst().orElseThrow();
        TimeTable same = slot(1, DayOfWeek.MONDAY, "08:00", "10:00", "LT-01");
        same.setTimetableId(first.getTimetableId());
        check("updating a slot to its own times/venue is not a clash with itself", !throwsA(ValidationException.class, () -> svc.updateSlot(same)));
        check("conflict query excludes the slot being edited", Integer.valueOf(1).equals(dao.lastConflictArgs.get(5)));
        TimeTable intoClash = slot(1, DayOfWeek.TUESDAY, "11:00", "12:30", "LT-02");
        intoClash.setTimetableId(1);
        check("moving a slot into another slot's venue/time is rejected", throwsA(ValidationException.class, () -> svc.updateSlot(intoClash)));
        TimeTable ghost = slot(1, DayOfWeek.FRIDAY, "08:00", "10:00", "LT-05");
        ghost.setTimetableId(4242);
        check("updateSlot on a missing slot -> ValidationException", throwsA(ValidationException.class, () -> svc.updateSlot(ghost)));
        check("updateSlot(null) -> ValidationException", throwsA(ValidationException.class, () -> svc.updateSlot(null)));

        // delete
        int w1 = dao.writes;
        check("deleteSlot on a missing slot -> ValidationException and no write",
                throwsA(ValidationException.class, () -> svc.deleteSlot(4242)) && dao.writes == w1);
        check("deleteSlot(0) -> ValidationException", throwsA(ValidationException.class, () -> svc.deleteSlot(0)));
        boolean ok = !throwsA(Exception.class, () -> svc.deleteSlot(2));
        check("deleteSlot removes the slot", ok && dao.store.stream().noneMatch(t -> t.getTimetableId() == 2));

        // reading
        try {
            svc.getTimeTable(3, 2);
            check("getTimeTable passes department and semester to the DAO", Integer.valueOf(3).equals(dao.lastDepartment)
                    && Integer.valueOf(2).equals(dao.lastSemester));
            svc.searchTimeTable("lt-01", null, 1);
            check("searchTimeTable passes keyword, department and semester to the DAO",
                    "lt-01".equals(dao.lastKeyword) && dao.lastDepartment == null && Integer.valueOf(1).equals(dao.lastSemester));
        } catch (Exception e) {
            check("timetable read threw " + e, false);
        }
        check("getSlotById(0) -> ValidationException", throwsA(ValidationException.class, () -> svc.getSlotById(0)));
        check("getSlotById(unknown) -> ValidationException", throwsA(ValidationException.class, () -> svc.getSlotById(4242)));

        dao.down = true;
        check("database failure surfaces as DatabaseException (list)", throwsA(DatabaseException.class, () -> svc.getTimeTable(null, null)));
        check("database failure surfaces as DatabaseException (search)", throwsA(DatabaseException.class, () -> svc.searchTimeTable("x", null, null)));
        check("database failure surfaces as DatabaseException (add)",
                throwsA(DatabaseException.class, () -> svc.addSlot(slot(1, DayOfWeek.FRIDAY, "08:00", "10:00", "LT-05"))));
        check("database failure surfaces as DatabaseException (delete)", throwsA(DatabaseException.class, () -> svc.deleteSlot(1)));
        Session.end();
    }

    // ================================================================= launch part B in a child JVM

    private static int runJdbcPart() throws Exception {
        System.out.println("[B] Re-launching with a fake JDBC driver (real DAOs, panels and shell)");
        String java = System.getProperty("java.home") + "/bin/java";
        ProcessBuilder pb = new ProcessBuilder(java, "-Djava.awt.headless=true", "-cp",
                System.getProperty("java.class.path"), NoticeTimetableChecks.class.getName(), "jdbc");
        pb.environment().put("FOTAMS_DB_URL", "jdbc:fakenotice:test");
        pb.inheritIO();
        // the child prints its own PASS/FAIL lines and a RESULT line; its exit code is what counts here
        return pb.start().waitFor();
    }

    // ================================================================= fake JDBC driver (part B)

    /** One statement the code under test executed: its SQL text and the parameters bound to it. */
    private record Exec(String sql, TreeMap<Integer, Object> params) {
    }

    private static final List<Exec> EXEC = new ArrayList<>();
    private static volatile Function<String, List<Map<String, Object>>> responder = sql -> List.of();
    private static volatile SQLException failNext;

    private static final class NullParam {
        final int sqlType;

        NullParam(int sqlType) {
            this.sqlType = sqlType;
        }

        @Override
        public String toString() {
            return "NULL(" + sqlType + ")";
        }
    }

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

    private static final class FakeDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            boolean[] closed = {false};
            return (Connection) Proxy.newProxyInstance(NoticeTimetableChecks.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, (proxy, method, a) -> switch (method.getName()) {
                        case "prepareStatement" -> statement((String) a[0]);
                        case "close" -> {
                            closed[0] = true;
                            yield null;
                        }
                        case "isClosed" -> closed[0];
                        case "isValid" -> !closed[0];
                        case "getAutoCommit" -> true;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == a[0];
                        case "toString" -> "FakeConnection";
                        default -> defaultFor(method.getReturnType());
                    });
        }

        private static Object statement(String sql) {
            TreeMap<Integer, Object> params = new TreeMap<>();
            return Proxy.newProxyInstance(NoticeTimetableChecks.class.getClassLoader(),
                    new Class<?>[] {java.sql.PreparedStatement.class}, (proxy, method, a) -> {
                        switch (method.getName()) {
                            case "setObject", "setString", "setInt" -> {
                                params.put((Integer) a[0], a[1]);
                                return null;
                            }
                            case "setNull" -> {
                                params.put((Integer) a[0], new NullParam((Integer) a[1]));
                                return null;
                            }
                            case "executeQuery" -> {
                                failIfRequested();
                                EXEC.add(new Exec(sql, params));
                                return resultSet(responder.apply(sql));
                            }
                            case "executeUpdate" -> {
                                failIfRequested();
                                EXEC.add(new Exec(sql, params));
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

        private static void failIfRequested() throws SQLException {
            SQLException e = failNext;
            if (e != null) {
                failNext = null;
                throw e;
            }
        }

        private static Object resultSet(List<Map<String, Object>> rows) {
            int[] pos = {-1};
            boolean[] wasNull = {false};
            return Proxy.newProxyInstance(NoticeTimetableChecks.class.getClassLoader(),
                    new Class<?>[] {java.sql.ResultSet.class}, (proxy, method, a) -> {
                        String name = method.getName();
                        if ("next".equals(name)) {
                            return ++pos[0] < rows.size();
                        }
                        if ("wasNull".equals(name)) {
                            return wasNull[0];
                        }
                        if (a != null && a.length >= 1 && (a[0] instanceof String || a[0] instanceof Integer)
                                && name.startsWith("get")) {
                            Object key = a[0] instanceof Integer i ? "#" + i : a[0];
                            Object value = rows.get(pos[0]).get(key);
                            wasNull[0] = value == null;
                            return switch (name) {
                                case "getInt" -> value == null ? 0 : ((Number) value).intValue();
                                case "getString" -> value == null ? null : value.toString();
                                default -> value;       // getObject(label, Class)
                            };
                        }
                        return defaultFor(method.getReturnType());
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fakenotice:");
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

    private static void installFakeDriver() throws SQLException {
        DriverManager.registerDriver(new FakeDriver());
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> noticeRow(int id, String title, String content, LocalDate date, Integer by, String byName) {
        return row("notice_id", id, "title", title, "content", content, "notice_date", date,
                "created_by", by, "created_by_name", byName);
    }

    private static Map<String, Object> slotRow(int id, int courseId, String day, String start, String end, String venue,
                                               String code, String name, int level, int sem, int dept, String deptName) {
        return row("timetable_id", id, "course_id", courseId, "day_of_week", day,
                "start_time", LocalTime.parse(start), "end_time", LocalTime.parse(end), "venue", venue,
                "course_code", code, "course_name", name, "academic_level", level, "semester", sem,
                "department_id", dept, "department_name", deptName);
    }

    private static Exec last() {
        return EXEC.get(EXEC.size() - 1);
    }

    private static int placeholders(String sql) {
        int n = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') {
                n++;
            }
        }
        return n;
    }

    // ================================================================= B1. real DAOs

    private static void daoChecks() throws Exception {
        installFakeDriver();
        System.out.println("[B1] NoticeDAO (real SQL and binding)");
        NoticeDAO nd = new NoticeDAO();

        responder = sql -> List.of(
                noticeRow(2, "Exam Notice", "Mid exam", LocalDate.of(2026, 9, 20), 1, "System Administrator"),
                noticeRow(1, "Old", "Orphan", LocalDate.of(2026, 1, 2), null, null));
        EXEC.clear();
        List<Notice> all = nd.findAll();
        check("findAll maps id, title, content, date, author", all.size() == 2 && all.get(0).getNoticeId() == 2
                && "Exam Notice".equals(all.get(0).getTitle()) && LocalDate.of(2026, 9, 20).equals(all.get(0).getNoticeDate())
                && Integer.valueOf(1).equals(all.get(0).getCreatedBy()) && "System Administrator".equals(all.get(0).getCreatedByName()));
        check("a notice whose author was deleted maps createdBy to null (not 0)", all.get(1).getCreatedBy() == null && all.get(1).getCreatedByName() == null);
        check("findAll orders newest first and uses no parameters",
                last().sql().contains("ORDER BY n.notice_date DESC") && last().params().isEmpty());

        EXEC.clear();
        nd.search("50%_off!");
        check("search escapes LIKE wildcards in the keyword and binds it for title AND content",
                last().params().size() == 2 && "%50!%!_off!!%".equals(last().params().get(1))
                        && "%50!%!_off!!%".equals(last().params().get(2)));
        check("search SQL declares the escape character and contains no keyword text",
                last().sql().contains("LIKE ? ESCAPE '!'") && !last().sql().contains("50") && placeholders(last().sql()) == 2);

        EXEC.clear();
        String evil = "x'); DROP TABLE notices;--";
        nd.search(evil);
        check("hostile search text is bound as a parameter, never concatenated into SQL",
                !last().sql().contains("DROP") && last().params().get(1).toString().contains("DROP"));

        EXEC.clear();
        nd.search("   ");
        check("blank search behaves as findAll (no WHERE, no parameters)", !last().sql().contains("WHERE") && last().params().isEmpty());

        EXEC.clear();
        nd.findById(9);
        check("findById binds the id", Integer.valueOf(9).equals(last().params().get(1)) && placeholders(last().sql()) == 1);
        responder = sql -> List.of();
        check("findById returns null when nothing matches", nd.findById(9) == null);

        responder = sql -> List.of(row("#1", 5));
        check("countAll reads the count", nd.countAll() == 5);

        responder = sql -> List.of();
        EXEC.clear();
        Notice n = notice("Title ' with quote", "Body", D);
        n.setCreatedBy(7);
        int id = nd.insert(n);
        check("insert returns and stores the generated id", id == 42 && n.getNoticeId() == 42);
        check("insert binds title, content, java.time.LocalDate and author in column order",
                last().sql().startsWith("INSERT INTO notices (title, content, notice_date, created_by) VALUES (?, ?, ?, ?)")
                        && "Title ' with quote".equals(last().params().get(1)) && "Body".equals(last().params().get(2))
                        && D.equals(last().params().get(3)) && Integer.valueOf(7).equals(last().params().get(4)));
        EXEC.clear();
        Notice noAuthor = notice("t", "c", D);
        nd.insert(noAuthor);
        check("insert without an author binds SQL NULL for created_by",
                last().params().get(4) instanceof NullParam p && p.sqlType == Types.INTEGER);

        EXEC.clear();
        Notice u = notice("New", "Newer", LocalDate.of(2026, 10, 1));
        u.setNoticeId(3);
        nd.update(u);
        check("update binds title, content, date, id and never touches created_by",
                !last().sql().contains("created_by") && "New".equals(last().params().get(1))
                        && LocalDate.of(2026, 10, 1).equals(last().params().get(3)) && Integer.valueOf(3).equals(last().params().get(4)));
        EXEC.clear();
        nd.delete(3);
        check("delete binds the id", last().sql().startsWith("DELETE FROM notices WHERE notice_id = ?")
                && Integer.valueOf(3).equals(last().params().get(1)));

        failNext = new SQLException("Table 'faculty_ams.notices' doesn't exist", "42S02", 1146);
        String missing = messageOf(nd::findAll);
        check("missing notices table -> DatabaseException telling the user to run migration 001",
                missing != null && missing.contains("001_notices_timetables.sql"));
        failNext = new SQLException("boom");
        check("any other SQL error on read -> DatabaseException (cause kept)", throwsA(DatabaseException.class, nd::findAll));
        failNext = new SQLException("Lock wait timeout exceeded");
        check("SQL error on insert -> DatabaseException", throwsA(DatabaseException.class, () -> nd.insert(notice("t", "c", D))));
        failNext = new SQLException("Lock wait timeout exceeded");
        check("SQL error on delete -> DatabaseException", throwsA(DatabaseException.class, () -> nd.delete(1)));
        failNext = null;

        System.out.println("[B2] TimeTableDAO (real SQL and binding)");
        TimeTableDAO td = new TimeTableDAO();
        responder = sql -> List.of(
                slotRow(1, 1, "MONDAY", "08:00:00", "10:00:00", "LT-01", "ICT2132", "OOP", 2, 1, 1, "ICT"),
                slotRow(2, 2, "TUESDAY", "10:00:00", "12:00:00", "LT-02", "ICT2113", "Data Structures", 2, 2, 1, "ICT"));
        EXEC.clear();
        List<TimeTable> slots = td.findAll(null, null);
        check("findAll maps slot and joined course/department fields", slots.size() == 2
                && slots.get(0).getDayOfWeek() == DayOfWeek.MONDAY && LocalTime.of(8, 0).equals(slots.get(0).getStartTime())
                && LocalTime.of(10, 0).equals(slots.get(0).getEndTime()) && "LT-01".equals(slots.get(0).getVenue())
                && "ICT2132".equals(slots.get(0).getCourseCode()) && "OOP".equals(slots.get(0).getCourseName())
                && slots.get(0).getSemester() == 1 && slots.get(1).getSemester() == 2 && slots.get(0).getAcademicLevel() == 2
                && slots.get(0).getDepartmentId() == 1 && "ICT".equals(slots.get(0).getDepartmentName()));
        check("unfiltered list has no parameters and orders Monday-first then by time",
                last().params().isEmpty() && last().sql().contains("ORDER BY FIELD(t.day_of_week,'MONDAY'")
                        && last().sql().contains("t.start_time"));
        check("slot department/semester come from the course (joins courses and departments)",
                last().sql().contains("JOIN courses c") && last().sql().contains("JOIN departments d")
                        && !last().sql().contains("t.semester") && !last().sql().contains("t.department"));

        EXEC.clear();
        td.findAll(3, 2);
        check("department and semester filters are bound in order",
                last().sql().contains("c.department_id = ?") && last().sql().contains("c.semester = ?")
                        && Integer.valueOf(3).equals(last().params().get(1)) && Integer.valueOf(2).equals(last().params().get(2)));

        EXEC.clear();
        td.search("lt_01%", 3, 2);
        check("keyword searches course code, name, venue and weekday with escaped wildcards (4 params) then filters",
                placeholders(last().sql()) == 6 && last().params().size() == 6
                        && "%lt!_01!%%".equals(last().params().get(1)) && "%lt!_01!%%".equals(last().params().get(4))
                        && Integer.valueOf(3).equals(last().params().get(5)) && Integer.valueOf(2).equals(last().params().get(6))
                        && last().sql().contains("t.venue LIKE ? ESCAPE '!'") && last().sql().contains("t.day_of_week LIKE ?"));
        EXEC.clear();
        td.search("x'); DROP TABLE timetables;--", null, null);
        check("hostile keyword is bound, not concatenated", !last().sql().contains("DROP") && last().params().size() == 4);
        EXEC.clear();
        td.search("  ", null, null);
        check("blank keyword adds no keyword filter", last().params().isEmpty() && !last().sql().contains("LIKE"));

        EXEC.clear();
        TimeTable byId = td.findById(2);
        check("findById binds the id and maps the joined slot",
                byId != null && byId.getTimetableId() == 1 && Integer.valueOf(2).equals(last().params().get(1))
                        && placeholders(last().sql()) == 1 && last().sql().contains("t.timetable_id = ?"));
        responder = sql -> List.of();
        check("findById returns null when nothing matches", td.findById(2) == null);
        responder = sql -> List.of(
                slotRow(1, 1, "MONDAY", "08:00:00", "10:00:00", "LT-01", "ICT2132", "OOP", 2, 1, 1, "ICT"));
        EXEC.clear();
        td.findConflicts(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "LT-01", 5, 8);
        check("findConflicts binds day, end, start, venue, course, excluded id (overlap test: start < otherEnd AND end > otherStart)",
                "MONDAY".equals(last().params().get(1)) && LocalTime.of(11, 0).equals(last().params().get(2))
                        && LocalTime.of(9, 0).equals(last().params().get(3)) && "LT-01".equals(last().params().get(4))
                        && Integer.valueOf(5).equals(last().params().get(5)) && Integer.valueOf(8).equals(last().params().get(6))
                        && last().sql().contains("t.start_time < ?") && last().sql().contains("t.end_time > ?")
                        && last().sql().contains("(t.venue = ? OR t.course_id = ?)") && last().sql().contains("t.timetable_id <> ?"));

        responder = sql -> List.of();
        EXEC.clear();
        TimeTable s = slot(1, DayOfWeek.FRIDAY, "13:00", "15:00", "LT-09");
        int sid = td.insert(s);
        check("insert returns and stores the generated id", sid == 42 && s.getTimetableId() == 42);
        check("insert binds course, day name, java.time.LocalTime start/end and venue",
                last().sql().startsWith("INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue) VALUES (?, ?, ?, ?, ?)")
                        && Integer.valueOf(1).equals(last().params().get(1)) && "FRIDAY".equals(last().params().get(2))
                        && LocalTime.of(13, 0).equals(last().params().get(3)) && LocalTime.of(15, 0).equals(last().params().get(4))
                        && "LT-09".equals(last().params().get(5)));
        EXEC.clear();
        s.setTimetableId(11);
        td.update(s);
        check("update binds the five fields then the id as 6th parameter",
                Integer.valueOf(11).equals(last().params().get(6)) && "LT-09".equals(last().params().get(5))
                        && last().sql().endsWith("WHERE timetable_id = ?"));
        EXEC.clear();
        td.delete(11);
        check("delete binds the id", Integer.valueOf(11).equals(last().params().get(1)));

        failNext = new SQLIntegrityConstraintViolationException("Cannot add or update a child row", "23000", 1452);
        String fk = messageOf(() -> td.insert(slot(99, DayOfWeek.FRIDAY, "13:00", "15:00", "LT-09")));
        check("insert for a vanished course -> friendly 'course no longer exists' message", fk != null && fk.contains("no longer exists"));
        failNext = new SQLException("Table 'faculty_ams.timetables' doesn't exist", "42S02", 1146);
        String missingTt = messageOf(() -> td.findAll(null, null));
        check("missing timetables table -> DatabaseException telling the user to run migration 001",
                missingTt != null && missingTt.contains("001_notices_timetables.sql"));
        failNext = new SQLException("boom");
        check("any other SQL error on update -> DatabaseException", throwsA(DatabaseException.class, () -> td.update(slot(1, DayOfWeek.FRIDAY, "13:00", "15:00", "X"))));
        failNext = null;
        EXEC.clear();
    }

    // ================================================================= B3. real Admin panels

    private static <T extends Component> List<T> all(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
        collect(root, type, out);
        return out;
    }

    private static <T extends Component> void collect(Component c, Class<T> type, List<T> out) {
        if (type.isInstance(c)) {
            out.add(type.cast(c));
        }
        if (c instanceof Container k) {
            for (Component child : k.getComponents()) {
                collect(child, type, out);
            }
        }
    }

    private static AbstractButton button(Component root, String text) {
        return all(root, AbstractButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElse(null);
    }

    /** The text field that follows the given label in the same container. */
    private static JTextComponent fieldAfter(Component root, String labelText) {
        for (JLabel label : all(root, JLabel.class)) {
            if (labelText.equals(label.getText())) {
                Container parent = label.getParent();
                Component[] kids = parent.getComponents();
                for (int i = 0; i < kids.length - 1; i++) {
                    if (kids[i] == label && kids[i + 1] instanceof JTextComponent tc) {
                        return tc;
                    }
                }
            }
        }
        return null;
    }

    private static JTextComponent fieldWithHint(Component root, String hint) {
        for (JTextComponent tc : all(root, JTextComponent.class)) {
            if (hint.equals(tc.getToolTipText())) {
                return tc;
            }
        }
        return null;
    }

    private static Exec lastWith(String fragment) {
        for (int i = EXEC.size() - 1; i >= 0; i--) {
            if (EXEC.get(i).sql().contains(fragment)) {
                return EXEC.get(i);
            }
        }
        return null;
    }

    private static final Function<String, List<Map<String, Object>>> ADMIN_DB = sql -> {
        if (sql.contains("FROM notices n")) {
            return List.of(
                    noticeRow(2, "Exam Notice", "Mid exam schedule will be released next week.", LocalDate.of(2026, 9, 20), 1, "System Administrator"),
                    noticeRow(1, "Library Notice", "New books are available.", LocalDate.of(2026, 8, 28), 1, "System Administrator"));
        }
        if (sql.contains("FROM timetables t")) {
            return List.of(
                    slotRow(1, 1, "MONDAY", "08:00:00", "10:00:00", "LT-01", "ICT2132", "OOP", 2, 1, 1, "Information and Communication Technology"),
                    slotRow(2, 2, "TUESDAY", "10:00:00", "12:00:00", "LT-02", "ICT2113", "Data Structures", 2, 1, 1, "Information and Communication Technology"));
        }
        if (sql.contains("FROM departments")) {
            return List.of(row("department_id", 1, "department_code", "DICT", "department_name", "Information and Communication Technology"));
        }
        if (sql.contains("FROM courses c")) {
            return List.of(row("course_id", 1, "course_code", "ICT2132", "course_name", "OOP", "credits", 3, "academic_level", 2,
                    "semester", 1, "course_type", "THEORY", "department_id", 1, "lecturer_id", null,
                    "department_name", "Information and Communication Technology", "lecturer_name", null));
        }
        if (sql.startsWith("SELECT COUNT(*)")) {
            return List.of(row("#1", 5));
        }
        return List.of();
    };

    private static JTable tableWithColumn(Component root, String column) {
        for (JTable t : all(root, JTable.class)) {
            for (int i = 0; i < t.getColumnCount(); i++) {
                if (column.equals(t.getColumnName(i))) {
                    return t;
                }
            }
        }
        return null;
    }

    private static List<String> textsOf(Component root) {
        List<String> texts = new ArrayList<>();
        for (JTextComponent tc : all(root, JTextComponent.class)) {
            texts.add(tc.getText());
        }
        return texts;
    }

    private static void panelChecks() {
        System.out.println("[B3] Admin Notice and Timetable screens (real panels, fake database)");
        Session.start(newUser(Role.ADMIN));
        responder = ADMIN_DB;

        EXEC.clear();
        NoticeManagementPanel np = new NoticeManagementPanel();
        np.refresh();
        JTable nt = tableWithColumn(np, "Posted by");
        check("notice screen lists the notices from the database", nt != null && nt.getRowCount() == 2);
        check("notice table shows ID, Title, Date, Posted by and Actions",
                nt != null && nt.getColumnCount() == 5 && "Posted by".equals(nt.getColumnName(3))
                        && "System Administrator".equals(String.valueOf(nt.getValueAt(0, 3))));
        check("notice row shows its title and date",
                nt != null && "Exam Notice".equals(String.valueOf(nt.getValueAt(0, 1)))
                        && "2026-09-20".equals(String.valueOf(nt.getValueAt(0, 2))));
        JTextComponent nSearch = fieldWithHint(np, "Search notices...");
        check("notice screen has a search box", nSearch != null);
        nSearch.setText("exam");
        EXEC.clear();
        ((javax.swing.JTextField) nSearch).postActionEvent();
        Exec q = lastWith("FROM notices n");
        check("Search sends the keyword (as a bound parameter) for title and content",
                q != null && q.params().size() == 2 && "%exam%".equals(q.params().get(1)));
        nSearch.setText("");
        EXEC.clear();
        ((javax.swing.JTextField) nSearch).postActionEvent();
        q = lastWith("FROM notices n");
        check("clearing the search box reloads without a keyword",
                q != null && q.params().isEmpty());
        check("notice screen has an Add Notice button", button(np, "Add Notice") != null);

        EXEC.clear();
        TimeTableManagementPanel tp = new TimeTableManagementPanel();
        tp.refresh();
        JTable tt = tableWithColumn(tp, "Lecture Hall");
        check("timetable screen lists the slots from the database", tt != null && tt.getRowCount() == 2);
        check("timetable row shows day, time range, course, department, semester and hall",
                tt != null && "Monday".equals(String.valueOf(tt.getValueAt(0, 1))) && "08:00 - 10:00".equals(String.valueOf(tt.getValueAt(0, 2)))
                        && "ICT2132".equals(String.valueOf(tt.getValueAt(0, 3))) && "1".equals(String.valueOf(tt.getValueAt(0, 6)))
                        && "LT-01".equals(String.valueOf(tt.getValueAt(0, 7))));
        List<JComboBox> combos = all(tp, JComboBox.class);
        check("department filter is loaded from the database (All + 1 department)",
                combos.stream().anyMatch(c -> c.getItemCount() == 2));

        JTextComponent tSearch = fieldWithHint(tp, "Search timetable...");
        check("timetable screen has a search box", tSearch != null);
        tSearch.setText("lt-01");
        EXEC.clear();
        ((javax.swing.JTextField) tSearch).postActionEvent();
        q = lastWith("FROM timetables t");
        check("search with a keyword searches course code, name, venue and day",
                q != null && q.params().size() == 4 && "%lt-01%".equals(q.params().get(1)));
        tSearch.setText("");
        EXEC.clear();
        ((javax.swing.JTextField) tSearch).postActionEvent();
        q = lastWith("FROM timetables t");
        check("clearing the search box reloads without keyword, department or semester",
                q != null && q.params().isEmpty());
        check("timetable screen has an Add Timetable button", button(tp, "Add Timetable") != null);
        Session.end();
    }

    // ================================================================= B4. shell navigation

    private static void shellChecks() {
        System.out.println("[B4] Admin shell navigation (shared AppShell)");
        User admin = newUser(Role.ADMIN);
        Session.start(admin);
        responder = ADMIN_DB;
        EXEC.clear();
        AdminShell shell = new AdminShell(null, admin);
        check("shell sidebar has Notices and Timetables entries",
                button(shell, "Notices") != null && button(shell, "Timetables") != null);
        check("shell pages are not loaded until opened (Notices query not yet run)", lastWith("FROM notices n") == null);
        button(shell, "Notices").doClick();
        JTable nt = tableWithColumn(shell, "Posted by");
        check("opening Notices from the sidebar loads the notice list", nt != null && nt.getRowCount() == 2);
        button(shell, "Timetables").doClick();
        JTable tt = tableWithColumn(shell, "Lecture Hall");
        check("opening Timetables from the sidebar loads the timetable", tt != null && tt.getRowCount() == 2);
        check("Notice and Timetable pages carry their headings",
                all(shell, JLabel.class).stream().anyMatch(l -> "Notice Management".equals(l.getText()))
                        && all(shell, JLabel.class).stream().anyMatch(l -> "Timetable Management".equals(l.getText())));
        Session.end();

        for (Role role : new Role[] {Role.LECTURER, Role.TECHNICAL_OFFICER, Role.STUDENT}) {
            User u = newUser(role);
            Session.start(u);
            check(role + " cannot open the Admin shell (so cannot reach Notice/Timetable management)",
                    throwsA(IllegalStateException.class, () -> new AdminShell(null, u)));
        }
        Session.end();
    }
}