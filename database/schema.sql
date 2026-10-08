-- =============================================================================
-- Faculty of Technology Academic Management System (FoT-AMS)
-- University of Ruhuna - ICT2132 Object Oriented Programming Practicum
-- Group 12
--
-- Shared database foundation (Member 1 / Leader):
--   departments, users, lecturers, students, courses
--
-- Members 2, 3 and 4 add their own tables (marks, attendance, medical,
-- enrollment, notices, timetables, ...) AFTER this file, referencing:
--   users(user_id), students(user_id), lecturers(user_id),
--   departments(department_id), courses(course_id)
--
-- Run:  mysql -u root -p < database/schema.sql
-- =============================================================================

DROP DATABASE IF EXISTS faculty_ams;
CREATE DATABASE faculty_ams CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE faculty_ams;

-- -----------------------------------------------------------------------------
-- Departments
-- -----------------------------------------------------------------------------
CREATE TABLE departments (
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
CREATE TABLE users (
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
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT fk_users_department FOREIGN KEY (department_id)
                           REFERENCES departments (department_id)
                           ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_users_role ON users (role);

-- -----------------------------------------------------------------------------
-- Lecturer-specific data (1:1 with users where role = LECTURER)
-- -----------------------------------------------------------------------------
CREATE TABLE lecturers (
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
CREATE TABLE students (
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
CREATE TABLE courses (
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

CREATE INDEX idx_courses_level_semester ON courses (academic_level, semester);