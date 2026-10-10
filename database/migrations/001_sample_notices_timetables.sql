-- Optional demo rows for the Notice / Timetable screens.
-- Run AFTER 001_notices_timetables.sql. Only inserts when the tables are empty.
USE faculty_ams;

INSERT INTO notices (title, content, notice_date, created_by)
SELECT 'Exam Notice', 'Mid exam schedule will be released next week.', '2026-09-20', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices);

INSERT INTO notices (title, content, notice_date, created_by)
SELECT 'Library Notice', 'New books are available in the faculty library.', '2026-08-28', 1 FROM DUAL
WHERE (SELECT COUNT(*) FROM notices) = 1;

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'MONDAY', '08:00:00', '10:00:00', 'LT-01'
FROM courses c WHERE c.course_code = 'ICT2132'
  AND NOT EXISTS (SELECT 1 FROM timetables);

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'TUESDAY', '10:00:00', '12:00:00', 'LT-02'
FROM courses c WHERE c.course_code = 'ICT2113'
  AND (SELECT COUNT(*) FROM timetables) = 1;