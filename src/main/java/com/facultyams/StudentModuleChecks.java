package com.facultyams;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.model.CourseAttendance;
import com.facultyams.model.CourseOffering;
import com.facultyams.model.MedicalRequest;
import com.facultyams.model.MedicalStatus;
import com.facultyams.model.User;
import com.facultyams.model.student.StudentMaterial;
import com.facultyams.model.student.StudentNotice;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Session;
import com.facultyams.service.DataService;
import com.facultyams.service.DbStudentAttendanceService;
import com.facultyams.service.DbStudentMedicalService;
import com.facultyams.service.EnrollmentResult;
import com.facultyams.service.EnrollmentService;
import com.facultyams.service.ResultService;
import com.facultyams.service.ResultService.Summary;
import com.facultyams.service.StudentNoticeService;
import com.facultyams.service.profile.DbStudentProfileService;
import com.facultyams.service.timetable.FileTimetableService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Database-backed checks for the undergraduate module. Plain {@code main} so it runs without JUnit:
 * set FOTAMS_DB_URL / FOTAMS_DB_USER / FOTAMS_DB_PASSWORD, load schema.sql, sample_data.sql, database/migrations/001-008
 * and then database/test/student_checks_fixture.sql (the enrollments, marks, materials and medical request the checks expect), and run.
 * It restores every row it changes.
 */
public final class StudentModuleChecks {
    private static int passed;
    private static int failed;

    private StudentModuleChecks() { }

    interface Body { void run() throws Exception; }

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS  " + name); }
        else { failed++; System.out.println("FAIL  " + name); }
    }

    static void expectThrows(String name, Class<? extends Throwable> type, Body body) {
        try {
            body.run();
            failed++;
            System.out.println("FAIL  " + name + " (no exception)");
        } catch (Throwable t) {
            boolean ok = type.isInstance(t);
            if (ok) { passed++; System.out.println("PASS  " + name + "  -> " + t.getClass().getSimpleName() + ": " + t.getMessage()); }
            else { failed++; System.out.println("FAIL  " + name + " (got " + t + ")"); }
        }
    }

    static void login(int id) throws Exception {
        User u = new UserDAO().findById(id);
        Session.start(u);
        // services are per-session singletons; they re-bind when another student logs in
    }

    static void sql(String statement) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(statement);
        }
    }

    static String scalar(String query) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(query)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    static CourseOffering offering(String code) {
        return new CourseOffering(code, code, 2, "", "", "", "", false, "");
    }

    public static void main(String[] args) throws Exception {
        authorization();
        results();
        attendance();
        enrollment();
        materials();
        notices();
        profile();
        medical();
        timetable();
        System.out.println();
        System.out.println("Checks passed: " + passed + ", failed: " + failed);
        DatabaseConnection.closeAll();
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------- authorization
    static void authorization() throws Exception {
        System.out.println("== Authorization / ownership");
        Session.end();
        expectThrows("no session: results are refused", AuthorizationException.class, () -> ResultService.getInstance());
        expectThrows("no session: enrollment is refused", AuthorizationException.class, () -> new EnrollmentService().getOffered());
        expectThrows("no session: notices are refused", AuthorizationException.class, () -> StudentNoticeService.getInstance());
        login(2); // a lecturer
        expectThrows("lecturer session cannot use student results", AuthorizationException.class, () -> ResultService.getInstance());
        expectThrows("lecturer session cannot enroll", AuthorizationException.class, () -> new EnrollmentService().getOffered());
        login(11);
        DbStudentAttendanceService att = new DbStudentAttendanceService();
        expectThrows("student 11 cannot read attendance of 12", AuthorizationException.class, () -> att.summary("12"));
        expectThrows("student 11 cannot read sessions of 12", AuthorizationException.class,
                () -> att.sessionsBetween("12", LocalDate.now().minusDays(30), LocalDate.now()));
        expectThrows("student 11 cannot read medical records of 12", AuthorizationException.class,
                () -> new DbStudentMedicalService(att).list("12"));
        check("student 11 can read own attendance", !att.summary("11").isEmpty());
        Session.end();
        User viaLogin = new com.facultyams.service.AuthService().login("tg1001", "student123");
        check("team AuthService login of tg1001 returns a Student", viaLogin instanceof com.facultyams.model.Student);
    }

    // ---------------------------------------------------------------- results
    static void results() throws Exception {
        System.out.println("== Results / GPA");
        login(11);
        Summary s = ResultService.getInstance().getSummary();
        check("tg1001 CGPA = (4.0*3 + 3.3*2 + 4.0*2)/7 = 3.80 (got " + s.cgpa() + ")", s.cgpa().toPlainString().equals("3.80"));
        check("tg1001 graded courses = 3", s.gradedCourses() == 3);
        check("tg1001 credits earned = 7 / graded credits = 7 / registered = 9",
                s.earnedCredits() == 7 && s.gradedCredits() == 7 && s.plannedCredits() == 9);
        var sems = ResultService.getInstance().getSemesters();
        check("tg1001 has exactly one semester (Level 2, Sem 1)", sems.size() == 1 && sems.get(0).year() == 2 && sems.get(0).semester() == 1);
        check("tg1001 ICT2113 grade A / 80.4", sems.get(0).courses().stream()
                .anyMatch(c -> c.code().equals("ICT2113") && c.grade().equals("A") && c.scoreText().equals("80.4")));
        check("tg1001 ICT2152 (no marks) is PENDING with no grade point",
                sems.get(0).courses().stream().anyMatch(c -> c.code().equals("ICT2152")
                        && c.status() == ResultService.ResultStatus.PENDING && !c.hasMarks()));

        login(12);
        Summary s12 = ResultService.getInstance().getSummary();
        check("tg1002 CGPA = (2.3*3 + 2.0*2)/5 = 2.18 (got " + s12.cgpa() + ")", s12.cgpa().toPlainString().equals("2.18"));
        check("tg1002 ICT2122 is FAIL (CA below 40 -> not eligible) despite grade points 2.0",
                ResultService.getInstance().getSemesters().get(0).courses().stream()
                        .anyMatch(c -> c.code().equals("ICT2122") && c.status() == ResultService.ResultStatus.FAIL));
        check("tg1002 earned credits = 3 (failed course not counted)", s12.earnedCredits() == 3);

        login(16);
        check("tg1006 (no enrollments) has no results", ResultService.getInstance().getSemesters().isEmpty()
                && !ResultService.getInstance().getSummary().hasResults());
        login(14);
        check("tg1004 (enrolled, no marks) has pending course and no GPA",
                !ResultService.getInstance().getSummary().hasResults()
                        && !ResultService.getInstance().getSemesters().isEmpty());
    }

    // ---------------------------------------------------------------- attendance
    static void attendance() throws Exception {
        System.out.println("== Attendance (cross-checked against an independent SQL calculation)");
        for (int id : new int[]{11, 12, 13, 14, 15, 16}) {
            login(id);
            List<CourseAttendance> rows = new DbStudentAttendanceService().summary(String.valueOf(id));
            String present = scalar("SELECT COUNT(*) FROM session_attendance WHERE user_id=" + id + " AND LOWER(status)='present'");
            String med = scalar("SELECT COUNT(*) FROM medical m JOIN session_attendance sa ON sa.attendance_id=m.attendance_id "
                    + "WHERE sa.user_id=" + id + " AND LOWER(m.approval_status)='approved'");
            int comps = Integer.parseInt(scalar("SELECT COUNT(DISTINCT component_id) FROM session_attendance WHERE user_id=" + id));
            double expected = comps == 0 ? 0 : 100.0 * (Integer.parseInt(present) + Integer.parseInt(med)) / (15.0 * comps);
            if (rows.isEmpty()) {
                check("tg100" + (id - 10) + " has no attendance rows when nothing is recorded (components=" + comps + ")", comps == 0);
                continue;
            }
            CourseAttendance c = rows.stream().filter(CourseAttendance::hasRecords).findFirst().orElse(rows.get(0));
            double got = c.percent();
            check("user " + id + " ICT2113 attendance " + String.format("%.1f", got) + "% == SQL " + String.format("%.1f", expected) + "%",
                    Math.abs(got - expected) < 0.05 || Math.abs(got * 100 - expected) < 0.05);
        }
    }

    // ---------------------------------------------------------------- enrollment
    static void enrollment() throws Exception {
        System.out.println("== Enrollment rules");
        login(12);
        EnrollmentService svc = new EnrollmentService();
        String dept = DataService.getInstance().getCurrentStudent().getDepartment();
        check("offered courses are all from the student's own department",
                svc.getOffered().stream().allMatch(o -> dept.equals(o.getDepartment())) && !svc.getOffered().isEmpty());
        check("ENG2122 (other department) is not offered", svc.getOffered().stream().noneMatch(o -> o.getCode().equals("ENG2122")));
        EnrollmentResult other = svc.enroll(offering("ENG2122"));
        check("enrolling in another department's course is refused: " + other.message(), !other.success());
        EnrollmentResult dup = svc.enroll(offering("ICT2113"));
        check("duplicate enrollment is refused: " + dup.message(), !dup.success());
        EnrollmentResult drop1 = svc.drop(offering("ICT2113"));
        check("cannot drop a course that has marks/attendance: " + drop1.message(), !drop1.success());

        EnrollmentResult ok = svc.enroll(offering("ICT2152"));
        check("valid enrollment succeeds: " + ok.message(), ok.success());
        check("enrollment is stored in the database",
                "1".equals(scalar("SELECT COUNT(*) FROM enrollments WHERE student_id=12 AND course_id=5 AND status='ENROLLED'")));
        check("enrolled credits now 9", svc.getTotalCredits() == 9);
        EnrollmentResult again = svc.enroll(offering("ICT2152"));
        check("second enrollment in the same course is refused: " + again.message(), !again.success());
        EnrollmentResult dropped = svc.drop(offering("ICT2152"));
        check("drop of a course without records succeeds: " + dropped.message(), dropped.success());
        check("row is marked DROPPED, not duplicated",
                "1".equals(scalar("SELECT COUNT(*) FROM enrollments WHERE student_id=12 AND course_id=5 AND status='DROPPED'")));
        check("re-enrolling after a drop works", svc.enroll(offering("ICT2152")).success());
        check("still exactly one row for the pair", "1".equals(scalar("SELECT COUNT(*) FROM enrollments WHERE student_id=12 AND course_id=5")));
        svc.drop(offering("ICT2152"));

        // 20-credit cap: three 4-credit test courses bring tg1002 to 7 + 12 = 19, so a further 2 credits is 21 > 20
        for (int i = 1; i <= 3; i++) {
            sql("INSERT INTO courses (course_code, course_name, credits, academic_level, semester, course_type, department_id) "
                    + "VALUES ('ICT900" + i + "','Credit cap test " + i + "',4,2,1,'THEORY',1)");
            sql("INSERT INTO enrollments (student_id, course_id) SELECT 12, course_id FROM courses WHERE course_code='ICT900" + i + "'");
        }
        EnrollmentResult cap = svc.enroll(offering("ICT2152"));
        check("20-credit limit enforced (19 + 2 > 20): " + cap.message(), !cap.success());
        sql("DELETE FROM enrollments WHERE course_id IN (SELECT course_id FROM courses WHERE course_code LIKE 'ICT900_')");
        sql("DELETE FROM courses WHERE course_code LIKE 'ICT900_'");

        // passed course cannot be repeated
        sql("UPDATE enrollments SET status='DROPPED' WHERE student_id=12 AND course_id=1");
        EnrollmentResult passedAgain = svc.enroll(offering("ICT2113"));
        check("already-passed course cannot be enrolled again: " + passedAgain.message(), !passedAgain.success());
        sql("UPDATE enrollments SET status='ENROLLED' WHERE student_id=12 AND course_id=1");

        sql("DELETE FROM enrollments WHERE student_id=12 AND course_id=5");   // leave no DROPPED test row behind

        // timetable clash
        login(11);
        EnrollmentService svc11 = new EnrollmentService();
        EnrollmentResult clash = svc11.enroll(offering("ICT2142"));
        check("weekly timetable clash (ICT2142 vs ICT2122 on Wednesday) is refused: " + clash.message(), !clash.success());
        check("no row was written for the refused clash",
                "0".equals(scalar("SELECT COUNT(*) FROM enrollments WHERE student_id=11 AND course_id=4")));

    }

    // ---------------------------------------------------------------- materials
    static void materials() throws Exception {
        System.out.println("== Course materials");
        login(12);
        DataService ds = DataService.getInstance();
        check("tg1002 sees materials of ICT2113 (3)", ds.getMaterialsForCourse("ICT2113").size() == 3);
        check("tg1002 sees materials of ICT2122 (1)", ds.getMaterialsForCourse("ICT2122").size() == 1);
        check("tg1002 sees nothing for ICT2142 (not enrolled)", ds.getMaterialsForCourse("ICT2142").isEmpty());
        check("tg1002 sees nothing for ENG2122 (other department, not enrolled)", ds.getMaterialsForCourse("ENG2122").isEmpty());
        StudentMaterial hidden = null;
        try { hidden = ds.getMaterial(5); } catch (RuntimeException ex) { /* refusing is fine too */ }
        check("direct lookup of a material of a non-enrolled course is refused (id 5)", hidden == null);
        StudentMaterial own = ds.getMaterial(1);
        check("direct lookup of an enrolled course's material works (id 1) and is a link", own != null && own.isLink());
        login(16);
        check("tg1006 (no enrollments) has no materials", DataService.getInstance().getMaterialsForCourse("ICT2113").isEmpty());
    }

    // ---------------------------------------------------------------- notices
    static void notices() throws Exception {
        System.out.println("== Notices");
        sql("DELETE FROM notice_reads WHERE user_id IN (11,12)");
        login(11);
        StudentNoticeService n = StudentNoticeService.getInstance();
        List<StudentNotice> list = n.getNotices();
        int total = Integer.parseInt(scalar("SELECT COUNT(*) FROM notices"));
        check("all " + total + " notices are listed with a readable date", list.size() == total
                && list.stream().allMatch(x -> x.getDate() != null && !x.getDate().isBlank()));
        int unread = n.unreadCount();
        check("all notices unread at first", unread == total);
        n.markAsRead(list.get(0));
        check("marking one notice read lowers the unread count", n.unreadCount() == total - 1);
        n.markAsRead(list.get(0));
        check("marking twice does not double count", n.unreadCount() == total - 1);
        login(12);
        check("another student's read state is independent", StudentNoticeService.getInstance().unreadCount() == total);
        sql("DELETE FROM notice_reads WHERE user_id IN (11,12)");
    }

    // ---------------------------------------------------------------- profile
    static void profile() throws Exception {
        System.out.println("== Profile");
        login(13);
        DbStudentProfileService p = new DbStudentProfileService();
        String name = scalar("SELECT full_name FROM users WHERE user_id=13");
        String reg = scalar("SELECT registration_no FROM students WHERE user_id=13");
        String phone = scalar("SELECT contact_number FROM users WHERE user_id=13");
        expectThrows("invalid phone number is rejected", RuntimeException.class, () -> p.updateContactNumber("abc"));
        expectThrows("blank phone number is rejected", RuntimeException.class, () -> p.updateContactNumber("  "));
        p.updateContactNumber("0771234567");
        check("valid phone is saved", "0771234567".equals(scalar("SELECT contact_number FROM users WHERE user_id=13")));
        check("name and registration number are untouched", name.equals(scalar("SELECT full_name FROM users WHERE user_id=13"))
                && reg.equals(scalar("SELECT registration_no FROM students WHERE user_id=13")));
        check("profile reads the logged-in student's own record", p.getProfile().getUserId() == 13);
        sql("UPDATE users SET contact_number='" + phone + "' WHERE user_id=13");

        login(14);
        String oldHash = scalar("SELECT password_hash FROM users WHERE user_id=14");
        sql("UPDATE users SET password_hash='" + PasswordUtil.hashPassword("Old#Pass123") + "' WHERE user_id=14");
        DbStudentProfileService p14 = new DbStudentProfileService();
        expectThrows("wrong current password is rejected", RuntimeException.class,
                () -> p14.changePassword("Wrong#Pass1".toCharArray(), "New#Pass1234".toCharArray()));
        expectThrows("weak new password is rejected", RuntimeException.class,
                () -> p14.changePassword("Old#Pass123".toCharArray(), "abc".toCharArray()));
        p14.changePassword("Old#Pass123".toCharArray(), "New#Pass1234".toCharArray());
        check("new password verifies and is stored as a hash",
                PasswordUtil.verifyPassword("New#Pass1234", scalar("SELECT password_hash FROM users WHERE user_id=14")));
        sql("UPDATE users SET password_hash='" + oldHash + "' WHERE user_id=14");
    }

    // ---------------------------------------------------------------- medical
    static void medical() throws Exception {
        System.out.println("== Medical");
        login(13);
        DbStudentAttendanceService att = new DbStudentAttendanceService();
        Path tmp = Files.createTempDirectory("med-test");
        DbStudentMedicalService med = new DbStudentMedicalService(tmp, att);
        check("tg1003 sees no one else's requests (MED-SAMPLE-0001 belongs to another student)",
                med.list("13").stream().noneMatch(r -> "MED-SAMPLE-0001".equals(r.referenceNo())));
        login(12);
        DbStudentMedicalService med12 = new DbStudentMedicalService(tmp, att);
        check("tg1002 sees own pending request MED-SAMPLE-0001",
                med12.list("12").stream().anyMatch(r -> "MED-SAMPLE-0001".equals(r.id()) && r.isPending()));

        login(13);
        String absent = scalar("SELECT sa.attendance_id FROM session_attendance sa WHERE sa.user_id=13 AND LOWER(sa.status)='absent' "
                + "AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id=sa.attendance_id) ORDER BY sa.attendance_id LIMIT 1");
        String present = scalar("SELECT attendance_id FROM session_attendance WHERE user_id=13 AND LOWER(status)='present' LIMIT 1");
        String others = scalar("SELECT attendance_id FROM session_attendance WHERE user_id=12 AND LOWER(status)='absent' "
                + "AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id=session_attendance.attendance_id) LIMIT 1");
        LocalDate today = LocalDate.now();
        java.util.function.BiFunction<List<String>, String, MedicalRequest> draft = (ids, reason) -> new MedicalRequest(
                null, null, "13", today, today, ids, reason, "Dr. Test", "MC-1", "notes", List.of(), null, MedicalStatus.PENDING, null);

        expectThrows("empty reason is rejected", IllegalArgumentException.class, () -> med.submit(draft.apply(List.of(absent), " "), List.of()));
        expectThrows("no session selected is rejected", IllegalArgumentException.class, () -> med.submit(draft.apply(List.of(), "Fever"), List.of()));
        expectThrows("a PRESENT session cannot be excused", IllegalArgumentException.class, () -> med.submit(draft.apply(List.of(present), "Fever"), List.of()));
        expectThrows("another student's session is refused", AuthorizationException.class, () -> med.submit(draft.apply(List.of(others), "Fever"), List.of()));
        File bad = File.createTempFile("cert", ".exe");
        expectThrows("unsupported file type is rejected", IllegalArgumentException.class, () -> med.submit(draft.apply(List.of(absent), "Fever"), List.of(bad)));
        List<File> four = new ArrayList<>();
        for (int i = 0; i < 4; i++) four.add(File.createTempFile("cert" + i, ".pdf"));
        expectThrows("more than 3 files is rejected", IllegalArgumentException.class, () -> med.submit(draft.apply(List.of(absent), "Fever"), four));
        check("nothing was written by the rejected requests",
                "0".equals(scalar("SELECT COUNT(*) FROM medical WHERE attendance_id=" + absent)));

        File pdf = File.createTempFile("certificate", ".pdf");
        Files.writeString(pdf.toPath(), "%PDF-1.4 test");
        MedicalRequest saved = med.submit(draft.apply(List.of(absent), "Viral fever"), List.of(pdf));
        check("valid request is saved as PENDING with a reference " + saved.referenceNo(), saved.isPending() && saved.referenceNo() != null);
        check("row stored in the database, unreviewed",
                "1".equals(scalar("SELECT COUNT(*) FROM medical WHERE attendance_id=" + absent + " AND LOWER(approval_status)='pending' AND reviewed_by IS NULL")));
        check("certificate file copied under the student's own folder", Files.exists(tmp.resolve("13").resolve(saved.id())));
        expectThrows("a second request for the same session is rejected", IllegalArgumentException.class,
                () -> med.submit(draft.apply(List.of(absent), "Again"), List.of()));
        login(12);
        expectThrows("another student cannot withdraw it", IllegalArgumentException.class, () -> med12.withdraw(saved.id()));
        login(13);
        med.withdraw(saved.id());
        check("owner can withdraw a pending request", "0".equals(scalar("SELECT COUNT(*) FROM medical WHERE attendance_id=" + absent)));
        check("approved requests (of anyone) cannot be withdrawn", expectNoWithdraw(med)
                && "1".equals(scalar("SELECT COUNT(*) FROM medical WHERE medical_id=1")));
        sql("DELETE FROM medical WHERE attendance_id=" + absent);
    }

    private static boolean expectNoWithdraw(DbStudentMedicalService med) {
        try {
            med.withdraw("MED-1");   // an approved request of another student
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    // ---------------------------------------------------------------- timetable
    static void timetable() throws Exception {
        System.out.println("== Timetable");
        login(11);
        FileTimetableService t = new FileTimetableService(Files.createTempFile("tt", ".properties"));
        var sessions = t.getSessions();
        check("tg1001 timetable lists the enrolled courses' slots with venues", sessions.size() >= 4
                && sessions.stream().allMatch(s -> s.getVenue() != null));
        check("tg1001 has no clashing slots", t.findAcademicClashIds().isEmpty());
        sql("INSERT INTO enrollments (student_id, course_id) VALUES (11, 4)");
        check("a clash that exists in the data is detected (course 2 vs 4 on Wednesday)", t.findAcademicClashIds().size() == 2);
        sql("DELETE FROM enrollments WHERE student_id=11 AND course_id=4");
        login(16);
        check("tg1006 (no enrollments) has an empty timetable",
                new FileTimetableService(Files.createTempFile("tt", ".properties")).getSessions().isEmpty());
    }
}
