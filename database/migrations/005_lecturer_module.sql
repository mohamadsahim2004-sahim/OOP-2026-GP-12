-- =============================================================================
-- Migration 005 - Lecturer module (enrollments, evaluation components, marks, course materials)
--
-- Creates enrollments, evaluation_component, component_marks, marks and course_materials and
-- adds any missing `marks` column.
--
--   * Repeatable and non-destructive: tables are created only when missing, columns are added
--     only when missing. No row is deleted or overwritten.
--   * `marks` and `course_materials` are shared by the Lecturer and Undergraduate modules and
--     there is ONE schema for both (the column names MarksDAO / CourseMaterialDAO / StudentResultDAO /
--     StudentMaterialDAO all use: marks.marksid, student_id, course_id, obtainMarks, finalMarks, ...;
--     course_materials.materialID, courseID, description, notes, referenceText).
--   * CHANGED: older versions of this script RENAMED an incompatible `marks` / `course_materials`
--     table to <name>_other_module without copying its rows, which hid the rows from every screen.
--     This version never renames anything. If such a table exists and is incompatible the script
--     STOPS with an error and changes nothing; follow README.md, "Recovery" (back up, create a
--     compatible table, copy, compare counts, only then swap).
--   * Needs: schema.sql (users, students, courses). Run after 003 for attendance-based eligibility.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/005_lecturer_module.sql"

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
SET @s := IF(@bad, 'SELECT ''ABORT: table users does not exist. Run database/schema.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table students does not exist. Run database/schema.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__students_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table courses does not exist. Run database/schema.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__courses_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME IN ('marksid', 'student_id', 'course_id')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks exists but lacks key column(s) marksid, student_id, course_id. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME IN ('student_id', 'course_id') AND DATA_TYPE NOT IN ('int', 'bigint', 'mediumint', 'smallint', 'tinyint')) > 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks: key column(s) student_id, course_id are not integer columns. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_key_types__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME IN ('materialID', 'courseID')) < 2);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_materials exists but lacks key column(s) materialID, courseID. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_materials_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME IN ('student_id', 'course_id')) < 2);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments exists but lacks key column(s) student_id, course_id. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component' AND COLUMN_NAME IN ('eval_component_id', 'course_id', 'component_name', 'category', 'max_marks', 'weightage')) < 6);
SET @s := IF(@bad, 'SELECT ''ABORT: table evaluation_component exists but lacks key column(s) eval_component_id, course_id, component_name, category, max_marks, weightage. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__evaluation_component_incompatible__see_README`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks' AND COLUMN_NAME IN ('student_id', 'eval_component_id', 'marks_obtained')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table component_marks exists but lacks key column(s) student_id, eval_component_id, marks_obtained. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__component_marks_incompatible__see_README`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- Enrollment: which students take which course (scopes lecturer access; 007 adds status / dropped_at).
CREATE TABLE IF NOT EXISTS enrollments (
    student_id  INT       NOT NULL,
    course_id   INT       NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_lm_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_enrollments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- Evaluation components (assessment scheme of a course).
--   category   CA / ASSIGNMENT / PRACTICAL count towards the CA part, FINAL_EXAM is the final examination.
--   weightage  percentage of the FINAL course mark; the weightages of one course must total 100.
CREATE TABLE IF NOT EXISTS evaluation_component (
    eval_component_id INT          NOT NULL AUTO_INCREMENT,
    course_id         INT          NOT NULL,
    component_name    VARCHAR(60)  NOT NULL,
    category          ENUM('CA', 'ASSIGNMENT', 'PRACTICAL', 'FINAL_EXAM') NOT NULL,
    max_marks         DECIMAL(6,2) NOT NULL,
    weightage         DECIMAL(5,2) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eval_component_id),
    CONSTRAINT uq_eval_component_name UNIQUE (course_id, component_name),
    CONSTRAINT chk_lm_eval_max CHECK (max_marks > 0),
    CONSTRAINT chk_lm_eval_weight CHECK (weightage > 0 AND weightage <= 100),
    CONSTRAINT fk_lm_eval_component_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- One mark per student per component (the primary key prevents duplicates).
CREATE TABLE IF NOT EXISTS component_marks (
    student_id        INT          NOT NULL,
    eval_component_id INT          NOT NULL,
    marks_obtained    DECIMAL(6,2) NOT NULL,
    entered_by        INT          NULL,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, eval_component_id),
    CONSTRAINT chk_lm_component_marks_nonneg CHECK (marks_obtained >= 0),
    CONSTRAINT fk_lm_component_marks_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_component_marks_component FOREIGN KEY (eval_component_id)
        REFERENCES evaluation_component (eval_component_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_lm_component_marks_user FOREIGN KEY (entered_by)
        REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

-- Final results per student and course (column names are those MarksDAO and StudentResultDAO use).
--   obtainMarks = CA percentage (0-100), finalMarks = final-exam percentage (0-100),
--   overall_mark = weighted overall (0-100), grade_points = 0.00-4.00.
CREATE TABLE IF NOT EXISTS marks (
    marksid      INT          NOT NULL AUTO_INCREMENT,
    student_id   INT          NOT NULL,
    course_id    INT          NOT NULL,
    obtainMarks  DECIMAL(5,2) NOT NULL DEFAULT 0,
    finalMarks   DECIMAL(5,2) NOT NULL DEFAULT 0,
    grade        VARCHAR(3)   NULL,
    isEligible   BOOLEAN      NOT NULL DEFAULT FALSE,
    overall_mark DECIMAL(5,2) NULL,
    grade_points DECIMAL(3,2) NULL,
    published    TINYINT(1)   NOT NULL DEFAULT 0,
    published_at DATETIME     NULL,
    PRIMARY KEY (marksid),
    CONSTRAINT uq_marks_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_lm_marks_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_marks_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- If `marks` already existed with the key columns (older lecturer database), add every column that is missing.
-- column marks.obtainMarks
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'obtainMarks') = 0, 'ALTER TABLE `marks` ADD COLUMN `obtainMarks` DECIMAL(5,2) NOT NULL DEFAULT 0', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.finalMarks
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'finalMarks') = 0, 'ALTER TABLE `marks` ADD COLUMN `finalMarks` DECIMAL(5,2) NOT NULL DEFAULT 0', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.grade
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'grade') = 0, 'ALTER TABLE `marks` ADD COLUMN `grade` VARCHAR(3) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.isEligible
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'isEligible') = 0, 'ALTER TABLE `marks` ADD COLUMN `isEligible` BOOLEAN NOT NULL DEFAULT FALSE', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.overall_mark
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'overall_mark') = 0, 'ALTER TABLE `marks` ADD COLUMN `overall_mark` DECIMAL(5,2) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.grade_points
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'grade_points') = 0, 'ALTER TABLE `marks` ADD COLUMN `grade_points` DECIMAL(3,2) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.published
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'published') = 0, 'ALTER TABLE `marks` ADD COLUMN `published` TINYINT(1) NOT NULL DEFAULT 0', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column marks.published_at
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'published_at') = 0, 'ALTER TABLE `marks` ADD COLUMN `published_at` DATETIME NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- One result row per student and course. If duplicates already exist they are LEFT UNTOUCHED, the
-- key is not added and a WARNING row is printed (resolve the duplicates, then run this script again).
-- unique key marks(student_id, course_id)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 2 AND SUM(COLUMN_NAME IN ('student_id', 'course_id')) = 2) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `marks` GROUP BY `student_id`, `course_id` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_marks_student_course on marks(student_id, course_id) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `uq_marks_student_course` UNIQUE (`student_id`, `course_id`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- Course materials (column names are those CourseMaterialDAO and StudentMaterialDAO use).
CREATE TABLE IF NOT EXISTS course_materials (
    materialID    INT          NOT NULL AUTO_INCREMENT,
    courseID      INT          NOT NULL,
    description   VARCHAR(255) NULL,
    notes         TEXT         NULL,
    referenceText TEXT         NULL,
    PRIMARY KEY (materialID),
    CONSTRAINT fk_lm_course_materials_course FOREIGN KEY (courseID)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- Seed fix: the old sample_data.sql stored the plain text 'lec123' for lec.perera, which can never
-- verify (login expects a PBKDF2 hash). Only that exact demo account with that exact invalid value is touched.
-- Password after the fix: lecturer123
UPDATE users
   SET password_hash = 'pbkdf2$65536$FdpS9+ZsGAbslDVUlTaMIg==$RV3chOlbB18L4C/ablgIGrtH17XQf8XfX4/pRTLReG8='
 WHERE username = 'lec.perera' AND email = 'lec.perera@fot.example.lk' AND password_hash = 'lec123';

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('005_lecturer_module', 'enrollments, evaluation_component, component_marks, marks, course_materials', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 005_lecturer_module finished - see schema_migrations' AS result;