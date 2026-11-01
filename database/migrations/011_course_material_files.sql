-- =============================================================================
-- Migration 011 - Uploaded lecture files (course_material_files)
--
-- Lets a lecturer upload a PDF / Word / PowerPoint file with a course material. The file's bytes are
-- stored in the database, so every student of the course can download it on any computer.
--
--   * Repeatable: the table is created only when missing. Nothing is dropped, renamed or deleted.
--   * Needs: schema.sql (course_materials). Run it after 010.
--   * Files are limited to 10 MB by the application; the MySQL setting max_allowed_packet must be at least
--     that (the MySQL 8 default is 64 MB, so nothing needs to be changed).
--   * If the table is missing the application still works; only file upload / download is unavailable and
--     the lecturer is told to run this script.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/011_course_material_files.sql"
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

CREATE TABLE IF NOT EXISTS course_material_files (
    materialID  INT          NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT       NOT NULL,
    file_data   LONGBLOB     NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (materialID),
    CONSTRAINT fk_cmf_material FOREIGN KEY (materialID)
        REFERENCES course_materials (materialID) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('011_course_material_files', 'course_material_files: uploaded lecture files', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 011_course_material_files finished - see schema_migrations' AS result;
