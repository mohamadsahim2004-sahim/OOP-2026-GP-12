USE faculty_ams;

SET @has_dob := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students' AND COLUMN_NAME = 'dob'
);
SET @sql := IF(@has_dob = 0,
    'ALTER TABLE students ADD COLUMN dob DATE NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('012_student_date_of_birth', 'nullable date of birth for student profiles', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
