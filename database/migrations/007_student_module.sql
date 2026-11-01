-- =============================================================================
-- Migration 007 - Undergraduate (student) module additions
--
-- Run AFTER schema.sql, sample_data.sql and migrations 001, 002, 003 and 005.
-- Safe to run more than once: every change is guarded and no data is dropped.
--
--   mysql -u root -p faculty_ams < database/migrations/007_student_module.sql
--
-- Adds only what the student screens need on top of the shared tables:
--   enrollments     status, dropped_at           (enroll / drop instead of delete)
--   notices         category, priority           (notice board filters and badges)
--   notice_reads    new table                    (each student's private read state)
--   medical         request_ref, doctor, certificate_no, notes, certificate_files,
--                   reviewer_remarks, reviewed_by now nullable (pending requests)
-- =============================================================================

USE faculty_ams;

-- ---------------------------------------------------------------- enrollments
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'status');
SET @ddl := IF(@col = 0,
    'ALTER TABLE enrollments
         ADD COLUMN status ENUM(''ENROLLED'', ''DROPPED'') NOT NULL DEFAULT ''ENROLLED'',
         ADD COLUMN dropped_at TIMESTAMP NULL DEFAULT NULL',
    'SELECT ''enrollments.status already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------- notices
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices' AND COLUMN_NAME = 'priority');
SET @ddl := IF(@col = 0,
    'ALTER TABLE notices
         ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT ''GENERAL'',
         ADD COLUMN priority ENUM(''NORMAL'', ''IMPORTANT'', ''URGENT'') NOT NULL DEFAULT ''NORMAL''',
    'SELECT ''notices.priority already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------------- notice_reads
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

-- -------------------------------------------------------------------- medical
-- A pending request has no reviewer yet, so reviewed_by must allow NULL.
ALTER TABLE medical MODIFY reviewed_by INT NULL;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'request_ref');
SET @ddl := IF(@col = 0,
    'ALTER TABLE medical
         ADD COLUMN request_ref       VARCHAR(40)  NULL,
         ADD COLUMN doctor            VARCHAR(100) NULL,
         ADD COLUMN certificate_no    VARCHAR(50)  NULL,
         ADD COLUMN notes             VARCHAR(500) NULL,
         ADD COLUMN certificate_files TEXT         NULL,
         ADD COLUMN reviewer_remarks  VARCHAR(255) NULL,
         ADD INDEX idx_medical_request_ref (request_ref)',
    'SELECT ''medical.request_ref already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
