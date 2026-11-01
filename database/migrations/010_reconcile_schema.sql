-- =============================================================================
-- Migration 010 - Reconcile schema (final keys, foreign keys and range checks)
--
-- Final step of the schema upgrade. Verifies that every table and column the application's
-- DAOs use exists (STOPS with an error naming the table if not, so run 003, 005, 007, 009 first), then adds -
-- only where missing and only where existing data allows it - the keys and constraints of the
-- authoritative schema (database/schema.sql):
--   * unique keys: course_component(course_id, type), session_attendance(user_id, component_id, session_no),
--                  medical(attendance_id), marks(student_id, course_id), evaluation_component(course_id, component_name)
--   * foreign keys for tables that were created outside these scripts
--   * CHECKs: attendance status / session number 1-15, marks 0-100, grade points 0-4
--
-- Nothing is deleted or rewritten. When existing rows would violate a key or constraint, that one
-- statement is SKIPPED and a WARNING row is printed; fix the data (database/audit_existing_database.sql
-- lists it) and run this script again.
--
--   mysql -u <admin_user> -p faculty_ams -e "source database/migrations/010_reconcile_schema.sql"

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

SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table departments does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__departments_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table users does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lecturers') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table lecturers does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__lecturers_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table students does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__students_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table courses does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__courses_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table notices does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notices_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice_reads') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table notice_reads does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notice_reads_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'timetables') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table timetables does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__timetables_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_component does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_component_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table session_attendance does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__session_attendance_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table evaluation_component does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__evaluation_component_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table component_marks does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__component_marks_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials') = 0);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_materials does not exist. Run the migrations in the order given in README.md.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_materials_table_missing__run_earlier_migrations`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME IN ('department_id', 'department_code', 'department_name', 'created_at', 'head_user_id')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table departments lacks one or more of its columns (department_id, department_code, department_name, created_at, head_user_id). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__departments_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME IN ('user_id', 'username', 'password_hash', 'full_name', 'email', 'role', 'contact_number', 'profile_picture', 'department_id', 'is_active', 'created_at', 'updated_at')) < 12);
SET @s := IF(@bad, 'SELECT ''ABORT: table users lacks one or more of its columns (user_id, username, password_hash, full_name, email, role, contact_number, profile_picture, department_id, is_active, created_at, updated_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__users_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lecturers' AND COLUMN_NAME IN ('user_id', 'designation')) < 2);
SET @s := IF(@bad, 'SELECT ''ABORT: table lecturers lacks one or more of its columns (user_id, designation). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__lecturers_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students' AND COLUMN_NAME IN ('user_id', 'registration_no', 'batch', 'student_status')) < 4);
SET @s := IF(@bad, 'SELECT ''ABORT: table students lacks one or more of its columns (user_id, registration_no, batch, student_status). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__students_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME IN ('course_id', 'course_code', 'course_name', 'credits', 'academic_level', 'semester', 'course_type', 'department_id', 'lecturer_id', 'created_at')) < 10);
SET @s := IF(@bad, 'SELECT ''ABORT: table courses lacks one or more of its columns (course_id, course_code, course_name, credits, academic_level, semester, course_type, department_id, lecturer_id, created_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__courses_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notices' AND COLUMN_NAME IN ('notice_id', 'title', 'content', 'notice_date', 'created_by', 'created_at', 'category', 'priority')) < 8);
SET @s := IF(@bad, 'SELECT ''ABORT: table notices lacks one or more of its columns (notice_id, title, content, notice_date, created_by, created_at, category, priority). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notices_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice_reads' AND COLUMN_NAME IN ('user_id', 'notice_id', 'read_at')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table notice_reads lacks one or more of its columns (user_id, notice_id, read_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__notice_reads_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'timetables' AND COLUMN_NAME IN ('timetable_id', 'course_id', 'day_of_week', 'start_time', 'end_time', 'venue', 'created_at')) < 7);
SET @s := IF(@bad, 'SELECT ''ABORT: table timetables lacks one or more of its columns (timetable_id, course_id, day_of_week, start_time, end_time, venue, created_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__timetables_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME IN ('student_id', 'course_id', 'enrolled_at', 'status', 'dropped_at')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table enrollments lacks one or more of its columns (student_id, course_id, enrolled_at, status, dropped_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__enrollments_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component' AND COLUMN_NAME IN ('component_id', 'course_id', 'type')) < 3);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_component lacks one or more of its columns (component_id, course_id, type). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_component_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND COLUMN_NAME IN ('attendance_id', 'user_id', 'component_id', 'session_no', 'date', 'status')) < 6);
SET @s := IF(@bad, 'SELECT ''ABORT: table session_attendance lacks one or more of its columns (attendance_id, user_id, component_id, session_no, date, status). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__session_attendance_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME IN ('medical_id', 'attendance_id', 'reviewed_by', 'submitted_date', 'reason', 'approval_status', 'request_ref', 'doctor', 'certificate_no', 'notes', 'certificate_files', 'reviewer_remarks', 'submitted_by', 'evidence_name', 'evidence_file', 'rejection_reason', 'decided_at', 'created_at')) < 18);
SET @s := IF(@bad, 'SELECT ''ABORT: table medical lacks one or more of its columns (medical_id, attendance_id, reviewed_by, submitted_date, reason, approval_status, request_ref, doctor, certificate_no, notes, certificate_files, reviewer_remarks, submitted_by, evidence_name, evidence_file, rejection_reason, decided_at, created_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component' AND COLUMN_NAME IN ('eval_component_id', 'course_id', 'component_name', 'category', 'max_marks', 'weightage', 'created_at')) < 7);
SET @s := IF(@bad, 'SELECT ''ABORT: table evaluation_component lacks one or more of its columns (eval_component_id, course_id, component_name, category, max_marks, weightage, created_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__evaluation_component_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks' AND COLUMN_NAME IN ('student_id', 'eval_component_id', 'marks_obtained', 'entered_by', 'updated_at')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table component_marks lacks one or more of its columns (student_id, eval_component_id, marks_obtained, entered_by, updated_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__component_marks_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME IN ('marksid', 'student_id', 'course_id', 'obtainMarks', 'finalMarks', 'grade', 'isEligible', 'overall_mark', 'grade_points', 'published', 'published_at')) < 11);
SET @s := IF(@bad, 'SELECT ''ABORT: table marks lacks one or more of its columns (marksid, student_id, course_id, obtainMarks, finalMarks, grade, isEligible, overall_mark, grade_points, published, published_at). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__marks_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @bad := ((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME IN ('materialID', 'courseID', 'description', 'notes', 'referenceText')) < 5);
SET @s := IF(@bad, 'SELECT ''ABORT: table course_materials lacks one or more of its columns (materialID, courseID, description, notes, referenceText). Run migrations 003, 005, 007 and 009 first.'' AS problem', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__course_materials_missing_columns__run_003_005_007_009`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- unique keys
-- unique key course_component(course_id, type)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 2 AND SUM(COLUMN_NAME IN ('course_id', 'type')) = 2) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `course_component` GROUP BY `course_id`, `type` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_course_component on course_component(course_id, type) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `course_component` ADD CONSTRAINT `uq_course_component` UNIQUE (`course_id`, `type`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- unique key session_attendance(user_id, component_id, session_no)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 3 AND SUM(COLUMN_NAME IN ('user_id', 'component_id', 'session_no')) = 3) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `session_attendance` GROUP BY `user_id`, `component_id`, `session_no` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_session_attendance on session_attendance(user_id, component_id, session_no) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `session_attendance` ADD CONSTRAINT `uq_session_attendance` UNIQUE (`user_id`, `component_id`, `session_no`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- unique key medical(attendance_id)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 1 AND SUM(COLUMN_NAME IN ('attendance_id')) = 1) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `medical` GROUP BY `attendance_id` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_medical_attendance on medical(attendance_id) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `uq_medical_attendance` UNIQUE (`attendance_id`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- unique key marks(student_id, course_id)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 2 AND SUM(COLUMN_NAME IN ('student_id', 'course_id')) = 2) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `marks` GROUP BY `student_id`, `course_id` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_marks_student_course on marks(student_id, course_id) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `uq_marks_student_course` UNIQUE (`student_id`, `course_id`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- unique key evaluation_component(course_id, component_name)
SET @has := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 2 AND SUM(COLUMN_NAME IN ('course_id', 'component_name')) = 2) ux);
SET @dups := (SELECT COUNT(*) FROM (SELECT 1 FROM `evaluation_component` GROUP BY `course_id`, `component_name` HAVING COUNT(*) > 1) dp);
SET @s := IF(@has > 0, 'DO 0', IF(@dups > 0, 'SELECT ''WARNING: unique key uq_eval_component_name on evaluation_component(course_id, component_name) NOT added because duplicate rows exist. Resolve them (README, Recovery), then run this script again.'' AS warning', 'ALTER TABLE `evaluation_component` ADD CONSTRAINT `uq_eval_component_name` UNIQUE (`course_id`, `component_name`)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- foreign keys (only added when missing and no orphan row exists)
-- foreign key course_component.course_id -> courses.course_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_component' AND COLUMN_NAME = 'course_id' AND REFERENCED_TABLE_NAME = 'courses');
SET @orph := (SELECT COUNT(*) FROM `course_component` x WHERE x.`course_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `courses` r WHERE r.`course_id` = x.`course_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_course_component_course (course_component.course_id -> courses.course_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `course_component` ADD CONSTRAINT `fk_course_component_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key session_attendance.user_id -> students.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'students');
SET @orph := (SELECT COUNT(*) FROM `session_attendance` x WHERE x.`user_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `students` r WHERE r.`user_id` = x.`user_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_session_attendance_student (session_attendance.user_id -> students.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `session_attendance` ADD CONSTRAINT `fk_session_attendance_student` FOREIGN KEY (`user_id`) REFERENCES `students` (`user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key session_attendance.component_id -> course_component.component_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND COLUMN_NAME = 'component_id' AND REFERENCED_TABLE_NAME = 'course_component');
SET @orph := (SELECT COUNT(*) FROM `session_attendance` x WHERE x.`component_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `course_component` r WHERE r.`component_id` = x.`component_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_session_attendance_component (session_attendance.component_id -> course_component.component_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `session_attendance` ADD CONSTRAINT `fk_session_attendance_component` FOREIGN KEY (`component_id`) REFERENCES `course_component` (`component_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key medical.attendance_id -> session_attendance.attendance_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'attendance_id' AND REFERENCED_TABLE_NAME = 'session_attendance');
SET @orph := (SELECT COUNT(*) FROM `medical` x WHERE x.`attendance_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `session_attendance` r WHERE r.`attendance_id` = x.`attendance_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_medical_attendance (medical.attendance_id -> session_attendance.attendance_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `fk_medical_attendance` FOREIGN KEY (`attendance_id`) REFERENCES `session_attendance` (`attendance_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key medical.reviewed_by -> users.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'reviewed_by' AND REFERENCED_TABLE_NAME = 'users');
SET @orph := (SELECT COUNT(*) FROM `medical` x WHERE x.`reviewed_by` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `users` r WHERE r.`user_id` = x.`reviewed_by`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_medical_reviewer (medical.reviewed_by -> users.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `fk_medical_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key medical.submitted_by -> users.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND COLUMN_NAME = 'submitted_by' AND REFERENCED_TABLE_NAME = 'users');
SET @orph := (SELECT COUNT(*) FROM `medical` x WHERE x.`submitted_by` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `users` r WHERE r.`user_id` = x.`submitted_by`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_medical_submitted_by (medical.submitted_by -> users.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `fk_medical_submitted_by` FOREIGN KEY (`submitted_by`) REFERENCES `users` (`user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key enrollments.student_id -> students.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'student_id' AND REFERENCED_TABLE_NAME = 'students');
SET @orph := (SELECT COUNT(*) FROM `enrollments` x WHERE x.`student_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `students` r WHERE r.`user_id` = x.`student_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_enrollments_student (enrollments.student_id -> students.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `enrollments` ADD CONSTRAINT `fk_lm_enrollments_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key enrollments.course_id -> courses.course_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'course_id' AND REFERENCED_TABLE_NAME = 'courses');
SET @orph := (SELECT COUNT(*) FROM `enrollments` x WHERE x.`course_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `courses` r WHERE r.`course_id` = x.`course_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_enrollments_course (enrollments.course_id -> courses.course_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `enrollments` ADD CONSTRAINT `fk_lm_enrollments_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key evaluation_component.course_id -> courses.course_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_component' AND COLUMN_NAME = 'course_id' AND REFERENCED_TABLE_NAME = 'courses');
SET @orph := (SELECT COUNT(*) FROM `evaluation_component` x WHERE x.`course_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `courses` r WHERE r.`course_id` = x.`course_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_eval_component_course (evaluation_component.course_id -> courses.course_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `evaluation_component` ADD CONSTRAINT `fk_lm_eval_component_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key component_marks.student_id -> students.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks' AND COLUMN_NAME = 'student_id' AND REFERENCED_TABLE_NAME = 'students');
SET @orph := (SELECT COUNT(*) FROM `component_marks` x WHERE x.`student_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `students` r WHERE r.`user_id` = x.`student_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_component_marks_student (component_marks.student_id -> students.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `component_marks` ADD CONSTRAINT `fk_lm_component_marks_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key component_marks.eval_component_id -> evaluation_component.eval_component_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks' AND COLUMN_NAME = 'eval_component_id' AND REFERENCED_TABLE_NAME = 'evaluation_component');
SET @orph := (SELECT COUNT(*) FROM `component_marks` x WHERE x.`eval_component_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `evaluation_component` r WHERE r.`eval_component_id` = x.`eval_component_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_component_marks_component (component_marks.eval_component_id -> evaluation_component.eval_component_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `component_marks` ADD CONSTRAINT `fk_lm_component_marks_component` FOREIGN KEY (`eval_component_id`) REFERENCES `evaluation_component` (`eval_component_id`) ON UPDATE CASCADE ON DELETE RESTRICT'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key component_marks.entered_by -> users.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'component_marks' AND COLUMN_NAME = 'entered_by' AND REFERENCED_TABLE_NAME = 'users');
SET @orph := (SELECT COUNT(*) FROM `component_marks` x WHERE x.`entered_by` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `users` r WHERE r.`user_id` = x.`entered_by`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_component_marks_user (component_marks.entered_by -> users.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `component_marks` ADD CONSTRAINT `fk_lm_component_marks_user` FOREIGN KEY (`entered_by`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE ON DELETE SET NULL'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key marks.student_id -> students.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'student_id' AND REFERENCED_TABLE_NAME = 'students');
SET @orph := (SELECT COUNT(*) FROM `marks` x WHERE x.`student_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `students` r WHERE r.`user_id` = x.`student_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_marks_student (marks.student_id -> students.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `fk_lm_marks_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key marks.course_id -> courses.course_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND COLUMN_NAME = 'course_id' AND REFERENCED_TABLE_NAME = 'courses');
SET @orph := (SELECT COUNT(*) FROM `marks` x WHERE x.`course_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `courses` r WHERE r.`course_id` = x.`course_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_marks_course (marks.course_id -> courses.course_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `fk_lm_marks_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key course_materials.courseID -> courses.course_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_materials' AND COLUMN_NAME = 'courseID' AND REFERENCED_TABLE_NAME = 'courses');
SET @orph := (SELECT COUNT(*) FROM `course_materials` x WHERE x.`courseID` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `courses` r WHERE r.`course_id` = x.`courseID`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_lm_course_materials_course (course_materials.courseID -> courses.course_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `course_materials` ADD CONSTRAINT `fk_lm_course_materials_course` FOREIGN KEY (`courseID`) REFERENCES `courses` (`course_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key notice_reads.user_id -> users.user_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice_reads' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'users');
SET @orph := (SELECT COUNT(*) FROM `notice_reads` x WHERE x.`user_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `users` r WHERE r.`user_id` = x.`user_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_notice_reads_user (notice_reads.user_id -> users.user_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `notice_reads` ADD CONSTRAINT `fk_notice_reads_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- foreign key notice_reads.notice_id -> notices.notice_id
SET @has := (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice_reads' AND COLUMN_NAME = 'notice_id' AND REFERENCED_TABLE_NAME = 'notices');
SET @orph := (SELECT COUNT(*) FROM `notice_reads` x WHERE x.`notice_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `notices` r WHERE r.`notice_id` = x.`notice_id`));
SET @s := IF(@has > 0, 'DO 0', IF(@orph > 0, 'SELECT ''WARNING: foreign key fk_notice_reads_notice (notice_reads.notice_id -> notices.notice_id) NOT added because orphan rows exist. Run database/audit_existing_database.sql to list them, fix them, then run this script again.'' AS warning', 'ALTER TABLE `notice_reads` ADD CONSTRAINT `fk_notice_reads_notice` FOREIGN KEY (`notice_id`) REFERENCES `notices` (`notice_id`) ON UPDATE CASCADE ON DELETE CASCADE'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- range / value checks (only added when every existing row already satisfies them)
-- check session_attendance.chk_attendance_status
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND CONSTRAINT_NAME = 'chk_attendance_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `session_attendance` WHERE NOT (status IN ('present', 'absent', 'late')));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_attendance_status on session_attendance NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `session_attendance` ADD CONSTRAINT `chk_attendance_status` CHECK (status IN (''present'', ''absent'', ''late''))'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check session_attendance.chk_attendance_session_no
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND CONSTRAINT_NAME = 'chk_attendance_session_no' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `session_attendance` WHERE NOT (session_no BETWEEN 1 AND 15));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_attendance_session_no on session_attendance NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `session_attendance` ADD CONSTRAINT `chk_attendance_session_no` CHECK (session_no BETWEEN 1 AND 15)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check marks.chk_marks_ca_range
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND CONSTRAINT_NAME = 'chk_marks_ca_range' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `marks` WHERE NOT (obtainMarks BETWEEN 0 AND 100));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_marks_ca_range on marks NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `chk_marks_ca_range` CHECK (obtainMarks BETWEEN 0 AND 100)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check marks.chk_marks_final_range
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND CONSTRAINT_NAME = 'chk_marks_final_range' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `marks` WHERE NOT (finalMarks BETWEEN 0 AND 100));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_marks_final_range on marks NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `chk_marks_final_range` CHECK (finalMarks BETWEEN 0 AND 100)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check marks.chk_marks_overall_range
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND CONSTRAINT_NAME = 'chk_marks_overall_range' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `marks` WHERE NOT (overall_mark IS NULL OR overall_mark BETWEEN 0 AND 100));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_marks_overall_range on marks NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `chk_marks_overall_range` CHECK (overall_mark IS NULL OR overall_mark BETWEEN 0 AND 100)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check marks.chk_marks_points_range
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'marks' AND CONSTRAINT_NAME = 'chk_marks_points_range' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `marks` WHERE NOT (grade_points IS NULL OR grade_points BETWEEN 0 AND 4));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_marks_points_range on marks NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `marks` ADD CONSTRAINT `chk_marks_points_range` CHECK (grade_points IS NULL OR grade_points BETWEEN 0 AND 4)'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- check medical.chk_medical_status
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND CONSTRAINT_NAME = 'chk_medical_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @viol := (SELECT COUNT(*) FROM `medical` WHERE NOT (approval_status IN ('pending', 'approved', 'rejected')));
SET @s := IF(@has > 0, 'DO 0', IF(@viol > 0, 'SELECT ''WARNING: CHECK chk_medical_status on medical NOT added because existing rows violate it. Run database/verify_assignment_data.sql / audit_existing_database.sql to list them, correct them, then run this script again.'' AS warning', 'ALTER TABLE `medical` ADD CONSTRAINT `chk_medical_status` CHECK (approval_status IN (''pending'', ''approved'', ''rejected''))'));
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- indexes
-- index session_attendance.idx_session_attendance_date
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'session_attendance' AND INDEX_NAME = 'idx_session_attendance_date') = 0, 'ALTER TABLE `session_attendance` ADD INDEX `idx_session_attendance_date` (`date`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- index medical.idx_medical_status
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND INDEX_NAME = 'idx_medical_status') = 0, 'ALTER TABLE `medical` ADD INDEX `idx_medical_status` (`approval_status`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- index medical.idx_medical_request_ref
SET @s := IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND INDEX_NAME = 'idx_medical_request_ref') = 0, 'ALTER TABLE `medical` ADD INDEX `idx_medical_request_ref` (`request_ref`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT INTO schema_migrations (migration_id, description, applied_by)
VALUES ('010_reconcile_schema', 'final keys, foreign keys and range checks', CURRENT_USER())
ON DUPLICATE KEY UPDATE last_run_at = CURRENT_TIMESTAMP, run_count = run_count + 1;
SELECT 'migration 010_reconcile_schema finished - see schema_migrations' AS result;