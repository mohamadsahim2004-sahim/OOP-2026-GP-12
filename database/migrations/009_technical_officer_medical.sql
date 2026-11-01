-- =============================================================================
-- 009: Technical Officer module - medical evidence, decision audit, 'late' status
-- Run ONCE, after 003 (and optionally 004):
--   mysql -u root -p faculty_ams < database/migrations/009_technical_officer_medical.sql
-- Existing rows are preserved:
--   * pending medicals had the submitting officer stored in reviewed_by; that value moves to
--     submitted_by and reviewed_by becomes NULL until a decision is made
--   * already approved/rejected medicals keep their reviewer; decided_at is back-filled
-- =============================================================================
USE faculty_ams;

ALTER TABLE medical
    MODIFY reviewed_by      INT          NULL,
    ADD COLUMN submitted_by     INT          NULL        ,
    ADD COLUMN evidence_name    VARCHAR(255) NULL        ,
    ADD COLUMN evidence_file    VARCHAR(100) NULL        ,
    ADD COLUMN rejection_reason VARCHAR(255) NULL        ,
    ADD COLUMN decided_at       DATETIME     NULL        ,
    ADD COLUMN created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_medical_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (user_id),
    ADD CONSTRAINT chk_medical_status CHECK (approval_status IN ('pending', 'approved', 'rejected'));

UPDATE medical SET submitted_by = reviewed_by WHERE submitted_by IS NULL;
UPDATE medical SET reviewed_by = NULL WHERE approval_status = 'pending';
UPDATE medical SET decided_at = CURRENT_TIMESTAMP WHERE approval_status <> 'pending' AND decided_at IS NULL;

-- attendance status values are now: present, absent, late (column is VARCHAR(10), no change needed)
CREATE INDEX idx_session_attendance_date ON session_attendance (date);
CREATE INDEX idx_medical_status ON medical (approval_status);