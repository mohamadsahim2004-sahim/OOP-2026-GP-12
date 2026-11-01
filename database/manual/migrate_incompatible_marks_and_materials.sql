-- =============================================================================
-- MANUAL RECOVERY TEMPLATE - incompatible `marks` and/or `course_materials` table
--
-- Use this ONLY when schema.sql / migration 005 / database/audit_existing_database.sql reported that
-- `marks` or `course_materials` already exists with another layout (for example columns such as course_code
-- or score instead of marksid / student_id / course_id / obtainMarks / finalMarks ...).
-- The numbered migrations STOP in that case and change nothing. They never rename, drop or convert such a
-- table by themselves, because the legacy columns cannot be mapped without looking at your real data.
--
-- THIS FILE IS A TEMPLATE. Run it section by section by hand:
--   * Sections A and B are read-only (SELECT / SHOW) and are safe to run.
--   * Sections C, D, E and F change data. They are COMMENTED OUT. Read the result of A and B first, then
--     edit the column mapping in D, and un-comment ONE section at a time.
--   * Every step has a check. If a check does not give the expected result, STOP and restore the backup
--     (README.md, "Recovery").
--
-- IF THE OLD VERSION OF MIGRATION 005 WAS RUN on your database (it renamed an incompatible `marks` /
-- `course_materials` to `marks_other_module` / `course_materials_other_module` and created new empty tables),
-- the other module's records are NOT lost: they sit in the *_other_module tables. database/audit_existing_database.sql
-- lists them. Use the *_other_module table as the SOURCE in section D (replace `FROM marks o` by
-- `FROM marks_other_module o`) and copy its rows into the current `marks` table with INSERT ... SELECT
-- (skip rows that already exist for the same student and course).
--
-- Nothing in this file touches `users`, `students`, `courses`, `enrollments`, attendance or medical data.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/manual/migrate_incompatible_marks_and_materials.sql"
-- =============================================================================

USE faculty_ams;

-- -----------------------------------------------------------------------------
-- STEP 0 (outside MySQL, PowerShell) - take a full backup first and keep it:
--   mysqldump -u <admin_user> -p --single-transaction --routines --triggers --default-character-set=utf8mb4 --result-file=backups\faculty_ams_before_marks_recovery.sql faculty_ams
-- Check that the file exists and is not empty before continuing.
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- A. DIAGNOSE (read-only): what exists today?
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('marks', 'course_materials')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- row counts of the tables as they are now (write these numbers down)
SELECT 'marks' AS tbl, COUNT(*) AS row_count FROM marks
UNION ALL
SELECT 'course_materials', COUNT(*) FROM course_materials;

-- foreign keys that point INTO these tables (must be none, or handled before a rename)
SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IN ('marks', 'course_materials');

-- the exact definitions (copy the output into your notes)
SHOW CREATE TABLE marks;
SHOW CREATE TABLE course_materials;

-- -----------------------------------------------------------------------------
-- B. COMPARE (read-only): the shape the application needs
--    marks             marksid, student_id, course_id, obtainMarks, finalMarks, grade, isEligible,
--                      overall_mark, grade_points, published, published_at
--    course_materials  materialID, courseID, description, notes, referenceText
-- Columns the application needs but the existing table lacks:
-- -----------------------------------------------------------------------------
SELECT 'marks' AS tbl, need.col AS missing_column
FROM (SELECT 'marksid' AS col UNION ALL SELECT 'student_id' UNION ALL SELECT 'course_id' UNION ALL SELECT 'obtainMarks'
      UNION ALL SELECT 'finalMarks' UNION ALL SELECT 'grade' UNION ALL SELECT 'isEligible' UNION ALL SELECT 'overall_mark'
      UNION ALL SELECT 'grade_points' UNION ALL SELECT 'published' UNION ALL SELECT 'published_at') need
WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS c
                  WHERE c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = 'marks' AND c.COLUMN_NAME = need.col)
UNION ALL
SELECT 'course_materials', need.col
FROM (SELECT 'materialID' AS col UNION ALL SELECT 'courseID' UNION ALL SELECT 'description' UNION ALL SELECT 'notes'
      UNION ALL SELECT 'referenceText') need
WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS c
                  WHERE c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = 'course_materials' AND c.COLUMN_NAME = need.col);

-- Look at a few legacy rows to decide how each legacy column maps to a new column:
SELECT * FROM marks LIMIT 10;
SELECT * FROM course_materials LIMIT 10;

-- =============================================================================
-- Everything below changes data. It is commented out on purpose.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- C. KEEP A COPY INSIDE THE DATABASE (the mysqldump from step 0 remains the real backup)
--    Un-comment, run, then CHECK that both counts are equal.
-- -----------------------------------------------------------------------------
-- CREATE TABLE marks_legacy_backup AS SELECT * FROM marks;
-- SELECT (SELECT COUNT(*) FROM marks) AS original_rows, (SELECT COUNT(*) FROM marks_legacy_backup) AS backup_rows;
--
-- CREATE TABLE course_materials_legacy_backup AS SELECT * FROM course_materials;
-- SELECT (SELECT COUNT(*) FROM course_materials) AS original_rows, (SELECT COUNT(*) FROM course_materials_legacy_backup) AS backup_rows;

-- -----------------------------------------------------------------------------
-- D. BUILD THE COMPATIBLE TABLE NEXT TO THE OLD ONE AND COPY EVERY ROW (not a parallel table: it replaces the
--    old one in step F and the old one is kept only as the *_legacy_backup)
--    EDIT the SELECT list so that each new column is filled from the matching legacy column.
--    The mapping below is an EXAMPLE for a legacy layout (student_id, course_code, score, grade); change it
--    to what step A showed. Marks must end up as PERCENTAGES 0-100.
-- -----------------------------------------------------------------------------
-- CREATE TABLE marks_new (
--     marksid      INT          NOT NULL AUTO_INCREMENT,
--     student_id   INT          NOT NULL,
--     course_id    INT          NOT NULL,
--     obtainMarks  DECIMAL(5,2) NOT NULL DEFAULT 0,
--     finalMarks   DECIMAL(5,2) NOT NULL DEFAULT 0,
--     grade        VARCHAR(3)   NULL,
--     isEligible   BOOLEAN      NOT NULL DEFAULT FALSE,
--     overall_mark DECIMAL(5,2) NULL,
--     grade_points DECIMAL(3,2) NULL,
--     published    TINYINT(1)   NOT NULL DEFAULT 0,
--     published_at DATETIME     NULL,
--     PRIMARY KEY (marksid),
--     CONSTRAINT uq_marks_student_course_new UNIQUE (student_id, course_id)
-- ) ENGINE = InnoDB;
--
-- INSERT INTO marks_new (student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at)
-- SELECT o.student_id,
--        c.course_id,                    -- legacy table stored course_code: look the id up
--        0, 0,                           -- CA / final are unknown for a legacy single-score row: fill them if you have them
--        o.grade,
--        0,
--        o.score,                        -- legacy "score" becomes the overall mark (must already be 0-100)
--        NULL,                           -- grade points: leave NULL, or fill from your grade table
--        0, NULL                         -- not published until a lecturer publishes it in the application
-- FROM marks o
-- JOIN courses c ON c.course_code = o.course_code;
--
-- CHECK 1: every legacy row was copied (the two numbers must be equal; otherwise find the rows that did not join):
-- SELECT (SELECT COUNT(*) FROM marks) AS legacy_rows, (SELECT COUNT(*) FROM marks_new) AS copied_rows;
-- CHECK 2: the rows that were NOT copied (must return no row):
-- SELECT o.* FROM marks o LEFT JOIN courses c ON c.course_code = o.course_code WHERE c.course_id IS NULL;
-- CHECK 3: no student_id without a student (must return no row):
-- SELECT n.* FROM marks_new n LEFT JOIN students s ON s.user_id = n.student_id WHERE s.user_id IS NULL;

-- (course_materials is mapped in the same way: create course_materials_new with
--  materialID INT AUTO_INCREMENT PK, courseID INT NOT NULL, description VARCHAR(255), notes TEXT, referenceText TEXT,
--  copy with INSERT ... SELECT, compare counts.)

-- -----------------------------------------------------------------------------
-- E. ADD THE FOREIGN KEYS to the new table (only after checks 1-3 above are clean)
-- -----------------------------------------------------------------------------
-- ALTER TABLE marks_new
--     ADD CONSTRAINT fk_lm_marks_student FOREIGN KEY (student_id) REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
--     ADD CONSTRAINT fk_lm_marks_course  FOREIGN KEY (course_id)  REFERENCES courses  (course_id) ON UPDATE CASCADE ON DELETE CASCADE;

-- -----------------------------------------------------------------------------
-- F. SWAP in ONE atomic statement (the old table is kept as marks_legacy_old; nothing is dropped)
-- -----------------------------------------------------------------------------
-- RENAME TABLE marks TO marks_legacy_old, marks_new TO marks;
-- Afterwards run migrations 005, 007 and 010 again (they add the missing constraints/checks) and
-- database/audit_existing_database.sql. Drop marks_legacy_old / *_legacy_backup MANUALLY, later, only when you
-- are sure no record is missing (the mysqldump file is your safety net).
-- =============================================================================