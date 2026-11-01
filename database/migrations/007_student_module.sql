-- =============================================================================
-- Migration 007 - Undergraduate (student) module additions
--
-- Adds only what the student screens need on top of the shared tables:
--   enrollments   status, dropped_at           (enroll / drop instead of delete)
--   notices       category, priority           (notice board filters and badges)
--   notice_reads  new table                    (each student's private read state)
--   medical       reviewed_by nullable, request_ref, doctor, certificate_no, notes,
--                 certificate_files, reviewer_remarks (+ index on request_ref)
--
--   * Repeatable: every column and index is checked on its own, so a half-applied earlier run is completed.
--   * No row is changed or deleted.
--   * Needs: schema.sql, 001 (notices), 003 (medical), 005 (enrollments).
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/007_student_module.sql"

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

SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments does not exist. Run database/migrations/005_lecturer_module.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table notices does not exist. Run database/schema.sql (or 001_notices_timetables.sql) first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notices_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical does not exist. Run database/migrations/003_attendance_medical.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table users does not exist. Run database/schema.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- enrollments
-- column enrollments.status
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'status') = 0, 'ALTER TABLE `enrollments` ADD COLUMN `status` ENUM(''ENROLLED'', ''DROPPED'') NOT NULL DEFAULT ''ENROLLED''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column enrollments.dropped_at
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'dropped_at') = 0, 'ALTER TABLE `enrollments` ADD COLUMN `dropped_at` TIMESTAMP NULL DEFAULT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- notices
-- column notices.category
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices' AND COLUMN_NAME = 'category') = 0, 'ALTER TABLE `notices` ADD COLUMN `category` VARCHAR(30) NOT NULL DEFAULT ''GENERAL''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column notices.priority
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices' AND COLUMN_NAME = 'priority') = 0, 'ALTER TABLE `notices` ADD COLUMN `priority` ENUM(''NORMAL'', ''IMPORTANT'', ''URGENT'') NOT NULL DEFAULT ''NORMAL''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- notice_reads
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

-- medical: a pending request has no reviewer yet, so reviewed_by must allow NULL
-- medical.reviewed_by must allow NULL
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'reviewed_by' AND IS_NULLABLE = 'NO') > 0, 'ALTER TABLE `medical` MODIFY COLUMN `reviewed_by` INT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.request_ref
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'request_ref') = 0, 'ALTER TABLE `medical` ADD COLUMN `request_ref` VARCHAR(40) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.doctor
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'doctor') = 0, 'ALTER TABLE `medical` ADD COLUMN `doctor` VARCHAR(100) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.certificate_no
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'certificate_no') = 0, 'ALTER TABLE `medical` ADD COLUMN `certificate_no` VARCHAR(50) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.notes
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'notes') = 0, 'ALTER TABLE `medical` ADD COLUMN `notes` VARCHAR(500) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.certificate_files
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'certificate_files') = 0, 'ALTER TABLE `medical` ADD COLUMN `certificate_files` TEXT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.reviewer_remarks
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'reviewer_remarks') = 0, 'ALTER TABLE `medical` ADD COLUMN `reviewer_remarks` VARCHAR(255) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- index medical.idx_medical_request_ref
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND INDEX_NAME = 'idx_medical_request_ref') = 0, 'ALTER TABLE `medical` ADD INDEX `idx_medical_request_ref` (`request_ref`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('007_student_module', 'enrollment status, notice category/priority, notice_reads, medical request columns', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 007_student_module finished - see schema_migrations' AS result;