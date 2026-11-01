-- =============================================================================
-- Faculty of Technology Academic Management System (FoT-AMS)
-- University of Ruhuna - ICT2132 Object Oriented Programming Practicum
-- Group 12
--
-- AUTHORITATIVE SCHEMA - the single definition of every table the application uses, in its
-- FINAL shape (all columns that migrations 001-010 add are already included):
--
--   foundation   departments (+ head_user_id), users, lecturers, students, courses
--   admin        notices (+ category, priority), notice_reads, timetables
--   enrolment    enrollments (+ status, dropped_at)
--   attendance   course_component, session_attendance, medical (+ request / evidence / decision columns)
--   marks        evaluation_component, component_marks, marks, course_materials
--   history      schema_migrations
--
-- Every DAO reads and writes exactly these columns. `marks` and `course_materials` are used by BOTH
-- the Lecturer and the Undergraduate modules and have ONE shape (marks.marksid, student_id, course_id,
-- obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at;
-- course_materials.materialID, courseID, description, notes, referenceText).
--
-- FRESH INSTALL (database does not exist yet):
--   mysql -u <admin_user> -p -e "source database/schema.sql"
--   then migrations 000 ... 010 (they find everything in place and only record themselves).
--
-- EXISTING DATABASE: do NOT rely on this file to change existing tables. CREATE TABLE IF NOT EXISTS
-- leaves a table exactly as it is. Back up, run database/audit_existing_database.sql, then run the
-- numbered migrations in database/migrations/ (README.md, "Upgrade an existing database"). They add what
-- is missing, without dropping, renaming or overwriting anything.
--
-- This script never drops anything and is safe to run again. If `marks` or `course_materials` already
-- exists with another module's columns, the last section STOPS with an error and changes nothing.
-- To wipe a throw-away development database use database/dev_reset_database.sql (separate file,
-- deliberately NOT part of normal setup).
-- =============================================================================

CREATE DATABASE IF NOT EXISTS faculty_ams CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE faculty_ams;

-- -----------------------------------------------------------------------------
-- Departments
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
                             department_id   INT          NOT NULL AUTO_INCREMENT,
                             department_code VARCHAR(10)  NOT NULL,
                             department_name VARCHAR(100) NOT NULL,
                             created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT pk_departments PRIMARY KEY (department_id),
                             CONSTRAINT uq_departments_code UNIQUE (department_code),
                             CONSTRAINT uq_departments_name UNIQUE (department_name)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Users (common data for every role)
-- password_hash stores a salted PBKDF2 hash produced by security.PasswordUtil
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                       user_id         INT          NOT NULL AUTO_INCREMENT,
                       username        VARCHAR(30)  NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(100) NOT NULL,
                       email           VARCHAR(100) NOT NULL,
                       role            ENUM('ADMIN', 'LECTURER', 'TECHNICAL_OFFICER', 'STUDENT') NOT NULL,
                       contact_number  VARCHAR(15)  NULL,
                       profile_picture VARCHAR(255) NULL,
                       department_id   INT          NULL,
                       is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT pk_users PRIMARY KEY (user_id),
                       INDEX idx_users_role (role),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT fk_users_department FOREIGN KEY (department_id)
                           REFERENCES departments (department_id)
                           ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;


-- -----------------------------------------------------------------------------
-- Lecturer-specific data (1:1 with users where role = LECTURER)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lecturers (
                           user_id     INT         NOT NULL,
                           designation VARCHAR(50) NOT NULL DEFAULT 'Lecturer',
                           CONSTRAINT pk_lecturers PRIMARY KEY (user_id),
                           CONSTRAINT fk_lecturers_user FOREIGN KEY (user_id)
                               REFERENCES users (user_id)
                               ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Student (undergraduate) specific data (1:1 with users where role = STUDENT)
-- student_status covers regular, repeat and batch-missed undergraduates
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
                          user_id         INT         NOT NULL,
                          registration_no VARCHAR(20) NOT NULL,
                          batch           VARCHAR(10) NOT NULL,
                          student_status  ENUM('REGULAR', 'REPEAT', 'BATCH_MISSED') NOT NULL DEFAULT 'REGULAR',
                          CONSTRAINT pk_students PRIMARY KEY (user_id),
                          CONSTRAINT uq_students_registration_no UNIQUE (registration_no),
                          CONSTRAINT fk_students_user FOREIGN KEY (user_id)
                              REFERENCES users (user_id)
                              ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Courses (course units). Every course belongs to a department.
-- lecturer_id = course coordinator / lecturer in charge (optional)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
                         course_id      INT          NOT NULL AUTO_INCREMENT,
                         course_code    VARCHAR(10)  NOT NULL,
                         course_name    VARCHAR(100) NOT NULL,
                         credits        TINYINT      NOT NULL,
                         academic_level TINYINT      NOT NULL,
                         semester       TINYINT      NOT NULL,
                         course_type    ENUM('THEORY', 'PRACTICAL', 'THEORY_AND_PRACTICAL') NOT NULL DEFAULT 'THEORY_AND_PRACTICAL',
                         department_id  INT          NOT NULL,
                         lecturer_id    INT          NULL,
                         created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT pk_courses PRIMARY KEY (course_id),
                         INDEX idx_courses_level_semester (academic_level, semester),
                         CONSTRAINT uq_courses_code UNIQUE (course_code),
                         CONSTRAINT chk_courses_credits CHECK (credits BETWEEN 1 AND 6),
                         CONSTRAINT chk_courses_level CHECK (academic_level BETWEEN 1 AND 4),
                         CONSTRAINT chk_courses_semester CHECK (semester IN (1, 2)),
                         CONSTRAINT fk_courses_department FOREIGN KEY (department_id)
                             REFERENCES departments (department_id)
                             ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT fk_courses_lecturer FOREIGN KEY (lecturer_id)
                             REFERENCES lecturers (user_id)
                             ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;


-- -----------------------------------------------------------------------------
-- departments.head_user_id : the lecturer who heads a department (nullable).
-- Added with ALTER only when the column is missing, so existing rows are untouched.
-- -----------------------------------------------------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'departments'
                      AND COLUMN_NAME = 'head_user_id');

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE departments
         ADD COLUMN head_user_id INT NULL,
         ADD CONSTRAINT fk_departments_head FOREIGN KEY (head_user_id)
             REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL',
    'SELECT ''departments.head_user_id already exists'' AS info');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- -----------------------------------------------------------------------------
-- notices : admin notices, read by every role. category / priority are used by the student notice board.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notices (
    notice_id   INT          NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150) NOT NULL,
    content     TEXT         NOT NULL,
    notice_date DATE         NOT NULL,
    created_by  INT          NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category    VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    priority    ENUM('NORMAL', 'IMPORTANT', 'URGENT') NOT NULL DEFAULT 'NORMAL',
    CONSTRAINT pk_notices PRIMARY KEY (notice_id),
    INDEX idx_notices_date (notice_date),
    CONSTRAINT fk_notices_user FOREIGN KEY (created_by)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

-- each student's private read state of a notice
CREATE TABLE IF NOT EXISTS notice_reads (
    user_id   INT       NOT NULL,
    notice_id INT       NOT NULL,
    read_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, notice_id),
    CONSTRAINT fk_notice_reads_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_notice_reads_notice FOREIGN KEY (notice_id)
        REFERENCES notices (notice_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- timetables : class slots. Department and semester come from the slot's course.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS timetables (
    timetable_id INT         NOT NULL AUTO_INCREMENT,
    course_id    INT         NOT NULL,
    day_of_week  ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    venue        VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_timetables PRIMARY KEY (timetable_id),
    INDEX idx_timetables_day (day_of_week, start_time),
    CONSTRAINT chk_timetables_time CHECK (end_time > start_time),
    CONSTRAINT fk_timetables_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- enrollments : which students take which course. DROPPED keeps the row (history) instead of deleting it.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS enrollments (
    student_id  INT       NOT NULL,
    course_id   INT       NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status      ENUM('ENROLLED', 'DROPPED') NOT NULL DEFAULT 'ENROLLED',
    dropped_at  TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_lm_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_enrollments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Attendance (Technical Officer module)
--   course_component   a course has at most one THEORY and one PRACTICAL component
--   session_attendance one row per student, component and session number 1-15 (status present/absent/late)
--   medical            filed against ONE absent session; approval_status pending/approved/rejected.
--                      reviewed_by is NULL until a decision; submitted_by is the officer who filed it
--                      (NULL when the student filed it); request_ref groups the rows of one student request.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_component (
    component_id INT NOT NULL AUTO_INCREMENT,
    course_id    INT NOT NULL,
    type         ENUM('THEORY', 'PRACTICAL') NOT NULL,
    PRIMARY KEY (component_id),
    CONSTRAINT uq_course_component UNIQUE (course_id, type),
    CONSTRAINT fk_course_component_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS session_attendance (
    attendance_id INT         NOT NULL AUTO_INCREMENT,
    user_id       INT         NOT NULL,
    component_id  INT         NOT NULL,
    session_no    INT         NOT NULL,
    date          DATE        NOT NULL,
    status        VARCHAR(10) NOT NULL,
    PRIMARY KEY (attendance_id),
    CONSTRAINT uq_session_attendance UNIQUE (user_id, component_id, session_no),
    INDEX idx_session_attendance_date (date),
    CONSTRAINT chk_attendance_status CHECK (status IN ('present', 'absent', 'late')),
    CONSTRAINT chk_attendance_session_no CHECK (session_no BETWEEN 1 AND 15),
    CONSTRAINT fk_session_attendance_student FOREIGN KEY (user_id) REFERENCES students (user_id),
    CONSTRAINT fk_session_attendance_component FOREIGN KEY (component_id) REFERENCES course_component (component_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS medical (
    medical_id        INT          NOT NULL AUTO_INCREMENT,
    attendance_id     INT          NOT NULL,
    reviewed_by       INT          NULL,
    submitted_date    DATE         NOT NULL,
    reason            VARCHAR(255) NOT NULL,
    approval_status   VARCHAR(10)  NOT NULL DEFAULT 'pending',
    request_ref       VARCHAR(40)  NULL,
    doctor            VARCHAR(100) NULL,
    certificate_no    VARCHAR(50)  NULL,
    notes             VARCHAR(500) NULL,
    certificate_files TEXT         NULL,
    reviewer_remarks  VARCHAR(255) NULL,
    submitted_by      INT          NULL,
    evidence_name     VARCHAR(255) NULL,
    evidence_file     VARCHAR(100) NULL,
    rejection_reason  VARCHAR(255) NULL,
    decided_at        DATETIME     NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (medical_id),
    CONSTRAINT uq_medical_attendance UNIQUE (attendance_id),
    INDEX idx_medical_request_ref (request_ref),
    INDEX idx_medical_status (approval_status),
    CONSTRAINT chk_medical_status CHECK (approval_status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT fk_medical_attendance FOREIGN KEY (attendance_id) REFERENCES session_attendance (attendance_id),
    CONSTRAINT fk_medical_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (user_id),
    CONSTRAINT fk_medical_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (user_id)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Marks (Lecturer + Undergraduate modules)
--   evaluation_component  assessment scheme of a course; weightages of one course total 100
--   component_marks       one mark per student and component
--   marks                 one calculated result per student and course
--                         obtainMarks = CA percentage, finalMarks = final-exam percentage (both 0-100),
--                         overall_mark 0-100, grade_points 0.00-4.00
--   course_materials      notes / references a lecturer adds to a course
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
    CONSTRAINT chk_marks_ca_range CHECK (obtainMarks BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_final_range CHECK (finalMarks BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_overall_range CHECK (overall_mark IS NULL OR overall_mark BETWEEN 0 AND 100),
    CONSTRAINT chk_marks_points_range CHECK (grade_points IS NULL OR grade_points BETWEEN 0 AND 4),
    CONSTRAINT fk_lm_marks_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lm_marks_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS course_materials (
    materialID    INT          NOT NULL AUTO_INCREMENT,
    courseID      INT          NOT NULL,
    description   VARCHAR(255) NULL,
    notes         TEXT         NULL,
    referenceText TEXT         NULL,
    week_no       TINYINT      NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (materialID),
    CONSTRAINT fk_lm_course_materials_course FOREIGN KEY (courseID)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- Files uploaded by a lecturer with a material (bytes stored in the database; limit 10 MB in the application).
CREATE TABLE IF NOT EXISTS course_material_files (
    materialID  INT          NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT       NOT NULL,
    file_data   LONGBLOB     NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (materialID),
    CONSTRAINT fk_cmf_material FOREIGN KEY (materialID)
        REFERENCES course_materials (materialID) ON UPDATE CASCADE ON DELETE CASCADE) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS assignments (
    assignment_id INT          NOT NULL AUTO_INCREMENT,
    course_id     INT          NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NULL,
    due_at        DATETIME     NOT NULL,
    cutoff_at     DATETIME     NULL,
    allow_late    BOOLEAN      NOT NULL DEFAULT FALSE,
    max_marks     DECIMAL(7,2) NULL,
    allowed_types VARCHAR(255) NOT NULL DEFAULT 'pdf,docx,zip,java',
    max_file_mb   SMALLINT     NOT NULL DEFAULT 20,
    max_files     SMALLINT     NOT NULL DEFAULT 5,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id),
    INDEX idx_assignments_course_due (course_id, due_at),
    CONSTRAINT fk_student_assignments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS submissions (
    submission_id INT          NOT NULL AUTO_INCREMENT,
    assignment_id INT          NOT NULL,
    student_id    INT          NOT NULL,
    comment       TEXT         NULL,
    submitted_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('SUBMITTED', 'LATE', 'GRADED') NOT NULL DEFAULT 'SUBMITTED',
    marks         DECIMAL(7,2) NULL,
    feedback      TEXT         NULL,
    graded_at     DATETIME     NULL,
    PRIMARY KEY (submission_id),
    CONSTRAINT uq_student_assignment_submission UNIQUE (assignment_id, student_id),
    INDEX idx_submissions_student (student_id, submitted_at),
    CONSTRAINT fk_student_submissions_assignment FOREIGN KEY (assignment_id)
        REFERENCES assignments (assignment_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_student_submissions_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS submission_files (
    file_id       INT          NOT NULL AUTO_INCREMENT,
    submission_id INT          NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_path   VARCHAR(1024) NOT NULL,
    file_size     BIGINT       NOT NULL,
    PRIMARY KEY (file_id),
    INDEX idx_submission_files_submission (submission_id),
    CONSTRAINT fk_submission_files_submission FOREIGN KEY (submission_id)
        REFERENCES submissions (submission_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- Migration history: every numbered migration records itself here.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schema_migrations (
    migration_id     VARCHAR(100) NOT NULL,
    description      VARCHAR(255) NOT NULL,
    first_applied_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    run_count        INT          NOT NULL DEFAULT 1,
    applied_by       VARCHAR(100) NULL,
    PRIMARY KEY (migration_id)
) ENGINE = InnoDB;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('schema.sql', 'Authoritative schema (final shape of every table)', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;

-- -----------------------------------------------------------------------------
-- Compatibility guard. CREATE TABLE IF NOT EXISTS silently keeps a table that already exists,
-- even one with another module's columns. For the two tables known to have been defined differently
-- by another branch (marks, course_materials) that would make every Lecturer / Undergraduate screen
-- fail later with "Unknown column". So the script STOPS here, with nothing changed, if either exists
-- without its key columns. Follow README.md, "Recovery".
-- -----------------------------------------------------------------------------
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME IN ('marksid', 'student_id', 'course_id', 'obtainMarks', 'finalMarks')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks exists but lacks key column(s) marksid, student_id, course_id, obtainMarks, finalMarks. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME IN ('materialID', 'courseID', 'description', 'notes', 'referenceText')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_materials exists but lacks key column(s) materialID, courseID, description, notes, referenceText. It belongs to a different design (for example columns course_code / score). NOTHING was renamed or changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_materials_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT 'schema.sql finished - every table of the authoritative schema exists' AS result;