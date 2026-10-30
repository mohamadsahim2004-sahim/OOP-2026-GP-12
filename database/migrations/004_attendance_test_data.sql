USE faculty_ams;

-- 1. Clear  the Technical Officer tables 
DELETE FROM medical;
DELETE FROM session_attendance;
DELETE FROM course_component;
ALTER TABLE medical AUTO_INCREMENT = 1;
ALTER TABLE session_attendance AUTO_INCREMENT = 1;
ALTER TABLE course_component AUTO_INCREMENT = 1;

-- 2. Components of course 1 (ICT2113): component 1 = theory, component 2 = practical
INSERT INTO course_component (course_id, type) VALUES (1, 'THEORY'), (1, 'PRACTICAL');

-- 3. Attendance: 15 sessions for 20 students (ids 11 to 30), theory and practical.
--    u = student, c = component, p = number of sessions attended (sessions 1..p are present, the rest absent)
INSERT INTO session_attendance (user_id, component_id, session_no, date, status)
SELECT x.u, x.c, n.n,
       DATE_SUB(CURDATE(), INTERVAL ((15 - n.n) * 7 + x.c - 1) DAY),
       IF(n.n <= x.p, 'present', 'absent')
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) n
JOIN (SELECT 11 u, 1 c, 15 p UNION ALL SELECT 11, 2, 15
      UNION ALL SELECT 12, 1, 14 UNION ALL SELECT 12, 2, 13
      UNION ALL SELECT 13, 1, 13 UNION ALL SELECT 13, 2, 14
      UNION ALL SELECT 14, 1, 12 UNION ALL SELECT 14, 2, 10
      UNION ALL SELECT 15, 1, 12 UNION ALL SELECT 15, 2, 14
      UNION ALL SELECT 16, 1, 11 UNION ALL SELECT 16, 2, 12
      UNION ALL SELECT 17, 1, 10 UNION ALL SELECT 17, 2, 11
      UNION ALL SELECT 18, 1, 9  UNION ALL SELECT 18, 2, 15
      UNION ALL SELECT 19, 1, 8  UNION ALL SELECT 19, 2, 9
      UNION ALL SELECT 20, 1, 10 UNION ALL SELECT 20, 2, 13
      UNION ALL SELECT 21, 1, 11 UNION ALL SELECT 21, 2, 10
      UNION ALL SELECT 22, 1, 9  UNION ALL SELECT 22, 2, 12
      UNION ALL SELECT 23, 1, 8  UNION ALL SELECT 23, 2, 15
      UNION ALL SELECT 24, 1, 14 UNION ALL SELECT 24, 2, 8
      UNION ALL SELECT 25, 1, 13 UNION ALL SELECT 25, 2, 14
      UNION ALL SELECT 26, 1, 12 UNION ALL SELECT 26, 2, 11
      UNION ALL SELECT 27, 1, 10 UNION ALL SELECT 27, 2, 13
      UNION ALL SELECT 28, 1, 15 UNION ALL SELECT 28, 2, 14
      UNION ALL SELECT 29, 1, 11 UNION ALL SELECT 29, 2, 9
      UNION ALL SELECT 30, 1, 7  UNION ALL SELECT 30, 2, 10) x;

-- 4. Medicals (always on an ABSENT session): approved, pending and rejected
--    u = student, c = component, s = session, reviewer = officer, days = days ago, st = status
INSERT INTO medical (attendance_id, reviewed_by, submitted_date, reason, approval_status)
SELECT a.attendance_id, m.reviewer, DATE_SUB(CURDATE(), INTERVAL m.days DAY), m.reason, m.st
FROM session_attendance a
JOIN (SELECT 16 u, 1 c, 12 s, 7 reviewer, 12 days, 'Fever' reason, 'approved' st
      UNION ALL SELECT 16, 1, 13, 7, 12, 'Fever', 'approved'
      UNION ALL SELECT 17, 1, 11, 7, 20, 'Dengue', 'approved'
      UNION ALL SELECT 17, 1, 12, 7, 20, 'Dengue', 'approved'
      UNION ALL SELECT 17, 1, 13, 7, 20, 'Dengue', 'approved'
      UNION ALL SELECT 18, 1, 10, 8, 30, 'Viral infection', 'approved'
      UNION ALL SELECT 18, 1, 11, 8, 30, 'Viral infection', 'approved'
      UNION ALL SELECT 19, 1, 9, 8, 40, 'Food poisoning', 'approved'
      UNION ALL SELECT 19, 2, 10, 8, 33, 'Food poisoning', 'approved'
      UNION ALL SELECT 24, 2, 9, 7, 45, 'Accident', 'approved'
      UNION ALL SELECT 24, 2, 10, 7, 45, 'Accident', 'approved'
      UNION ALL SELECT 20, 1, 11, 7, 3, 'Fever', 'pending'
      UNION ALL SELECT 30, 2, 12, 7, 2, 'Migraine', 'pending'
      UNION ALL SELECT 21, 1, 12, 8, 5, 'Family event', 'rejected') m
  ON a.user_id = m.u AND a.component_id = m.c AND a.session_no = m.s;