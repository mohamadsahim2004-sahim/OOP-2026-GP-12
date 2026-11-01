-- =============================================================================
-- Migration 000 - migration history table
--
-- Creates schema_migrations, the table in which every migration records that it ran
-- (migration_id, first_applied_at, last_run_at, run_count, applied_by).
-- Every other migration also creates it if it is missing, so the order of 000 does not matter;
-- this file only exists so the history table can be created on its own.
--
-- Safe to run any number of times. Changes no existing row and no other table.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/000_schema_migrations.sql"

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

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('000_schema_migrations', 'Migration history table', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 000_schema_migrations finished - see schema_migrations' AS result;