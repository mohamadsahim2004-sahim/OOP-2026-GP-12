package com.facultyams;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the database has everything the Lecturer module needs and says exactly what is
 * wrong and how to fix it. Read-only: it only runs SELECT statements.
 *
 * Run (after building):  java -cp "target/classes;target/dependency/*" com.facultyams.LecturerDbCheck
 * (on Linux / macOS use ":" instead of ";"). Exit code 0 = nothing failed, 1 = something failed.
 */
public final class LecturerDbCheck {

    private static final String FIX_MIGRATION =
            "Run database/migrations/005_lecturer_module.sql (it is safe to run again).";

    private record Need(String table, String columns, String hint) {
    }

    private static final List<Need> NEEDS = List.of(
            new Need("users", "user_id, username, password_hash, full_name, email, role, contact_number, department_id, is_active",
                    "Run database/schema.sql, then database/sample_data.sql."),
            new Need("lecturers", "user_id, designation", "Run database/schema.sql."),
            new Need("students", "user_id, registration_no, batch, student_status", "Run database/schema.sql."),
            new Need("courses", "course_id, course_code, course_name, credits, academic_level, semester, course_type, department_id, lecturer_id",
                    "Run database/schema.sql."),
            new Need("course_component", "component_id, course_id, type", "Run database/migrations/003_attendance_medical.sql."),
            new Need("session_attendance", "attendance_id, user_id, component_id, session_no, date, status",
                    "Run database/migrations/003_attendance_medical.sql."),
            new Need("medical", "medical_id, attendance_id, reviewed_by, submitted_date, approval_status",
                    "Run database/migrations/003_attendance_medical.sql."),
            new Need("enrollments", "student_id, course_id", FIX_MIGRATION),
            new Need("evaluation_component", "eval_component_id, course_id, component_name, category, max_marks, weightage", FIX_MIGRATION),
            new Need("component_marks", "student_id, eval_component_id, marks_obtained, entered_by", FIX_MIGRATION),
            new Need("marks", "marksid, student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at",
                    FIX_MIGRATION + " If `marks` belongs to ANOTHER module (columns such as course_code / score), "
                            + "005 renames it to marks_other_module so the Lecturer table can be created."),
            new Need("course_materials", "materialID, courseID, description, notes, referenceText", FIX_MIGRATION));

    private static int fails;
    private static int warns;

    private LecturerDbCheck() {
    }

    private static void pass(String text) {
        System.out.println("  PASS  " + text);
    }

    private static void warn(String text, String hint) {
        warns++;
        System.out.println("  WARN  " + text + "\n          -> " + hint);
    }

    private static void fail(String text, String hint) {
        fails++;
        System.out.println("  FAIL  " + text + "\n          -> " + hint);
    }

    public static void main(String[] args) {
        DatabaseConnection.ConnectionInfo info = DatabaseConnection.getConnectionInfo();
        System.out.println("Lecturer database check for " + info.database() + " on " + info.host() + ":" + info.port()
                + " as user '" + info.user() + "'");

        try (Connection con = DatabaseConnection.getConnection()) {
            pass("connected to the database");
            checkTables(con);
            if (fails == 0) {
                checkData(con);
            }
        } catch (DatabaseException | SQLException e) {
            fail("cannot connect: " + e.getMessage(),
                    "Start MySQL and check src/main/resources/db.properties (db.url, db.user, db.password).");
        }

        System.out.println("\nRESULT: " + fails + " failed, " + warns + " warning(s)"
                + (fails == 0 ? " - the database is ready for the Lecturer module." : " - fix the FAIL lines above, then run this again."));
        System.exit(fails == 0 ? 0 : 1);
    }

    private static void checkTables(Connection con) {
        for (Need need : NEEDS) {
            String sql = "SELECT " + need.columns() + " FROM " + need.table() + " WHERE 1 = 0";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                pass("table " + need.table() + " has the columns the Lecturer module uses");
            } catch (SQLException e) {
                fail("table " + need.table() + ": " + e.getMessage(), need.hint());
            }
        }
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT head_user_id FROM departments WHERE 1 = 0")) {
            pass("departments.head_user_id exists (needed by the Admin module)");
        } catch (SQLException e) {
            warn("departments.head_user_id is missing: " + e.getMessage(),
                    "Run database/migrations/002_department_head.sql (Admin module only; the Lecturer module does not need it).");
        }
    }

    private static void checkData(Connection con) throws SQLException {
        List<String> cannotLogIn = new ArrayList<>();
        int lecturers = 0;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT username, password_hash FROM users WHERE role = 'LECTURER'")) {
            while (rs.next()) {
                lecturers++;
                String hash = rs.getString(2);
                if (hash == null || !hash.startsWith("pbkdf2$")) {
                    cannotLogIn.add(rs.getString(1));
                }
            }
        }
        if (lecturers == 0) {
            warn("there is no lecturer account", "Run database/sample_data.sql or create a lecturer in the Admin module.");
        } else if (cannotLogIn.isEmpty()) {
            pass(lecturers + " lecturer account(s) with a valid password hash");
        } else {
            fail("these lecturers cannot log in (password is not a PBKDF2 hash): " + cannotLogIn,
                    "Run database/migrations/005_lecturer_module.sql (repairs lec.perera) or reset the password in the Admin module.");
        }

        int withoutCourse = scalar(con, "SELECT COUNT(*) FROM users u WHERE u.role = 'LECTURER' "
                + "AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.lecturer_id = u.user_id)");
        if (withoutCourse > 0) {
            warn(withoutCourse + " lecturer(s) have no assigned course (their screens will be empty)",
                    "Assign courses in the Admin module (Course Management).");
        }
        int enrolled = scalar(con, "SELECT COUNT(*) FROM enrollments");
        if (enrolled == 0) {
            warn("no student is enrolled in any course, so lecturers will see no students",
                    "Run database/migrations/006_lecturer_test_data.sql, or insert rows into `enrollments`.");
        } else {
            pass(enrolled + " enrollment(s)");
        }
        int coursesWithoutScheme = scalar(con, "SELECT COUNT(*) FROM courses c WHERE c.lecturer_id IS NOT NULL "
                + "AND NOT EXISTS (SELECT 1 FROM evaluation_component e WHERE e.course_id = c.course_id)");
        if (coursesWithoutScheme > 0) {
            warn(coursesWithoutScheme + " assigned course(s) have no evaluation components, so marks cannot be entered yet",
                    "Open Evaluation Components in the app, or run database/migrations/006_lecturer_test_data.sql for sample schemes.");
        }
        int duplicates = scalar(con, "SELECT COUNT(*) FROM (SELECT 1 FROM marks GROUP BY student_id, course_id "
                + "HAVING COUNT(*) > 1) d");
        if (duplicates > 0) {
            fail(duplicates + " student/course pair(s) have more than one row in `marks`",
                    "Remove the duplicate rows by hand, then run 005 again so the unique key can be added.");
        } else {
            pass("no duplicate result rows in `marks`");
        }
    }

    private static int scalar(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}