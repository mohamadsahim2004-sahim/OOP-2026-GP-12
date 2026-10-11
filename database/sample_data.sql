-- SRMS Sample Data matching Abhishek Verma Dashboard

USE faculty_ams_db;

-- 1. Insert User & Student
INSERT INTO users (id, username, password_hash, full_name, email, role, phone_number)
VALUES (1, 'abhishek', 'password123', 'Abhishek Verma', 'abhishek.verma@eng.university.ac.lk', 'STUDENT', '+94 77 123 4567')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

INSERT INTO students (id, reg_no, degree_program, academic_year, current_semester, cgpa, overall_rank, total_students, attendance_percentage, address, academic_advisor, emergency_contact, department)
VALUES (1, 'TG/2021/842', 'B.Tech CSE - 3rd Year', '2021/2022', 'Semester 5', 8.45, 12, 120, 87.00, 'No. 45, University Crescent, Colombo 07', 'Prof. K. R. Jayawardena', '+94 71 987 6543 (Parent)', 'Computer Science & Engineering')
ON DUPLICATE KEY UPDATE cgpa=VALUES(cgpa);

-- 2. Insert Courses
INSERT INTO courses (code, title, credits, department, semester, lecturer_name, course_type, schedule_time) VALUES
('CS301', 'Data Structures', 4, 'CSE', 'Semester 5', 'Dr. A. Fernando', 'Core', 'Mon 08:30 - 10:30'),
('CS302', 'Database Management Systems', 3, 'CSE', 'Semester 5', 'Prof. S. Perera', 'Core', 'Tue 08:30 - 10:30'),
('CS303', 'Operating Systems', 3, 'CSE', 'Semester 5', 'Dr. N. Rajapakse', 'Core', 'Wed 08:30 - 11:30'),
('CS304', 'Computer Networks', 3, 'CSE', 'Semester 5', 'Dr. M. Silva', 'Core', 'Tue 11:00 - 13:00'),
('CS305', 'Software Engineering', 3, 'CSE', 'Semester 5', 'Mrs. T. Wickramasinghe', 'Core', 'Mon 10:45 - 12:45'),
('CS306', 'Web Application Development', 2, 'CSE', 'Semester 5', 'Mr. R. Kumara', 'Elective', 'Mon 13:30 - 15:30'),
('CS307', 'Mobile Application Development', 2, 'CSE', 'Semester 5', 'Dr. D. Wijesinghe', 'Elective', 'Thu 14:00 - 16:00'),
('CS308', 'Artificial Intelligence Fundamentals', 3, 'CSE', 'Semester 5', 'Prof. C. Senaratne', 'Elective', 'Fri 14:00 - 17:00'),
('CS309', 'Cyber Security & Cryptography', 3, 'CSE', 'Semester 5', 'Dr. K. Pathirana', 'Elective', 'Thu 08:30 - 11:30'),
('MG301', 'Entrepreneurship & Innovation', 2, 'Management', 'Semester 5', 'Dr. H. Mendis', 'General', 'Wed 14:00 - 16:00')
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 3. Marks (Matching Image Table)
INSERT INTO marks (student_id, course_code, exam_type, grade, score, max_score, exam_date, semester, credits) VALUES
(1, 'CS301', 'Mid Sem', 'A', 86.00, 100.00, '2024-05-12', 'Semester 5', 4),
(1, 'CS302', 'Mid Sem', 'A+', 91.00, 100.00, '2024-05-10', 'Semester 5', 3),
(1, 'CS303', 'Mid Sem', 'B+', 78.00, 100.00, '2024-05-08', 'Semester 5', 3),
(1, 'CS304', 'Mid Sem', 'A', 85.00, 100.00, '2024-05-06', 'Semester 5', 3),
(1, 'CS305', 'Mid Sem', 'A+', 92.00, 100.00, '2024-05-04', 'Semester 5', 3);

-- 4. Notices (Matching Image List)
INSERT INTO notices (title, content, published_date, category, publisher, is_important) VALUES
('End Semester Exam Schedule', 'The end semester exams will begin from 25th May 2024. All undergraduate students are requested to check their venue allocations.', '2024-05-15', 'Examinations', 'Senior Assistant Registrar', TRUE),
('Result Declaration Notice', 'Results for Mid Semester Exams has been declared. Registered candidates can check provisional mark sheets online.', '2024-05-12', 'Academic', 'Office of the Dean', TRUE),
('Project Submission Deadline', 'Final year project submission is due on 30th May 2024. Late submissions will receive grade deductions.', '2024-05-10', 'Academic', 'Project Coordinator', TRUE);

-- 5. Attendance
INSERT INTO attendance (student_id, course_code, total_sessions, attended_sessions, last_attended_date) VALUES
(1, 'CS301', 32, 28, '2024-05-10'),
(1, 'CS302', 26, 24, '2024-05-09'),
(1, 'CS303', 26, 22, '2024-05-08'),
(1, 'CS304', 26, 23, '2024-05-07'),
(1, 'CS305', 24, 21, '2024-05-06'),
(1, 'CS306', 18, 14, '2024-05-03');

-- 6. Medical Records
INSERT INTO medical_records (ref_no, student_id, course_code, start_date, end_date, reason, doctor_name, hospital_clinic, submission_date, status, admin_remarks) VALUES
('MED-2024-001', 1, 'CS303', '2024-04-10', '2024-04-12', 'Viral Fever & Fatigue', 'Dr. M. K. Bandara', 'Colombo General Teaching Hospital', '2024-04-14', 'Approved', 'Verified by University Medical Officer.'),
('MED-2024-002', 1, 'CS306', '2024-05-02', '2024-05-03', 'Severe Migraine', 'Dr. S. N. Alwis', 'City Care Healthcare Center', '2024-05-04', 'Pending', 'Awaiting review by Senior Student Counselor.');
