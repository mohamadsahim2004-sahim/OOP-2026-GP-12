-- =============================================================================
-- Migration 006 - DEMO data for the Lecturer module (enrollments, assessment schemes, component marks)
--
-- DEMO DATA. Additive and repeatable: every row is inserted only if it does not exist yet; nothing is
-- updated or deleted. Rows are attached ONLY to the demo accounts (e-mail ending @fot.example.lk), never to a
-- real student, and course-level rows (assessment scheme, materials) only to a course that has none yet.
--
-- CHANGED from earlier versions, which enrolled EVERY student of the database (real ones too) in every
-- Level 2 Semester 1 course of every department, used fixed course ids 1 and 2 and `INSERT IGNORE`
-- (which hides failures), and used a Quiz marked out of 20 (the assignment requires all marks out of 100):
--   * only the demo undergraduates are enrolled, and only in the DICT Level 2 Semester 1 courses
--     (the department they belong to; the Undergraduate module offers courses by department),
--   * courses are found by course_code,
--   * an assessment scheme is added only to a course that has NO evaluation components yet,
--   * every component is marked out of 100.
--
-- THE ASSESSMENT SCHEMES BELOW ARE PLACEHOLDERS (CA 40% / final examination 60%), not the criteria of
-- the course coordinators. Replace them in the Lecturer module (Evaluation Components) with the
-- official criteria; the rules (CA >= 40 %, grades) are applied by the application, not by this script.
--
--   ICT2113 (Theory + Practical): Quiz 10, Mid Exam 10, Assignment 10, Practical 10, Final Exam 60
--   ICT2122 (Theory):             Test 20, Assignment 20, Final Exam 60
--
-- Demo lecturer logins: lec.perera (ICT2113, ICT2152)  lec.silva (ICT2122, ICT2132)  - password lecturer123
--
-- Needs (run first): schema.sql, sample_data.sql, migrations 005 and 007.  Run 004 before 008.
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/006_lecturer_test_data.sql"
-- =============================================================================

USE faculty_ams;

CREATE TABLE IF NOT EXISTS schema_migrations (
    migration_id     VARCHAR(100) NOT NULL,
    description      VARCHAR(255) NOT NULL,
    first_applied_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    run_count        INT          NOT NULL DEFAULT 1,
    applied_by       VARCHAR(100) NULL,
    PRIMARY KEY (migration_id)
) ENGINE = InnoDB;

SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table users does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table students does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__students_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table courses does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__courses_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table evaluation_component does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__evaluation_component_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table component_marks does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__component_marks_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME IN ('status', 'dropped_at')) < 2);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments lacks column(s) status, dropped_at. Run the migrations 003, 005, 007, 009, 010 BEFORE this demo-data script.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__run_migration_007_before_006`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- enrollments: demo undergraduates x the DICT Level 2 Semester 1 courses
INSERT INTO enrollments (student_id, course_id)
SELECT s.user_id, c.course_id
FROM students s
JOIN users u ON u.user_id = s.user_id AND u.email LIKE '%@fot.example.lk' AND u.role = 'STUDENT'
JOIN courses c ON c.academic_level = 2 AND c.semester = 1
              AND c.department_id = (SELECT department_id FROM departments WHERE department_code = 'DICT')
WHERE NOT EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id = s.user_id AND e.course_id = c.course_id);

-- placeholder assessment schemes (only for a course that has none)
DROP TEMPORARY TABLE IF EXISTS tmp_demo_scheme;
CREATE TEMPORARY TABLE tmp_demo_scheme (course_code VARCHAR(10) NOT NULL, component_name VARCHAR(60) NOT NULL, category VARCHAR(12) NOT NULL, max_marks DECIMAL(6,2) NOT NULL, weightage DECIMAL(5,2) NOT NULL) ENGINE = InnoDB;
INSERT INTO tmp_demo_scheme VALUES
    ('ICT2113', 'Quiz', 'CA', 100, 10),
    ('ICT2113', 'Mid Exam', 'CA', 100, 10),
    ('ICT2113', 'Assignment', 'ASSIGNMENT', 100, 10),
    ('ICT2113', 'Practical', 'PRACTICAL', 100, 10),
    ('ICT2113', 'Final Exam', 'FINAL_EXAM', 100, 60),
    ('ICT2122', 'Test', 'CA', 100, 20),
    ('ICT2122', 'Assignment', 'ASSIGNMENT', 100, 20),
    ('ICT2122', 'Final Exam', 'FINAL_EXAM', 100, 60);

INSERT INTO evaluation_component (course_id, component_name, category, max_marks, weightage)
SELECT c.course_id, t.component_name, t.category, t.max_marks, t.weightage
FROM tmp_demo_scheme t
JOIN courses c ON c.course_code = t.course_code COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM evaluation_component x WHERE x.course_id = c.course_id);

-- component marks of the demo undergraduates (all out of 100)
DROP TEMPORARY TABLE IF EXISTS tmp_demo_marks;
CREATE TEMPORARY TABLE tmp_demo_marks (username VARCHAR(30) NOT NULL, course_code VARCHAR(10) NOT NULL, component_name VARCHAR(60) NOT NULL, marks DECIMAL(6,2) NOT NULL) ENGINE = InnoDB;
INSERT INTO tmp_demo_marks VALUES
    ('tg1001', 'ICT2113', 'Quiz', 90),
    ('tg1001', 'ICT2113', 'Mid Exam', 85),
    ('tg1001', 'ICT2113', 'Assignment', 88),
    ('tg1001', 'ICT2113', 'Practical', 82),
    ('tg1001', 'ICT2113', 'Final Exam', 84),
    ('tg1001', 'ICT2122', 'Test', 80),
    ('tg1001', 'ICT2122', 'Assignment', 85),
    ('tg1001', 'ICT2122', 'Final Exam', 82),
    ('tg1002', 'ICT2113', 'Quiz', 75),
    ('tg1002', 'ICT2113', 'Mid Exam', 70),
    ('tg1002', 'ICT2113', 'Assignment', 72),
    ('tg1002', 'ICT2113', 'Practical', 68),
    ('tg1002', 'ICT2113', 'Final Exam', 70),
    ('tg1002', 'ICT2122', 'Test', 70),
    ('tg1002', 'ICT2122', 'Assignment', 72),
    ('tg1002', 'ICT2122', 'Final Exam', 68),
    ('tg1003', 'ICT2113', 'Quiz', 65),
    ('tg1003', 'ICT2113', 'Mid Exam', 60),
    ('tg1003', 'ICT2113', 'Assignment', 70),
    ('tg1003', 'ICT2113', 'Practical', 62),
    ('tg1003', 'ICT2113', 'Final Exam', 58),
    ('tg1003', 'ICT2122', 'Test', 60),
    ('tg1003', 'ICT2122', 'Assignment', 66),
    ('tg1003', 'ICT2122', 'Final Exam', 62),
    ('tg1004', 'ICT2113', 'Quiz', 55),
    ('tg1004', 'ICT2113', 'Mid Exam', 50),
    ('tg1004', 'ICT2113', 'Assignment', 60),
    ('tg1004', 'ICT2113', 'Practical', 52),
    ('tg1004', 'ICT2113', 'Final Exam', 48),
    ('tg1004', 'ICT2122', 'Test', 50),
    ('tg1004', 'ICT2122', 'Assignment', 55),
    ('tg1004', 'ICT2122', 'Final Exam', 45),
    ('tg1005', 'ICT2113', 'Quiz', 80),
    ('tg1005', 'ICT2113', 'Mid Exam', 78),
    ('tg1005', 'ICT2113', 'Assignment', 74),
    ('tg1005', 'ICT2113', 'Practical', 76),
    ('tg1005', 'ICT2113', 'Final Exam', 72),
    ('tg1005', 'ICT2122', 'Test', 75),
    ('tg1005', 'ICT2122', 'Assignment', 70),
    ('tg1005', 'ICT2122', 'Final Exam', 68),
    ('tg1006', 'ICT2113', 'Quiz', 60),
    ('tg1006', 'ICT2113', 'Mid Exam', 58),
    ('tg1006', 'ICT2113', 'Assignment', 66),
    ('tg1006', 'ICT2113', 'Practical', 64),
    ('tg1006', 'ICT2113', 'Final Exam', 55),
    ('tg1006', 'ICT2122', 'Test', 58),
    ('tg1006', 'ICT2122', 'Assignment', 62),
    ('tg1006', 'ICT2122', 'Final Exam', 50),
    ('tg1007', 'ICT2113', 'Quiz', 45),
    ('tg1007', 'ICT2113', 'Mid Exam', 50),
    ('tg1007', 'ICT2113', 'Assignment', 48),
    ('tg1007', 'ICT2113', 'Practical', 52),
    ('tg1007', 'ICT2113', 'Final Exam', 42),
    ('tg1007', 'ICT2122', 'Test', 48),
    ('tg1007', 'ICT2122', 'Assignment', 52),
    ('tg1007', 'ICT2122', 'Final Exam', 40),
    ('tg1008', 'ICT2113', 'Quiz', 70),
    ('tg1008', 'ICT2113', 'Mid Exam', 72),
    ('tg1008', 'ICT2113', 'Assignment', 68),
    ('tg1008', 'ICT2113', 'Practical', 75),
    ('tg1008', 'ICT2113', 'Final Exam', 66),
    ('tg1008', 'ICT2122', 'Test', 66),
    ('tg1008', 'ICT2122', 'Assignment', 70),
    ('tg1008', 'ICT2122', 'Final Exam', 64),
    ('tg1009', 'ICT2113', 'Quiz', 35),
    ('tg1009', 'ICT2113', 'Mid Exam', 30),
    ('tg1009', 'ICT2113', 'Assignment', 38),
    ('tg1009', 'ICT2113', 'Practical', 40),
    ('tg1009', 'ICT2113', 'Final Exam', 20),
    ('tg1009', 'ICT2122', 'Test', 30),
    ('tg1009', 'ICT2122', 'Assignment', 34),
    ('tg1009', 'ICT2122', 'Final Exam', 28),
    ('tg1010', 'ICT2113', 'Quiz', 30),
    ('tg1010', 'ICT2113', 'Mid Exam', 35),
    ('tg1010', 'ICT2113', 'Assignment', 38),
    ('tg1010', 'ICT2113', 'Practical', 32),
    ('tg1010', 'ICT2113', 'Final Exam', 40),
    ('tg1010', 'ICT2122', 'Test', 34),
    ('tg1010', 'ICT2122', 'Assignment', 30),
    ('tg1010', 'ICT2122', 'Final Exam', 36),
    ('tg1011', 'ICT2113', 'Quiz', 58),
    ('tg1011', 'ICT2113', 'Mid Exam', 55),
    ('tg1011', 'ICT2113', 'Assignment', 62),
    ('tg1011', 'ICT2113', 'Practical', 60),
    ('tg1011', 'ICT2113', 'Final Exam', 52),
    ('tg1011', 'ICT2122', 'Test', 55),
    ('tg1011', 'ICT2122', 'Assignment', 60),
    ('tg1011', 'ICT2122', 'Final Exam', 48),
    ('tg1012', 'ICT2113', 'Quiz', 50),
    ('tg1012', 'ICT2113', 'Mid Exam', 48),
    ('tg1012', 'ICT2113', 'Assignment', 52),
    ('tg1012', 'ICT2113', 'Practical', 55),
    ('tg1012', 'ICT2113', 'Final Exam', 45),
    ('tg1012', 'ICT2122', 'Test', 48),
    ('tg1012', 'ICT2122', 'Assignment', 50),
    ('tg1012', 'ICT2122', 'Final Exam', 42),
    ('tg1013', 'ICT2113', 'Quiz', 62),
    ('tg1013', 'ICT2113', 'Mid Exam', 66),
    ('tg1013', 'ICT2113', 'Assignment', 60),
    ('tg1013', 'ICT2113', 'Practical', 64),
    ('tg1013', 'ICT2113', 'Final Exam', 58),
    ('tg1013', 'ICT2122', 'Test', 60),
    ('tg1013', 'ICT2122', 'Assignment', 64),
    ('tg1013', 'ICT2122', 'Final Exam', 56),
    ('tg1014', 'ICT2113', 'Quiz', 72),
    ('tg1014', 'ICT2113', 'Mid Exam', 70),
    ('tg1014', 'ICT2113', 'Assignment', 75),
    ('tg1014', 'ICT2113', 'Practical', 68),
    ('tg1014', 'ICT2113', 'Final Exam', 64),
    ('tg1014', 'ICT2122', 'Test', 68),
    ('tg1014', 'ICT2122', 'Assignment', 72),
    ('tg1014', 'ICT2122', 'Final Exam', 60),
    ('tg1015', 'ICT2113', 'Quiz', 85),
    ('tg1015', 'ICT2113', 'Mid Exam', 88),
    ('tg1015', 'ICT2113', 'Assignment', 90),
    ('tg1015', 'ICT2113', 'Practical', 86),
    ('tg1015', 'ICT2113', 'Final Exam', 88),
    ('tg1015', 'ICT2122', 'Test', 86),
    ('tg1015', 'ICT2122', 'Assignment', 90),
    ('tg1015', 'ICT2122', 'Final Exam', 84),
    ('tg1016', 'ICT2113', 'Quiz', 68),
    ('tg1016', 'ICT2113', 'Mid Exam', 64),
    ('tg1016', 'ICT2113', 'Assignment', 70),
    ('tg1016', 'ICT2113', 'Practical', 66),
    ('tg1016', 'ICT2113', 'Final Exam', 60),
    ('tg1016', 'ICT2122', 'Test', 64),
    ('tg1016', 'ICT2122', 'Assignment', 68),
    ('tg1016', 'ICT2122', 'Final Exam', 58),
    ('tg0950', 'ICT2113', 'Quiz', 52),
    ('tg0950', 'ICT2113', 'Mid Exam', 48),
    ('tg0950', 'ICT2113', 'Assignment', 55),
    ('tg0950', 'ICT2113', 'Practical', 50),
    ('tg0950', 'ICT2113', 'Final Exam', 46),
    ('tg0950', 'ICT2122', 'Test', 50),
    ('tg0950', 'ICT2122', 'Assignment', 52),
    ('tg0950', 'ICT2122', 'Final Exam', 44),
    ('tg0951', 'ICT2113', 'Quiz', 64),
    ('tg0951', 'ICT2113', 'Mid Exam', 60),
    ('tg0951', 'ICT2113', 'Assignment', 66),
    ('tg0951', 'ICT2113', 'Practical', 62),
    ('tg0951', 'ICT2113', 'Final Exam', 56),
    ('tg0951', 'ICT2122', 'Test', 60),
    ('tg0951', 'ICT2122', 'Assignment', 62),
    ('tg0951', 'ICT2122', 'Final Exam', 54),
    ('tg0960', 'ICT2113', 'Quiz', 40),
    ('tg0960', 'ICT2113', 'Mid Exam', 42),
    ('tg0960', 'ICT2113', 'Assignment', 45),
    ('tg0960', 'ICT2113', 'Practical', 38),
    ('tg0960', 'ICT2113', 'Final Exam', 36),
    ('tg0960', 'ICT2122', 'Test', 40),
    ('tg0960', 'ICT2122', 'Assignment', 44),
    ('tg0960', 'ICT2122', 'Final Exam', 32),
    ('tg0961', 'ICT2113', 'Quiz', 38),
    ('tg0961', 'ICT2113', 'Mid Exam', 36),
    ('tg0961', 'ICT2113', 'Assignment', 40),
    ('tg0961', 'ICT2113', 'Practical', 35),
    ('tg0961', 'ICT2113', 'Final Exam', 30),
    ('tg0961', 'ICT2122', 'Test', 36),
    ('tg0961', 'ICT2122', 'Assignment', 38),
    ('tg0961', 'ICT2122', 'Final Exam', 28);

INSERT INTO component_marks (student_id, eval_component_id, marks_obtained, entered_by)
SELECT s.user_id, ec.eval_component_id, t.marks, c.lecturer_id
FROM tmp_demo_marks t
JOIN users u ON u.username = t.username COLLATE utf8mb4_unicode_ci AND u.email LIKE '%@fot.example.lk' AND u.role = 'STUDENT'
 JOIN students s ON s.user_id = u.user_id
JOIN courses c ON c.course_code = t.course_code COLLATE utf8mb4_unicode_ci
JOIN evaluation_component ec ON ec.course_id = c.course_id AND ec.component_name = t.component_name COLLATE utf8mb4_unicode_ci
JOIN enrollments e ON e.student_id = s.user_id AND e.course_id = c.course_id
WHERE NOT EXISTS (SELECT 1 FROM component_marks x WHERE x.student_id = s.user_id AND x.eval_component_id = ec.eval_component_id);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_marks;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_scheme;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('006_lecturer_test_data', 'demo enrollments, placeholder assessment schemes, component marks', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 006_lecturer_test_data finished - see schema_migrations' AS result;