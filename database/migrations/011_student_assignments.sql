USE faculty_ams;

SET @has_week_no := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME = 'week_no'
);
SET @sql := IF(@has_week_no = 0,
    'ALTER TABLE course_materials ADD COLUMN week_no TINYINT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_uploaded_at := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME = 'uploaded_at'
);
SET @sql := IF(@has_uploaded_at = 0,
    'ALTER TABLE course_materials ADD COLUMN uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS assignments (
    assignment_id INT          NOT NULL AUTO_INCREMENT,
    course_id     INT          NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NULL,
    due_at        DATETIME     NOT NULL,
    cutoff_at     DATETIME     NULL,
    allow_late    BOOLEAN      NOT NULL DEFAULT FALSE,
    max_marks     DECIMAL(7,2) NULL,
    allowed_types VARCHAR(255) NOT NULL DEFAULT 'pdf,docx,zip,java',
    max_file_mb   SMALLINT     NOT NULL DEFAULT 20,
    max_files     SMALLINT     NOT NULL DEFAULT 5,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id),
    INDEX idx_assignments_course_due (course_id, due_at),
    CONSTRAINT fk_student_assignments_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS submissions (
    submission_id INT          NOT NULL AUTO_INCREMENT,
    assignment_id INT          NOT NULL,
    student_id    INT          NOT NULL,
    comment       TEXT         NULL,
    submitted_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('SUBMITTED', 'LATE', 'GRADED') NOT NULL DEFAULT 'SUBMITTED',
    marks         DECIMAL(7,2) NULL,
    feedback      TEXT         NULL,
    graded_at     DATETIME     NULL,
    PRIMARY KEY (submission_id),
    CONSTRAINT uq_student_assignment_submission UNIQUE (assignment_id, student_id),
    INDEX idx_submissions_student (student_id, submitted_at),
    CONSTRAINT fk_student_submissions_assignment FOREIGN KEY (assignment_id)
        REFERENCES assignments (assignment_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_student_submissions_student FOREIGN KEY (student_id)
        REFERENCES students (user_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

SET @has_graded_at := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'submissions' AND COLUMN_NAME = 'graded_at'
);
SET @sql := IF(@has_graded_at = 0,
    'ALTER TABLE submissions ADD COLUMN graded_at DATETIME NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS submission_files (
    file_id       INT          NOT NULL AUTO_INCREMENT,
    submission_id INT          NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_path   VARCHAR(1024) NOT NULL,
    file_size     BIGINT       NOT NULL,
    PRIMARY KEY (file_id),
    INDEX idx_submission_files_submission (submission_id),
    CONSTRAINT fk_submission_files_submission FOREIGN KEY (submission_id)
        REFERENCES submissions (submission_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('011_student_assignments.sql', 'Student assignments, submissions, and material grouping metadata', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
