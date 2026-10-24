USE faculty_ams;

INSERT INTO course_component (course_id, type) VALUES (1, 'THEORY'), (1, 'PRACTICAL');

INSERT INTO session_attendance (user_id, component_id, session_no, date, status)
SELECT x.u, 1, n.n, CURDATE(), IF(n.n <= x.p, 'present', 'absent')
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) n
JOIN (SELECT 11 u, 14 p UNION ALL SELECT 12, 12 UNION ALL SELECT 13, 9
      UNION ALL SELECT 14, 11 UNION ALL SELECT 15, 9) x;

INSERT INTO medical (attendance_id, reviewed_by, submitted_date, reason, approval_status)
SELECT attendance_id, 7, CURDATE(), 'Fever', 'approved'
FROM session_attendance WHERE user_id = 14 AND component_id = 1 AND session_no IN (12, 13);

INSERT INTO medical (attendance_id, reviewed_by, submitted_date, reason, approval_status)
SELECT attendance_id, 7, CURDATE(), 'Fever', 'approved'
FROM session_attendance WHERE user_id = 15 AND component_id = 1 AND session_no IN (10, 11);