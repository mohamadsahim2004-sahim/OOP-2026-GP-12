-- =============================================================================
-- Migration 004 - DEMO attendance and medical data (Technical Officer module)
--
-- DEMO DATA. Additive and repeatable: every row is inserted only if it does not exist yet; nothing is
-- updated or deleted. Rows are attached ONLY to the demo accounts (e-mail ending @fot.example.lk), never to a
-- real student, and course-level rows (assessment scheme, materials) only to a course that has none yet.
--
-- CHANGED from earlier versions, which ran DELETE FROM medical / session_attendance / course_component
-- (destroying real attendance) and used fixed ids (course 1, users 7-8 and 11-30) and CURDATE():
--   * NO delete. Components, sessions and medicals are looked up by course_code / username.
--   * Session dates are fixed, so running it again produces identical data.
--   * Rows are keyed on the unique keys (student, component, session) / (attendance), so a second run adds nothing.
--
-- What it creates (assignment: 15 theory + 15 practical sessions; five attendance scenarios):
--   ICT2113 (Theory + Practical, 15 + 15 sessions) for the 20 demo undergraduates, and ICT2122 (Theory, 15 sessions).
--   Session dates are DEMO dates (weekly, ending 2026-10-27), not the official semester calendar.
--   Attendance % of a course = (present + late + approved-medical absences) / (15 x number of components).
--   ICT2113 combined percentage covers: >80% (tg1001, tg1002 ...), exactly 80% (tg1007, tg1014),
--   <80% without medicals (tg1004, tg1012 ...), >80% with medicals (tg1006, tg1008),
--   <80% with medicals (tg1009, tg1010, tg1011, tg0961).  Medicals are approved, pending and rejected.
--
-- Needs (run first): schema.sql, sample_data.sql, migrations 003, 005, 007, 009 (and 010).
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/004_attendance_test_data.sql"
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
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_component does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_component_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table session_attendance does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__session_attendance_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME IN ('submitted_by', 'decided_at', 'rejection_reason')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical lacks column(s) submitted_by, decided_at, rejection_reason. Run the migrations 003, 005, 007, 009, 010 BEFORE this demo-data script.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__run_migration_009_before_004`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- helper numbers 1..15 (temporary: exists only in this session)
DROP TEMPORARY TABLE IF EXISTS tmp_nums;
CREATE TEMPORARY TABLE tmp_nums (n INT NOT NULL PRIMARY KEY) ENGINE = InnoDB;
INSERT INTO tmp_nums VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14), (15);

-- components to create (only if the course does not have that component yet)
DROP TEMPORARY TABLE IF EXISTS tmp_demo_comp;
CREATE TEMPORARY TABLE tmp_demo_comp (course_code VARCHAR(10) NOT NULL, ctype VARCHAR(10) NOT NULL) ENGINE = InnoDB;
INSERT INTO tmp_demo_comp VALUES
    ('ICT2113', 'THEORY'),
    ('ICT2113', 'PRACTICAL'),
    ('ICT2122', 'THEORY');

INSERT INTO course_component (course_id, type)
SELECT c.course_id, t.ctype
FROM tmp_demo_comp t
JOIN courses c ON c.course_code = t.course_code COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM course_component x WHERE x.course_id = c.course_id AND x.type = t.ctype COLLATE utf8mb4_unicode_ci);

-- attended = number of sessions attended (sessions 1..attended are present, the rest absent)
DROP TEMPORARY TABLE IF EXISTS tmp_demo_att;
CREATE TEMPORARY TABLE tmp_demo_att (username VARCHAR(30) NOT NULL, course_code VARCHAR(10) NOT NULL, ctype VARCHAR(10) NOT NULL, attended INT NOT NULL, first_date DATE NOT NULL) ENGINE = InnoDB;
INSERT INTO tmp_demo_att VALUES
    ('tg1001', 'ICT2113', 'THEORY', 15, '2026-07-21'),
    ('tg1001', 'ICT2113', 'PRACTICAL', 15, '2026-07-23'),
    ('tg1002', 'ICT2113', 'THEORY', 14, '2026-07-21'),
    ('tg1002', 'ICT2113', 'PRACTICAL', 13, '2026-07-23'),
    ('tg1003', 'ICT2113', 'THEORY', 13, '2026-07-21'),
    ('tg1003', 'ICT2113', 'PRACTICAL', 14, '2026-07-23'),
    ('tg1004', 'ICT2113', 'THEORY', 12, '2026-07-21'),
    ('tg1004', 'ICT2113', 'PRACTICAL', 10, '2026-07-23'),
    ('tg1005', 'ICT2113', 'THEORY', 12, '2026-07-21'),
    ('tg1005', 'ICT2113', 'PRACTICAL', 14, '2026-07-23'),
    ('tg1006', 'ICT2113', 'THEORY', 11, '2026-07-21'),
    ('tg1006', 'ICT2113', 'PRACTICAL', 12, '2026-07-23'),
    ('tg1007', 'ICT2113', 'THEORY', 10, '2026-07-21'),
    ('tg1007', 'ICT2113', 'PRACTICAL', 11, '2026-07-23'),
    ('tg1008', 'ICT2113', 'THEORY', 9, '2026-07-21'),
    ('tg1008', 'ICT2113', 'PRACTICAL', 15, '2026-07-23'),
    ('tg1009', 'ICT2113', 'THEORY', 8, '2026-07-21'),
    ('tg1009', 'ICT2113', 'PRACTICAL', 9, '2026-07-23'),
    ('tg1010', 'ICT2113', 'THEORY', 10, '2026-07-21'),
    ('tg1010', 'ICT2113', 'PRACTICAL', 13, '2026-07-23'),
    ('tg1011', 'ICT2113', 'THEORY', 11, '2026-07-21'),
    ('tg1011', 'ICT2113', 'PRACTICAL', 10, '2026-07-23'),
    ('tg1012', 'ICT2113', 'THEORY', 9, '2026-07-21'),
    ('tg1012', 'ICT2113', 'PRACTICAL', 12, '2026-07-23'),
    ('tg1013', 'ICT2113', 'THEORY', 8, '2026-07-21'),
    ('tg1013', 'ICT2113', 'PRACTICAL', 15, '2026-07-23'),
    ('tg1014', 'ICT2113', 'THEORY', 14, '2026-07-21'),
    ('tg1014', 'ICT2113', 'PRACTICAL', 8, '2026-07-23'),
    ('tg1015', 'ICT2113', 'THEORY', 13, '2026-07-21'),
    ('tg1015', 'ICT2113', 'PRACTICAL', 14, '2026-07-23'),
    ('tg1016', 'ICT2113', 'THEORY', 12, '2026-07-21'),
    ('tg1016', 'ICT2113', 'PRACTICAL', 11, '2026-07-23'),
    ('tg0950', 'ICT2113', 'THEORY', 10, '2026-07-21'),
    ('tg0950', 'ICT2113', 'PRACTICAL', 13, '2026-07-23'),
    ('tg0951', 'ICT2113', 'THEORY', 15, '2026-07-21'),
    ('tg0951', 'ICT2113', 'PRACTICAL', 14, '2026-07-23'),
    ('tg0960', 'ICT2113', 'THEORY', 11, '2026-07-21'),
    ('tg0960', 'ICT2113', 'PRACTICAL', 9, '2026-07-23'),
    ('tg0961', 'ICT2113', 'THEORY', 7, '2026-07-21'),
    ('tg0961', 'ICT2113', 'PRACTICAL', 10, '2026-07-23'),
    ('tg1001', 'ICT2122', 'THEORY', 15, '2026-07-22'),
    ('tg1002', 'ICT2122', 'THEORY', 14, '2026-07-22'),
    ('tg1003', 'ICT2122', 'THEORY', 13, '2026-07-22'),
    ('tg1004', 'ICT2122', 'THEORY', 12, '2026-07-22'),
    ('tg1005', 'ICT2122', 'THEORY', 14, '2026-07-22'),
    ('tg1006', 'ICT2122', 'THEORY', 13, '2026-07-22'),
    ('tg1007', 'ICT2122', 'THEORY', 12, '2026-07-22'),
    ('tg1008', 'ICT2122', 'THEORY', 11, '2026-07-22'),
    ('tg1009', 'ICT2122', 'THEORY', 10, '2026-07-22'),
    ('tg1010', 'ICT2122', 'THEORY', 13, '2026-07-22'),
    ('tg1011', 'ICT2122', 'THEORY', 12, '2026-07-22'),
    ('tg1012', 'ICT2122', 'THEORY', 12, '2026-07-22'),
    ('tg1013', 'ICT2122', 'THEORY', 11, '2026-07-22'),
    ('tg1014', 'ICT2122', 'THEORY', 15, '2026-07-22'),
    ('tg1015', 'ICT2122', 'THEORY', 14, '2026-07-22'),
    ('tg1016', 'ICT2122', 'THEORY', 13, '2026-07-22'),
    ('tg0950', 'ICT2122', 'THEORY', 12, '2026-07-22'),
    ('tg0951', 'ICT2122', 'THEORY', 14, '2026-07-22'),
    ('tg0960', 'ICT2122', 'THEORY', 13, '2026-07-22'),
    ('tg0961', 'ICT2122', 'THEORY', 9, '2026-07-22');

INSERT INTO session_attendance (user_id, component_id, session_no, date, status)
SELECT u.user_id, cc.component_id, n.n, DATE_ADD(t.first_date, INTERVAL (n.n - 1) * 7 DAY),
       IF(n.n <= t.attended, 'present', 'absent')
FROM tmp_demo_att t
JOIN users u ON u.username = t.username COLLATE utf8mb4_unicode_ci AND u.email LIKE '%@fot.example.lk' AND u.role = 'STUDENT'
 JOIN students s ON s.user_id = u.user_id
JOIN courses c ON c.course_code = t.course_code COLLATE utf8mb4_unicode_ci
JOIN course_component cc ON cc.course_id = c.course_id AND cc.type = t.ctype COLLATE utf8mb4_unicode_ci
JOIN tmp_nums n
WHERE NOT EXISTS (SELECT 1 FROM session_attendance x
                  WHERE x.user_id = u.user_id AND x.component_id = cc.component_id AND x.session_no = n.n);

-- medicals (always on an ABSENT session): approved, pending and rejected. officer = the technical officer who filed/decided it.
DROP TEMPORARY TABLE IF EXISTS tmp_demo_med;
CREATE TEMPORARY TABLE tmp_demo_med (username VARCHAR(30) NOT NULL, ctype VARCHAR(10) NOT NULL, session_no INT NOT NULL, officer VARCHAR(30) NOT NULL, reason VARCHAR(255) NOT NULL, status VARCHAR(10) NOT NULL) ENGINE = InnoDB;
INSERT INTO tmp_demo_med VALUES
    ('tg1006', 'THEORY', 12, 'to.kumara', 'Fever', 'approved'),
    ('tg1006', 'THEORY', 13, 'to.kumara', 'Fever', 'approved'),
    ('tg1007', 'THEORY', 11, 'to.kumara', 'Dengue', 'approved'),
    ('tg1007', 'THEORY', 12, 'to.kumara', 'Dengue', 'approved'),
    ('tg1007', 'THEORY', 13, 'to.kumara', 'Dengue', 'approved'),
    ('tg1008', 'THEORY', 10, 'to.dissanayake', 'Viral infection', 'approved'),
    ('tg1008', 'THEORY', 11, 'to.dissanayake', 'Viral infection', 'approved'),
    ('tg1009', 'THEORY', 9, 'to.dissanayake', 'Food poisoning', 'approved'),
    ('tg1009', 'PRACTICAL', 10, 'to.dissanayake', 'Food poisoning', 'approved'),
    ('tg1014', 'PRACTICAL', 9, 'to.kumara', 'Accident', 'approved'),
    ('tg1014', 'PRACTICAL', 10, 'to.kumara', 'Accident', 'approved'),
    ('tg1010', 'THEORY', 11, 'to.kumara', 'Fever', 'pending'),
    ('tg0961', 'PRACTICAL', 12, 'to.kumara', 'Migraine', 'pending'),
    ('tg1011', 'THEORY', 12, 'to.dissanayake', 'Family event', 'rejected');

INSERT INTO medical (attendance_id, reviewed_by, submitted_by, submitted_date, reason, approval_status, decided_at, rejection_reason)
SELECT sa.attendance_id,
       IF(t.status = 'pending', NULL, ro.user_id),
       ro.user_id,
       DATE_ADD(sa.date, INTERVAL 2 DAY),
       t.reason, t.status,
       IF(t.status = 'pending', NULL, TIMESTAMP(DATE_ADD(sa.date, INTERVAL 3 DAY), '10:00:00')),
       IF(t.status = 'rejected', 'Certificate not accepted (demo data)', NULL)
FROM tmp_demo_med t
JOIN users u ON u.username = t.username COLLATE utf8mb4_unicode_ci AND u.email LIKE '%@fot.example.lk' AND u.role = 'STUDENT'
 JOIN students s ON s.user_id = u.user_id
JOIN courses c ON c.course_code = 'ICT2113'
JOIN course_component cc ON cc.course_id = c.course_id AND cc.type = t.ctype COLLATE utf8mb4_unicode_ci
JOIN session_attendance sa ON sa.user_id = u.user_id AND sa.component_id = cc.component_id AND sa.session_no = t.session_no
JOIN users ro ON ro.username = t.officer COLLATE utf8mb4_unicode_ci AND ro.role = 'TECHNICAL_OFFICER'
WHERE sa.status = 'absent'
  AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id = sa.attendance_id);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_med;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_att;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_comp;
DROP TEMPORARY TABLE IF EXISTS tmp_nums;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('004_attendance_test_data', 'demo attendance (ICT2113 T+P, ICT2122 T) and medicals for the demo undergraduates', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 004_attendance_test_data finished - see schema_migrations' AS result;

SELECT 'demo attendance rows' AS what, COUNT(*) AS n FROM session_attendance sa JOIN users u ON u.user_id = sa.user_id WHERE u.email LIKE '%@fot.example.lk'
UNION ALL
SELECT 'demo medical rows', COUNT(*) FROM medical m JOIN session_attendance sa ON sa.attendance_id = m.attendance_id JOIN users u ON u.user_id = sa.user_id WHERE u.email LIKE '%@fot.example.lk';