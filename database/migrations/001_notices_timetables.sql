-- =============================================================================
-- Migration 001 - Notices and Timetables (used by the Admin module)
--
-- Safe to run on an existing faculty_ams database: it only creates the two
-- tables if they are missing and never drops or alters existing data.
--
--   mysql -u root -p faculty_ams < database/migrations/001_notices_timetables.sql
--
-- Shared with the undergraduate module (Aamir): students only READ these tables.
-- If Aamir already has his own notices / timetables tables, merge the column
-- names below with his before running this script.
-- =============================================================================

USE faculty_ams;

CREATE TABLE IF NOT EXISTS notices (
    notice_id   INT          NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150) NOT NULL,
    content     TEXT         NOT NULL,
    notice_date DATE         NOT NULL,
    created_by  INT          NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notices PRIMARY KEY (notice_id),
    CONSTRAINT fk_notices_user FOREIGN KEY (created_by)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE INDEX idx_notices_date ON notices (notice_date);

-- Department and semester of a timetable slot come from its course.
CREATE TABLE IF NOT EXISTS timetables (
    timetable_id INT         NOT NULL AUTO_INCREMENT,
    course_id    INT         NOT NULL,
    day_of_week  ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    venue        VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_timetables PRIMARY KEY (timetable_id),
    CONSTRAINT chk_timetables_time CHECK (end_time > start_time),
    CONSTRAINT fk_timetables_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_timetables_day ON timetables (day_of_week, start_time);