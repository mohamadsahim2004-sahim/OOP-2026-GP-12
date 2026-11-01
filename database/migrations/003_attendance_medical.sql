-- =============================================================================
-- Migration 003 - Attendance and medical tables (Technical Officer module)
--
-- Creates course_component, session_attendance and medical (the shape used before migrations
-- 007 and 009 add their columns).
--
--   * Repeatable: tables are created only when missing. Nothing is dropped, renamed or deleted.
--   * If one of the three tables already exists but lacks its key columns it belongs to a
--     different design: this script STOPS with an error instead of ignoring it. Nothing is changed.
--     See README.md, "Recovery".
--   * Needs: schema.sql (users, students, courses).  Run before 004, 007 and 009.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/003_attendance_medical.sql"

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
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component' AND COLUMN_NAME IN ('component_id', 'course_id', 'type')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_component exists but lacks key column(s) component_id, course_id, type. Nothing was changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_component_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND COLUMN_NAME IN ('attendance_id', 'user_id', 'component_id', 'session_no', 'date', 'status')) < 6);
SET @s := IF(@bad, 'SELECT ''ABORT: table session_attendance exists but lacks key column(s) attendance_id, user_id, component_id, session_no, date, status. Nothing was changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__session_attendance_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME IN ('medical_id', 'attendance_id', 'reviewed_by', 'submitted_date', 'reason', 'approval_status')) < 6);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical exists but lacks key column(s) medical_id, attendance_id, reviewed_by, submitted_date, reason, approval_status. Nothing was changed. See README.md, Recovery.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_incompatible__see_README_Recovery`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- A course has at most one THEORY and one PRACTICAL component.
CREATE TABLE IF NOT EXISTS course_component (
    component_id INT NOT NULL AUTO_INCREMENT,
    course_id    INT NOT NULL,
    type         ENUM('THEORY', 'PRACTICAL') NOT NULL,
    PRIMARY KEY (component_id),
    CONSTRAINT uq_course_component UNIQUE (course_id, type),
    CONSTRAINT fk_course_component_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
) ENGINE = InnoDB;

-- One row per student, component and session (1-15). status: present / absent / late.
CREATE TABLE IF NOT EXISTS session_attendance (
    attendance_id INT         NOT NULL AUTO_INCREMENT,
    user_id       INT         NOT NULL,
    component_id  INT         NOT NULL,
    session_no    INT         NOT NULL,
    date          DATE        NOT NULL,
    status        VARCHAR(10) NOT NULL,
    PRIMARY KEY (attendance_id),
    CONSTRAINT uq_session_attendance UNIQUE (user_id, component_id, session_no),
    CONSTRAINT fk_session_attendance_student FOREIGN KEY (user_id) REFERENCES students (user_id),
    CONSTRAINT fk_session_attendance_component FOREIGN KEY (component_id) REFERENCES course_component (component_id)
) ENGINE = InnoDB;

-- A medical is filed against one ABSENT session; approval_status: pending / approved / rejected.
-- (007 and 009 make reviewed_by nullable and add the request / evidence / decision columns.)
CREATE TABLE IF NOT EXISTS medical (
    medical_id      INT          NOT NULL AUTO_INCREMENT,
    attendance_id   INT          NOT NULL,
    reviewed_by     INT          NOT NULL,
    submitted_date  DATE         NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    approval_status VARCHAR(10)  NOT NULL DEFAULT 'pending',
    PRIMARY KEY (medical_id),
    CONSTRAINT uq_medical_attendance UNIQUE (attendance_id),
    CONSTRAINT fk_medical_attendance FOREIGN KEY (attendance_id) REFERENCES session_attendance (attendance_id),
    CONSTRAINT fk_medical_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (user_id)
) ENGINE = InnoDB;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('003_attendance_medical', 'course_component, session_attendance, medical tables', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 003_attendance_medical finished - see schema_migrations' AS result;