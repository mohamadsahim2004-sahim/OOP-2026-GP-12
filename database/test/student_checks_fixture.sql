-- =============================================================================
-- Fixture for com.facultyams.StudentModuleChecks  (DEV / TEST DATABASES ONLY)
--
-- Run AFTER schema.sql, sample_data.sql and the migrations (including the demo data 004, 006, 008):
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/test/student_checks_fixture.sql"
--
-- Why this exists: 006_lecturer_test_data.sql enrolls every demo student in every DICT
-- Level 2 Semester 1 course, while the student checks need a few students with specific
-- enrollments, marks, materials and a pending medical request. This file puts those
-- students into exactly that state. It is idempotent (safe to run again) and only touches:
--   * enrollments and marks of students 11-16 (tg1001-tg1006)
--   * course_materials 1-5 and the ICT2142 timetable slot
--   * the sample medical request MED-SAMPLE-0001
-- Do NOT run it on a database with real data.
--
-- SAFETY GUARD (added): this file DELETES and REPLACES rows by fixed ids, so it now REFUSES to run
-- (and changes nothing) unless every user in the database is a demo account (e-mail ending
-- @fot.example.lk) and users 11-16 are exactly tg1001-tg1006.
--
-- CORRECTED (added): earlier versions stored CA and final as weighted POINTS (32.16 / 48.24) and used
-- letter grades that do not match the application's grade table. CA / final are now percentages
-- (0-100), and each overall mark lies in the band of its letter grade (80.40 A, 67.00 B+, 85.00 -> 80.00 A,
-- 52.00 C+, 47.00 C). Grade points, the expected CGPAs (3.80 / 2.18) and credits are unchanged.
-- =============================================================================

USE faculty_ams;

SET @bad := ((SELECT COUNT(*) FROM users WHERE email NOT LIKE '%@fot.example.lk') > 0);
SET @s := IF(@bad, 'SELECT ''ABORT: this fixture deletes and replaces rows and must only run on a database that contains ONLY the demo accounts (users with another e-mail domain exist). Nothing was changed.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__fixture_refuses_real_users`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM users WHERE (user_id = 11 AND username <> 'tg1001') OR (user_id = 12 AND username <> 'tg1002') OR (user_id = 16 AND username <> 'tg1006')) > 0 OR (SELECT COUNT(*) FROM users WHERE user_id BETWEEN 11 AND 16) < 6);
SET @s := IF(@bad, 'SELECT ''ABORT: users 11-16 are not tg1001-tg1006. Load sample_data.sql into an empty database first. Nothing was changed.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__fixture_needs_demo_ids_11_to_16`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;


-- ---------------------------------------------------------------- enrollments
-- course ids: 1 ICT2113 (3 cr), 2 ICT2122 (2), 3 ICT2132 (2), 4 ICT2142 (2), 5 ICT2152 (2)
DELETE FROM enrollments WHERE student_id BETWEEN 11 AND 16;

INSERT INTO enrollments (student_id, course_id) VALUES
    (11, 1), (11, 2), (11, 3), (11, 5),     -- tg1001: 9 credits registered (ICT2152 has no marks yet)
    (12, 1), (12, 2), (12, 3),              -- tg1002: 7 credits registered
    (13, 1), (13, 2), (13, 3),
    (14, 1), (14, 2),                       -- tg1004: enrolled, no marks
    (15, 1), (15, 2), (15, 3);              -- tg1006 (16) has no enrollments at all

-- ---------------------------------------------------------------------- marks
DELETE FROM marks WHERE student_id BETWEEN 11 AND 16;

-- tg1001: CGPA = (4.0*3 + 3.3*2 + 4.0*2) / 7 = 3.80, three graded courses, all passed
-- (obtainMarks = CA %, finalMarks = final-exam %, overall = 0.4 CA + 0.6 final; letter grade from the overall mark)
INSERT INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at) VALUES
    (11, 1, 80.40, 80.40, 'A',  1, 80.40, 4.00, 1, NOW()),
    (11, 2, 67.00, 67.00, 'B+', 1, 67.00, 3.30, 1, NOW()),
    (11, 3, 80.00, 80.00, 'A',  1, 80.00, 4.00, 1, NOW());

-- tg1002: CGPA = (2.3*3 + 2.0*2) / 5 = 2.18; ICT2122 fails because CA < 40 (not eligible)
INSERT INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at) VALUES
    (12, 1, 52.00, 52.00, 'C+', 1, 52.00, 2.30, 1, NOW()),
    (12, 2, 35.00, 55.00, 'C',  0, 47.00, 2.00, 1, NOW());

-- ------------------------------------------------------------ course materials
-- ICT2113 has 3 materials (the first one is a link), ICT2122 has 1, ICT2142 has 1.
REPLACE INTO course_materials (materialID, courseID, description, notes, referenceText) VALUES
    (1, 1, 'Week 1 - Course introduction',  'Read the module outline before the first lecture.', 'https://example.edu/ict2113/outline'),
    (2, 1, 'Week 2 - Classes and objects',  'Slides and practice questions.',                   'Deitel, Java How to Program, chapter 3'),
    (3, 1, 'Week 3 - Inheritance',          'Lab sheet 3.',                                     'lab3.pdf'),
    (4, 2, 'Week 1 - Classes and objects',  'Slides and practice questions.',                   'Deitel, Java How to Program, chapter 3'),
    (5, 4, 'Week 1 - Introduction',         'Module overview.',                                 'Module handbook, chapter 1');

-- ------------------------------------------------------------------- timetable
-- ICT2142 meets on Wednesday and overlaps ICT2122 (08:00-10:00), so enrolling in both is a clash.
INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT 4, 'WEDNESDAY', '09:00:00', '11:00:00', 'LT-04' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM timetables WHERE course_id = 4 AND day_of_week = 'WEDNESDAY');

-- ------------------------------------------------------- sample medical request
-- A pending request of tg1002 (user 12) for her first absent session without a request yet.
INSERT INTO medical (attendance_id, reviewed_by, submitted_date, reason, approval_status,
                     request_ref, doctor, certificate_no, notes, certificate_files)
SELECT sa.attendance_id, NULL, CURDATE(), 'Fever', 'pending',
       'MED-SAMPLE-0001', 'Dr. Sample', 'MC-0001', 'Sample request for the checks', NULL
FROM session_attendance sa
WHERE sa.user_id = 12 AND LOWER(sa.status) = 'absent'
  AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id = sa.attendance_id)
  AND NOT EXISTS (SELECT 1 FROM medical m2 WHERE m2.request_ref = 'MED-SAMPLE-0001')
ORDER BY sa.attendance_id
LIMIT 1;