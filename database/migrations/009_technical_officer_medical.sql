-- =============================================================================
-- Migration 009 - Technical Officer module - medical evidence, decision audit, 'late' status
--
-- Adds to `medical`: submitted_by, evidence_name, evidence_file, rejection_reason, decided_at,
-- created_at, makes reviewed_by nullable, adds the status CHECK and two indexes.
--
--   * REPEATABLE (older versions said "run ONCE"): every column, key and index is guarded, and
--     the one-time data conversion below runs ONLY in the run that adds submitted_by.
--   * One-time conversion (only when submitted_by did not exist yet):
--       - pending medicals had the submitting officer stored in reviewed_by; that value moves to
--         submitted_by and reviewed_by becomes NULL until a decision is made,
--       - already approved / rejected medicals keep their reviewer; decided_at is back-filled.
--     Re-running never repeats this, so student-submitted medicals (submitted_by NULL by design)
--     are never given a wrong submitter.
--   * Needs: 003 (and 007 if the student module is used).  Run BEFORE 004 (demo attendance).
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/009_technical_officer_medical.sql"

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

SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical does not exist. Run database/migrations/003_attendance_medical.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table session_attendance does not exist. Run database/migrations/003_attendance_medical.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__session_attendance_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table users does not exist. Run database/schema.sql first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- remember whether this is the run that introduces submitted_by (decides the one-time conversion)
SET @backfill := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'submitted_by') = 0, 1, 0);

-- medical.reviewed_by must allow NULL
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'reviewed_by' AND IS_NULLABLE = 'NO') > 0, 'ALTER TABLE `medical` MODIFY COLUMN `reviewed_by` INT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.submitted_by
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'submitted_by') = 0, 'ALTER TABLE `medical` ADD COLUMN `submitted_by` INT NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.evidence_name
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'evidence_name') = 0, 'ALTER TABLE `medical` ADD COLUMN `evidence_name` VARCHAR(255) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.evidence_file
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'evidence_file') = 0, 'ALTER TABLE `medical` ADD COLUMN `evidence_file` VARCHAR(100) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.rejection_reason
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'rejection_reason') = 0, 'ALTER TABLE `medical` ADD COLUMN `rejection_reason` VARCHAR(255) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.decided_at
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'decided_at') = 0, 'ALTER TABLE `medical` ADD COLUMN `decided_at` DATETIME NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- column medical.created_at
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'created_at') = 0, 'ALTER TABLE `medical` ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- one-time data conversion (skipped unless submitted_by was just added)
SET @s := IF(@backfill = 1, 'UPDATE medical SET submitted_by = reviewed_by WHERE submitted_by IS NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@backfill = 1, 'UPDATE medical SET reviewed_by = NULL WHERE approval_status = ''pending''', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@backfill = 1, 'UPDATE medical SET decided_at = CURRENT_TIMESTAMP WHERE approval_status <> ''pending'' AND decided_at IS NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- foreign key medical.submitted_by -> users.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'submitted_by' AND REFERENCED_TABLE_NAME = 'users');
SET @orph := (SELECT COUNT(*) FROM `medical` x WHERE x.`submitted_by` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `users` r WHERE r.`user_id` = x.`submitted_by`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_medical_submitted_by (medical.submitted_by -> users.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `fk_medical_submitted_by` FOREIGN KEY (`submitted_by`) REFERENCES `users` (`user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check medical.chk_medical_status
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND CONSTRAINT_NAME = 'chk_medical_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `medical` WHERE NOT (approval_status IN ('pending', 'approved', 'rejected')));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_medical_status on medical NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `chk_medical_status` CHECK (approval_status IN (''pending'', ''approved'', ''rejected''))'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- attendance status values are present / absent / late (column is VARCHAR(10), no change needed)
-- index session_attendance.idx_session_attendance_date
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND INDEX_NAME = 'idx_session_attendance_date') = 0, 'ALTER TABLE `session_attendance` ADD INDEX `idx_session_attendance_date` (`date`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- index medical.idx_medical_status
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND INDEX_NAME = 'idx_medical_status') = 0, 'ALTER TABLE `medical` ADD INDEX `idx_medical_status` (`approval_status`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('009_technical_officer_medical', 'medical evidence/decision columns, status check, indexes', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 009_technical_officer_medical finished - see schema_migrations' AS result;