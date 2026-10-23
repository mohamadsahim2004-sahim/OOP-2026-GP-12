-- =============================================================================
-- Migration 002 - Head of Department
--
-- Adds departments.head_user_id (nullable, points at the lecturer who heads the
-- department). Safe to run more than once and never touches existing rows.
--
--   mysql -u root -p faculty_ams < database/migrations/002_department_head.sql
-- =============================================================================

USE faculty_ams;

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