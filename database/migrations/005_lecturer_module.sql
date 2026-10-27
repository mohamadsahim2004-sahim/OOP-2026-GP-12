-- =============================================================================
-- Migration 005 - Lecturer module (marks, evaluation components, enrollment,
--                 course materials)
--
-- Safe to run more than once and on an existing faculty_ams database:
--   * tables are only created if missing,
--   * columns / constraints are only added if missing,
--   * no existing row is deleted or overwritten (one exception: a lecturer whose
--     password_hash is the invalid placeholder 'lec123' gets a real hash).
--
--   mysql -u root -p faculty_ams < database/migrations/005_lecturer_module.sql
--
-- Run AFTER schema.sql, sample_data.sql and migration 003 (course_component,
-- session_attendance, medical are used for attendance / eligibility).
--
-- NOTE FOR THE TEAM: the `marks` and `course_materials` names are shared with the
-- undergraduate branch (branch-aamir), whose schema defines DIFFERENT columns
-- (course_code, score, ...). Both modules cannot use one database until that is
-- reconciled. This script keeps the column names the Lecturer DAOs already used.
-- =============================================================================

USE faculty_ams;

-- -----------------------------------------------------------------------------
-- Compatibility guard.
-- The undergraduate branch uses tables with the SAME NAMES but DIFFERENT columns
-- (marks: course_code / score ..., course_materials: course_code / title ...).
-- "CREATE TABLE IF NOT EXISTS" would silently keep such a table and every Lecturer
-- screen would then fail with "Unknown column". A table that lacks the Lecturer key
-- columns is therefore RENAMED to <name>_other_module (nothing is deleted; undo with
-- RENAME TABLE <name>_other_module TO <name>;) and the Lecturer table is created below.
-- -----------------------------------------------------------------------------
-- marks: required columns marksid, student_id, course_id
SET @has   := (SELECT COUNT(*) FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks');
SET @keys  := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME IN ('marksid', 'student_id', 'course_id'));
SET @taken := (SELECT COUNT(*) FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks_other_module');
SET @ddl := IF(@has > 0 AND @keys < 3 AND @taken = 0,
    'RENAME TABLE marks TO marks_other_module',
    IF(@has > 0 AND @keys < 3,
       'SELECT ''WARNING: marks has an incompatible structure and marks_other_module already exists - rename or drop one of them by hand, then run this script again'' AS warning',
       'SELECT ''marks: no incompatible table found'' AS info'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- course_materials: required columns materialID, courseID
SET @has   := (SELECT COUNT(*) FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials');
SET @keys  := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME IN ('materialID', 'courseID'));
SET @taken := (SELECT COUNT(*) FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials_other_module');
SET @ddl := IF(@has > 0 AND @keys < 2 AND @taken = 0,
    'RENAME TABLE course_materials TO course_materials_other_module',
    IF(@has > 0 AND @keys < 2,
       'SELECT ''WARNING: course_materials has an incompatible structure and course_materials_other_module already exists - rename or drop one of them by hand, then run this script again'' AS warning',
       'SELECT ''course_materials: no incompatible table found'' AS info'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- Enrollment: which students take which course (used to scope lecturer access)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS enrollments (
    student_id INT       NOT NULL,
    course_id  INT       NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_lm_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_enrollments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Evaluation components (assessment scheme of a course).
--   category   CA / ASSIGNMENT / PRACTICAL count towards the CA part,
--              FINAL_EXAM is the final examination part.
--   weightage  percentage of the FINAL course mark; the weightages of one course
--              must add up to exactly 100 before marks can be entered.
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- Final results per student and course (column names unchanged from MarksDAO).
--   obtainMarks = CA percentage (0-100), finalMarks = final-exam percentage (0-100)
-- -----------------------------------------------------------------------------
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

-- If `marks` already existed (older lecturer database), add every column that is missing.
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'obtainMarks');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN obtainMarks DECIMAL(5,2) NOT NULL DEFAULT 0',
    'SELECT ''marks.obtainMarks already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'finalMarks');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN finalMarks DECIMAL(5,2) NOT NULL DEFAULT 0',
    'SELECT ''marks.finalMarks already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'grade');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN grade VARCHAR(3) NULL',
    'SELECT ''marks.grade already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'isEligible');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN isEligible BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT ''marks.isEligible already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'overall_mark');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN overall_mark DECIMAL(5,2) NULL',
    'SELECT ''marks.overall_mark already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'grade_points');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN grade_points DECIMAL(3,2) NULL',
    'SELECT ''marks.grade_points already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'published');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN published TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT ''marks.published already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'published_at');
SET @ddl := IF(@col = 0,
    'ALTER TABLE marks ADD COLUMN published_at DATETIME NULL',
    'SELECT ''marks.published_at already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- One result row per student and course. Only added when it cannot fail:
-- if duplicates already exist, they are LEFT UNTOUCHED and this step is skipped.
SET @idx  := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks'
                AND INDEX_NAME = 'uq_marks_student_course');
SET @dups := (SELECT COUNT(*) FROM
              (SELECT 1 FROM marks GROUP BY student_id, course_id HAVING COUNT(*) > 1) d);
SET @ddl := IF(@idx = 0 AND @dups = 0,
    'ALTER TABLE marks ADD CONSTRAINT uq_marks_student_course UNIQUE (student_id, course_id)',
    IF(@idx = 0,
       'SELECT ''SKIPPED unique index on marks: duplicate (student_id, course_id) rows exist - resolve them, then re-run this script'' AS warning',
       'SELECT ''marks unique index already exists'' AS info'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- Course materials (column names unchanged from CourseMaterialDAO)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- Seed fix: sample_data.sql used to store the plain text 'lec123' for lec.perera,
-- which can never verify (login expects a PBKDF2 hash). Password: lecturer123
-- -----------------------------------------------------------------------------
UPDATE users
   SET password_hash = 'pbkdf2$65536$FdpS9+ZsGAbslDVUlTaMIg==$RV3chOlbB18L4C/ablgIGrtH17XQf8XfX4/pRTLReG8='
 WHERE username = 'lec.perera' AND password_hash = 'lec123';