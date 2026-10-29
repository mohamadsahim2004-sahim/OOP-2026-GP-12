-- =============================================================================
-- Migration 008 - OPTIONAL test data for the undergraduate screens
--
-- Run AFTER 001-007. Idempotent (INSERT IGNORE / NOT EXISTS guards).
-- Gives the sample student tg1001 (user 11) something to see on every screen:
-- notices, weekly timetable slots, course materials and published marks.
-- Enrollments (all Level 2 Semester 1 courses) come from 006_lecturer_test_data.sql,
-- attendance (course 1, users 11-15) from 004_attendance_test_data.sql.
-- =============================================================================

USE faculty_ams;

UPDATE notices SET category = 'EXAMINATIONS', priority = 'IMPORTANT' WHERE title = 'Exam Notice' AND category = 'GENERAL';

INSERT INTO notices (title, content, notice_date, created_by, category, priority)
SELECT 'Library Opening Hours', 'The faculty library is open 8.00 a.m. to 8.00 p.m. on weekdays during the examination period.',
       CURDATE(), 1, 'GENERAL', 'NORMAL' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Library Opening Hours');

INSERT INTO notices (title, content, notice_date, created_by, category, priority)
SELECT 'Mid Semester Results Released', 'Mid semester results for Level 2 Semester 1 are now available in the Results section.',
       CURDATE(), 1, 'ACADEMIC', 'URGENT' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = 'Mid Semester Results Released');

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'WEDNESDAY', '08:00:00', '10:00:00', 'LT-03' FROM courses c
WHERE c.course_code = 'ICT2122' AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id);

INSERT INTO timetables (course_id, day_of_week, start_time, end_time, venue)
SELECT c.course_id, 'THURSDAY', '13:00:00', '15:00:00', 'Lab-02' FROM courses c
WHERE c.course_code = 'ICT2132' AND NOT EXISTS (SELECT 1 FROM timetables t WHERE t.course_id = c.course_id AND t.day_of_week = 'THURSDAY');

INSERT INTO course_materials (courseID, description, notes, referenceText)
SELECT c.course_id, 'Week 1 - Course introduction', 'Read the module outline before the first lecture.', 'Module handbook, chapter 1'
FROM courses c WHERE c.course_code = 'ICT2113'
  AND NOT EXISTS (SELECT 1 FROM course_materials m WHERE m.courseID = c.course_id);

INSERT INTO course_materials (courseID, description, notes, referenceText)
SELECT c.course_id, 'Week 1 - Classes and objects', 'Slides and practice questions.', 'Deitel, Java How to Program, chapter 3'
FROM courses c WHERE c.course_code = 'ICT2122'
  AND NOT EXISTS (SELECT 1 FROM course_materials m WHERE m.courseID = c.course_id);

-- Published results for tg1001 (overall = 40% CA + 60% final exam; grade bands as in GradingScheme)
INSERT IGNORE INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at)
SELECT 11, c.course_id, 32.00, 48.00, 'A', 1, 80.00, 4.00, 1, NOW() FROM courses c WHERE c.course_code = 'ICT2113';
INSERT IGNORE INTO marks (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at)
SELECT 11, c.course_id, 28.00, 42.00, 'B+', 1, 70.00, 3.30, 1, NOW() FROM courses c WHERE c.course_code = 'ICT2122';
