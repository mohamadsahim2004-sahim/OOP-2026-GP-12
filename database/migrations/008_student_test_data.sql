-- =============================================================================
-- Migration 008 - DEMO data for the undergraduate screens
--
-- DEMO DATA. Additive and repeatable: every row is inserted only if it does not exist yet; nothing is
-- updated or deleted. Rows are attached ONLY to the demo accounts (e-mail ending @fot.example.lk), never to a
-- real student, and course-level rows (assessment scheme, materials) only to a course that has none yet.
--
-- CHANGED from earlier versions:
--   * published results were typed in by hand with CA / final stored as WEIGHTED POINTS (32 and 48) although the
--     application stores and shows percentages 0-100, and with letter grades that do not follow the
--     grading table the application uses (overall 70 was stored as B+ although 70 is A-).
--     Results are now CALCULATED from the component marks (migration 006) and the attendance (migration 004)
--     with exactly the rules of AcademicRules.java:
--        obtainMarks = CA % (0-100)   finalMarks = final-exam % (0-100)   overall = sum(mark/max x weight)
--        isEligible  = CA >= 40 AND attendance >= 80 %  (attendance counts present + late + approved-medical absences)
--        grade / grade_points from the overall mark with the application's grade table (see UNVERIFIED note below)
--     Only complete sets of component marks produce a result; only the demo undergraduates are touched;
--     a result that already exists is never replaced.
--   * The 2 hand-typed legacy rows of tg1001 (exact old values) are removed first so they can be recalculated.
--     This is the only DELETE in the script and matches those two demo rows by their exact old values.
--   * No fixed user id (the old script assumed admin = 1, student = 11).
--
-- UNVERIFIED: the grade table (A+ >= 85 ... E < 30, 4.0 scale) is the one coded in AcademicRules.java /
-- GradingScheme.java. It has NOT been checked against UGC Commission Circular No. 12/2024. If the circular
-- differs, change the two Java classes and the CASE expressions here and in database/verify_assignment_data.sql.
--
-- Notices, timetable slots and course materials below are PLACEHOLDERS, not official.
-- Needs (run first): schema.sql, sample_data.sql, migrations 001, 004, 005, 006, 007.
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/008_student_test_data.sql"
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
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table notices does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notices_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'timetables') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table timetables does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__timetables_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_materials does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_materials_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table component_marks does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__component_marks_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table evaluation_component does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__evaluation_component_missing__run_earlier_migrations`', 'DO 0');
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
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments does not exist. Run the schema and migrations first (README.md, Fresh install / Upgrade).'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices' AND COLUMN_NAME IN ('category', 'priority')) < 2);
SET @s := IF(@bad, 'SELECT ''ABORT: table notices lacks column(s) category, priority. Run the migrations 003, 005, 007, 009, 010 BEFORE this demo-data script.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__run_migration_007_before_008`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- notices
INSERT INTO notices (title, content, notice_date, created_by, category, priority)
SELECT 'Library Opening Hours', 'The faculty library is open 8.00 a.m. to 8.00 p.m. on weekdays during the examination period.',
       '2026-10-26', (SELECT user_id FROM users WHERE username = 'admin' AND role = 'ADMIN' LIMIT 1), 'GENERAL', 'NORMAL' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Library Opening Hours');

INSERT INTO notices (title, content, notice_date, created_by, category, priority)
SELECT 'Mid Semester Results Released', 'Mid semester results for Level 2 Semester 1 are now available in the Results section.',
       '2026-10-27', (SELECT user_id FROM users WHERE username = 'admin' AND role = 'ADMIN' LIMIT 1), 'ACADEMIC', 'URGENT' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Mid Semester Results Released');

INSERT INTO notices (title, content, notice_date, created_by, category, priority)
SELECT 'Final Examination Timetable', 'The final examination timetable will be published on the notice board.',
       '2026-10-28', (SELECT user_id FROM users WHERE username = 'admin' AND role = 'ADMIN' LIMIT 1), 'EXAMINATIONS', 'IMPORTANT' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Final Examination Timetable');

-- timetable slots (placeholders; the demo attendance dates fall on these weekdays)
INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'WEDNESDAY', '08:00:00', '10:00:00', 'LT-03' FROM courses c
WHERE c.course_code = 'ICT2122'
  AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'WEDNESDAY' AND t.start_time = '08:00:00');

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'THURSDAY', '13:00:00', '15:00:00', 'Lab-02' FROM courses c
WHERE c.course_code = 'ICT2132'
  AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'THURSDAY' AND t.start_time = '13:00:00');

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'THURSDAY', '13:00:00', '15:00:00', 'Lab-01' FROM courses c
WHERE c.course_code = 'ICT2113'
  AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'THURSDAY' AND t.start_time = '13:00:00');

-- course materials (only for a course that has none)
INSERT INTO course_materials (courseID, description, notes, referenceText)
SELECT c.course_id, 'Week 1 - Course introduction', 'Read the module outline before the first lecture.', 'Module handbook, chapter 1'
FROM courses c WHERE c.course_code = 'ICT2113'
  AND NOT EXISTS (SELECT 1 FROM course_materials m WHERE m.courseID = c.course_id);

INSERT INTO course_materials (courseID, description, notes, referenceText)
SELECT c.course_id, 'Week 1 - Classes and objects', 'Slides and practice questions.', 'Deitel, Java How to Program, chapter 3'
FROM courses c WHERE c.course_code = 'ICT2122'
  AND NOT EXISTS (SELECT 1 FROM course_materials m WHERE m.courseID = c.course_id);

-- remove the two legacy hand-typed rows of tg1001 (exact old values only) so that they are recalculated below
DELETE m FROM marks m
JOIN users u ON u.user_id = m.student_id AND u.username = 'tg1001' AND u.email = 'tg1001@fot.example.lk'
JOIN courses c ON c.course_id = m.course_id
WHERE (c.course_code = 'ICT2113' AND m.obtainMarks = 32.00 AND m.finalMarks = 48.00 AND m.overall_mark = 80.00 AND m.grade = 'A')
   OR (c.course_code = 'ICT2122' AND m.obtainMarks = 28.00 AND m.finalMarks = 42.00 AND m.overall_mark = 70.00 AND m.grade = 'B+');

-- results (published) calculated from the component marks and the attendance
INSERT INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at)
SELECT r.student_id, r.course_id, r.ca, r.fin,
       CASE WHEN r.overall >= 85 THEN 'A+' WHEN r.overall >= 75 THEN 'A' WHEN r.overall >= 70 THEN 'A-' WHEN r.overall >= 65 THEN 'B+' WHEN r.overall >= 60 THEN 'B' WHEN r.overall >= 55 THEN 'B-' WHEN r.overall >= 50 THEN 'C+' WHEN r.overall >= 45 THEN 'C' WHEN r.overall >= 40 THEN 'C-' WHEN r.overall >= 35 THEN 'D+' WHEN r.overall >= 30 THEN 'D' ELSE 'E' END,
       (r.ca >= 40 AND r.att_present + r.att_absent > 0 AND r.att_total > 0
        AND 100 * (r.att_present + r.att_med) >= 80 * r.att_total),
       r.overall,
       CASE WHEN r.overall >= 85 THEN 4.0 WHEN r.overall >= 75 THEN 4.0 WHEN r.overall >= 70 THEN 3.7 WHEN r.overall >= 65 THEN 3.3 WHEN r.overall >= 60 THEN 3.0 WHEN r.overall >= 55 THEN 2.7 WHEN r.overall >= 50 THEN 2.3 WHEN r.overall >= 45 THEN 2.0 WHEN r.overall >= 40 THEN 1.7 WHEN r.overall >= 35 THEN 1.3 WHEN r.overall >= 30 THEN 1.0 ELSE 0.0 END,
       1, CURRENT_TIMESTAMP
FROM (
    SELECT s.user_id AS student_id, c.course_id,
           ROUND(100 * SUM(CASE WHEN ec.category <> 'FINAL_EXAM' THEN cm.marks_obtained / ec.max_marks * ec.weightage ELSE 0 END)
                 / SUM(CASE WHEN ec.category <> 'FINAL_EXAM' THEN ec.weightage ELSE 0 END), 2) AS ca,
           ROUND(100 * SUM(CASE WHEN ec.category = 'FINAL_EXAM' THEN cm.marks_obtained / ec.max_marks * ec.weightage ELSE 0 END)
                 / SUM(CASE WHEN ec.category = 'FINAL_EXAM' THEN ec.weightage ELSE 0 END), 2) AS fin,
           ROUND(100 * SUM(cm.marks_obtained / ec.max_marks * ec.weightage) / SUM(ec.weightage), 2) AS overall,
           COUNT(*) AS n_marks,
           (SELECT COUNT(*) FROM evaluation_component x WHERE x.course_id = c.course_id) AS n_comp,
           (SELECT COUNT(*) FROM session_attendance sa JOIN course_component cc ON cc.component_id = sa.component_id WHERE sa.user_id = s.user_id AND cc.course_id = c.course_id AND sa.status IN ('present', 'late')) AS att_present,
           (SELECT COUNT(*) FROM session_attendance sa JOIN course_component cc ON cc.component_id = sa.component_id WHERE sa.user_id = s.user_id AND cc.course_id = c.course_id AND sa.status = 'absent') AS att_absent,
           (SELECT COUNT(*) FROM session_attendance sa JOIN course_component cc ON cc.component_id = sa.component_id JOIN medical md ON md.attendance_id = sa.attendance_id WHERE sa.user_id = s.user_id AND cc.course_id = c.course_id AND sa.status = 'absent' AND md.approval_status = 'approved') AS att_med,
           (15 * (SELECT COUNT(*) FROM course_component cc2 WHERE cc2.course_id = c.course_id)) AS att_total
    FROM students s
    JOIN users u ON u.user_id = s.user_id AND u.email LIKE '%@fot.example.lk' AND u.role = 'STUDENT'
    JOIN courses c ON c.course_code IN ('ICT2113', 'ICT2122')
    JOIN enrollments e ON e.student_id = s.user_id AND e.course_id = c.course_id
    JOIN evaluation_component ec ON ec.course_id = c.course_id
    JOIN component_marks cm ON cm.student_id = s.user_id AND cm.eval_component_id = ec.eval_component_id
    GROUP BY s.user_id, c.course_id
) r
WHERE r.n_marks = r.n_comp
  AND NOT EXISTS (SELECT 1 FROM marks m WHERE m.student_id = r.student_id AND m.course_id = r.course_id);

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('008_student_test_data', 'demo notices, timetable slots, materials and calculated published results', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 008_student_test_data finished - see schema_migrations' AS result;

SELECT 'demo published results' AS what, COUNT(*) AS n FROM marks m JOIN users u ON u.user_id = m.student_id WHERE u.email LIKE '%@fot.example.lk' AND m.published = 1;