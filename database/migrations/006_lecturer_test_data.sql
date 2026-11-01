-- =============================================================================
-- Migration 006 - TEST DATA for the Lecturer module (optional, idempotent)
--
--   mysql -u root -p faculty_ams < database/migrations/006_lecturer_test_data.sql
--
-- Needs schema.sql, sample_data.sql and 003/004/005 first. Uses INSERT IGNORE, so
-- running it again changes nothing. It does NOT create any marks or results.
--
-- Lecturer logins:  lec.perera (courses 1, 5)  lec.silva (courses 2, 3)  - password lecturer123
-- -----------------------------------------------------------------------------

USE faculty_ams;

-- Every sample student takes all Level 2 Semester 1 sample courses (course ids 1-8)
INSERT IGNORE INTO enrollments (student_id, course_id)
SELECT s.user_id, c.course_id
FROM students s
JOIN courses c ON c.academic_level = 2 AND c.semester = 1;

-- ICT2113 (course 1, lecturer 2, Theory + Practical): CA part = 40, final exam = 60
INSERT IGNORE INTO evaluation_component (course_id, component_name, category, max_marks, weightage) VALUES
    (1, 'Quiz',        'CA',         20,  10),
    (1, 'Mid Exam',    'CA',        100,  10),
    (1, 'Assignment',  'ASSIGNMENT',100,  10),
    (1, 'Practical',   'PRACTICAL', 100,  10),
    (1, 'Final Exam',  'FINAL_EXAM',100,  60);

-- ICT2122 (course 2, lecturer 3, Theory): CA part = 40, final exam = 60
INSERT IGNORE INTO evaluation_component (course_id, component_name, category, max_marks, weightage) VALUES
    (2, 'Test',        'CA',        100,  20),
    (2, 'Assignment',  'ASSIGNMENT',100,  20),
    (2, 'Final Exam',  'FINAL_EXAM',100,  60);
