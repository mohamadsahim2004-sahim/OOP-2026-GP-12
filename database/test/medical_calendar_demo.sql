-- =============================================================================
-- Fixture for testing the student medical-submission calendar (DEV DATABASE ONLY)
--
-- Run after the demo data in README.md section 5:
--   mysql -u <admin_user> -p faculty_ams -e "source database/test/medical_calendar_demo.sql"
--
-- Makes the existing ICT2113 THEORY session 15 for tg1001 selectable by
-- changing its demo status to absent and moving its date to the most recent
-- weekday. It creates no medical request and only touches this exact demo row.
-- Do not run this against a database containing real student data.
-- =============================================================================

USE faculty_ams;

SET @bad := (
    SELECT COUNT(*) <> 1
    FROM users u
    JOIN students s ON s.user_id = u.user_id
    JOIN enrollments e ON e.student_id = s.user_id AND e.status = 'ENROLLED'
    JOIN courses c ON c.course_code = 'ICT2113'
    JOIN course_component cc ON cc.course_id = c.course_id AND cc.type = 'THEORY'
    JOIN session_attendance sa ON sa.user_id = u.user_id AND sa.component_id = cc.component_id
    LEFT JOIN medical m ON m.attendance_id = sa.attendance_id
    WHERE u.username = 'tg1001'
      AND u.email = 'tg1001@fot.example.lk'
      AND u.role = 'STUDENT'
      AND e.course_id = c.course_id
      AND sa.session_no = 15
      AND sa.status IN ('present', 'absent')
      AND m.medical_id IS NULL
);
SET @s := IF(@bad,
    'SELECT ''ABORT: expected one unsubmitted tg1001 ICT2113 THEORY demo session 15. Run the demo-data scripts first; this fixture changes nothing.'' AS problem',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@bad, 'SELECT * FROM `ABORT__medical_calendar_demo_prerequisite_missing`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @demo_date := DATE_SUB(
    CURDATE(),
    INTERVAL CASE DAYOFWEEK(CURDATE())
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 7 THEN 1
        ELSE 1
    END DAY
);

UPDATE session_attendance sa
JOIN users u ON u.user_id = sa.user_id
JOIN course_component cc ON cc.component_id = sa.component_id
JOIN courses c ON c.course_id = cc.course_id
SET sa.date = @demo_date,
    sa.status = 'absent'
WHERE u.username = 'tg1001'
  AND u.email = 'tg1001@fot.example.lk'
  AND u.role = 'STUDENT'
  AND c.course_code = 'ICT2113'
  AND cc.type = 'THEORY'
  AND sa.session_no = 15
  AND sa.status IN ('present', 'absent')
  AND NOT EXISTS (SELECT 1 FROM medical m WHERE m.attendance_id = sa.attendance_id);

SELECT u.username, c.course_code, cc.type, sa.session_no, sa.date, sa.status
FROM session_attendance sa
JOIN users u ON u.user_id = sa.user_id
JOIN course_component cc ON cc.component_id = sa.component_id
JOIN courses c ON c.course_id = cc.course_id
WHERE u.username = 'tg1001'
  AND u.email = 'tg1001@fot.example.lk'
  AND c.course_code = 'ICT2113'
  AND cc.type = 'THEORY'
  AND sa.session_no = 15
  AND sa.status = 'absent';
