-- =============================================================================
-- Optional DEMO rows for the Notice / Timetable screens  -  REPEATABLE, additive only.
-- Run AFTER 001_notices_timetables.sql (or schema.sql) and sample_data.sql.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/001_sample_notices_timetables.sql"
--
-- Each row is inserted only if the same notice title / the same course + day + start time does not exist.
-- The author is the demo admin looked up by username (earlier versions assumed user id 1).
-- The timetable slots are PLACEHOLDERS, not the official timetable.
-- =============================================================================

USE faculty_ams;

INSERT INTO notices (title, content, notice_date, created_by)
SELECT 'Exam Notice', 'Mid exam schedule will be released next week.', '2026-09-20',
       (SELECT user_id FROM users WHERE username = 'admin' AND role = 'ADMIN' LIMIT 1) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Exam Notice');

INSERT INTO notices (title, content, notice_date, created_by)
SELECT 'Library Notice', 'New books are available in the faculty library.', '2026-08-28',
       (SELECT user_id FROM users WHERE username = 'admin' AND role = 'ADMIN' LIMIT 1) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Library Notice');

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'MONDAY', '08:00:00', '10:00:00', 'LT-01'
FROM courses c WHERE c.course_code = 'ICT2132'
  AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'MONDAY' AND t.start_time = '08:00:00');

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'TUESDAY', '10:00:00', '12:00:00', 'LT-02'
FROM courses c WHERE c.course_code = 'ICT2113'
  AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'TUESDAY' AND t.start_time = '10:00:00');