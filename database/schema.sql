-- =============================================================================
-- Faculty of Technology Academic Management System (FoT-AMS)
-- University of Ruhuna - ICT2132 Object Oriented Programming Practicum
-- Group 12
--
-- Shared database foundation (Member 1 / Leader):
--   departments (+ head_user_id), users, lecturers, students, courses,
--   notices, timetables          (notices / timetables are used by the Admin module)
--
-- Members 2, 3 and 4 add their own tables (marks, attendance, medical,
-- enrollment, ...) AFTER this file, referencing:
--   users(user_id), students(user_id), lecturers(user_id),
--   departments(department_id), courses(course_id)
--
-- Run (as a MySQL account that may create databases, e.g. your admin account):
--   mysql -u <admin_user> -p < database/schema.sql
-- This script never drops anything: it only creates what is missing, so it is safe to
-- re-run on an existing database. A database created before notices / timetables /
-- departments.head_user_id existed is brought up to date by simply running it again
-- (database/migrations/ holds the same two changes as stand-alone scripts). To wipe a throw-away development database, use
-- database/dev_reset_database.sql (separate file, deliberately NOT part of normal setup).
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
-- notices : admin notices, read by every role. If another member already created a
-- notices table, IF NOT EXISTS leaves it alone (merge column names by hand).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notices (
    notice_id   INT          NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150) NOT NULL,
    content     TEXT         NOT NULL,
    notice_date DATE         NOT NULL,
    created_by  INT          NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notices PRIMARY KEY (notice_id),
    INDEX idx_notices_date (notice_date),
    CONSTRAINT fk_notices_user FOREIGN KEY (created_by)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
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
