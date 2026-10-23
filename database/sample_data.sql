-- =============================================================================
-- FoT-AMS sample data (shared foundation: departments, users, courses)
-- Run AFTER schema.sql:  mysql -u root -p faculty_ams < database/sample_data.sql
--
-- Test logins (password is the same for every user of a role):
--   ADMIN              admin                     / admin123
--   LECTURER           lec.perera ... lec.bandara / lecturer123
--   TECHNICAL_OFFICER  to.kumara ... to.wijesinghe / officer123
--   STUDENT            tg1001 ... tg1016, tg0950, tg0951, tg0960, tg0961 / student123
--
-- Fixed IDs are used so other modules can reference them in their own sample data:
--   admin = 1, lecturers = 2-6, technical officers = 7-10, students = 11-30
-- Course list: replace with the real B09 Level 2 Semester 1 timetable modules if they differ.
-- Passwords are PBKDF2 hashes; create new ones with:
--   java -cp target/classes com.facultyams.security.PasswordUtil <password>
-- =============================================================================

USE faculty_ams;

-- -----------------------------------------------------------------------------
-- Departments
-- -----------------------------------------------------------------------------
INSERT INTO departments (department_id, department_code, department_name) VALUES
                                                                              (1, 'DICT', 'Department of Information and Communication Technology'),
                                                                              (2, 'DET',  'Department of Engineering Technology'),
                                                                              (3, 'DBST', 'Department of Biosystems Technology'),
                                                                              (4, 'DMS',  'Department of Multidisciplinary Studies');

-- -----------------------------------------------------------------------------
-- Users: 1 admin, 5 lecturers, 4 technical officers, 20 students
-- -----------------------------------------------------------------------------
INSERT INTO users (user_id, username, password_hash, full_name, email, role, contact_number, profile_picture, department_id, is_active) VALUES
                                                                                                                                            (1, 'admin', 'pbkdf2$65536$w77g4TtIZKt50Ruf7LXL+g==$HuFl6yEf6V+PmGbN+VGr2N6XjheTRT9fUYVGfhpmnpE=', 'System Administrator', 'admin@fot.example.lk', 'ADMIN', '0412222001', NULL, NULL, TRUE),
                                                                                                                                            (2, 'lec.perera', 'pbkdf2$65536$8tuSVxqhvXEfzuMJwgqMww==$U0mYIQNAF4GhGv508OrIkdtX31AKjlCWa5BwnxHG81Q=', 'Dr. Nimal Perera', 'lec.perera@fot.example.lk', 'LECTURER', '0711234001', NULL, 1, TRUE),
                                                                                                                                            (3, 'lec.silva', 'pbkdf2$65536$mkLzeDT3J+CgkdfrPIl8eQ==$EygShxho8fn1Ssrx2KAIDlXMOKUg7CiutAiuAAcYe8g=', 'Ms. Kumari Silva', 'lec.silva@fot.example.lk', 'LECTURER', '0711234002', NULL, 1, TRUE),
                                                                                                                                            (4, 'lec.fernando', 'pbkdf2$65536$s/uOBOamZR1LcAaNL+6eZg==$pLZx9D4v3RA7CDI5kVOfyBoUHEcCDod0BPCOnlGkEDg=', 'Mr. Ruwan Fernando', 'lec.fernando@fot.example.lk', 'LECTURER', '0711234003', NULL, 1, TRUE),
                                                                                                                                            (5, 'lec.jayasuriya', 'pbkdf2$65536$teYAKzBT1t9FtcRJhW1sUw==$SPy8uostRXU1ySWEDXp8Ck131p5BcQewfekYegSIbyM=', 'Dr. Anoma Jayasuriya', 'lec.jayasuriya@fot.example.lk', 'LECTURER', '0711234004', NULL, 2, TRUE),
                                                                                                                                            (6, 'lec.bandara', 'pbkdf2$65536$eutlx2AD6FHtwDM3rvO54A==$YTxhu55HJ31eSWCX9OIT8x33bLNrS/xPxrgXiUYLt04=', 'Mr. Saman Bandara', 'lec.bandara@fot.example.lk', 'LECTURER', '0711234005', NULL, 4, TRUE),
                                                                                                                                            (7, 'to.kumara', 'pbkdf2$65536$hLj4tnO5qpZRcNVFQm9bOA==$xFvC9LQ3aZsI43rr1pwtFYOYfTQTG8FggWiE0319fvA=', 'Mr. Asela Kumara', 'to.kumara@fot.example.lk', 'TECHNICAL_OFFICER', '0722345001', NULL, 1, TRUE),
                                                                                                                                            (8, 'to.dissanayake', 'pbkdf2$65536$OM6Tlkf0fCxRRshu21TZww==$QM8Z15Q7MNWnvW0vd8xvO1jWPH+NI3X4FVYYv9C2b0E=', 'Ms. Chamari Dissanayake', 'to.dissanayake@fot.example.lk', 'TECHNICAL_OFFICER', '0722345002', NULL, 1, TRUE),
                                                                                                                                            (9, 'to.rathnayake', 'pbkdf2$65536$jtGVMLD7AZZ2Iqyldk3kFQ==$/RfBpXuVa2ZAWSpEGImE61kO9fgJLTkiYbsFqKfV6W8=', 'Mr. Pradeep Rathnayake', 'to.rathnayake@fot.example.lk', 'TECHNICAL_OFFICER', '0722345003', NULL, 2, TRUE),
                                                                                                                                            (10, 'to.wijesinghe', 'pbkdf2$65536$+d8D0pSDh1jTXcE8ChbIxQ==$Q983q2aUPx5JTefZvf6orQ6V6ODL1TBax4avgRV/hcA=', 'Ms. Dilini Wijesinghe', 'to.wijesinghe@fot.example.lk', 'TECHNICAL_OFFICER', '0722345004', NULL, 3, TRUE),
                                                                                                                                            (11, 'tg1001', 'pbkdf2$65536$+iCwnJOpjCugwurI63Ficw==$VCzkoldGbCyFKvgFoAoMGlv7CSiLNxKpaJo/LwR9Vbg=', 'Kasun Rajapaksha', 'tg1001@fot.example.lk', 'STUDENT', '0753456001', NULL, 1, TRUE),
                                                                                                                                            (12, 'tg1002', 'pbkdf2$65536$/zFURwBQ4iulRHkeVXa5dA==$hZ33Bt0OFjUDXa3ZFgQQS9bDmSRPG/rZdce55TO/WH8=', 'Nethmi Herath', 'tg1002@fot.example.lk', 'STUDENT', '0753456002', NULL, 1, TRUE),
                                                                                                                                            (13, 'tg1003', 'pbkdf2$65536$w4s7n0P6Bldog9I/yVu1KA==$3Ui1wJw4yuDS//LrtjzOrfOWSmyS6Up1sUFJvGetRho=', 'Isuru Gunasekara', 'tg1003@fot.example.lk', 'STUDENT', '0753456003', NULL, 1, TRUE),
                                                                                                                                            (14, 'tg1004', 'pbkdf2$65536$VeYZRAvrHCSaC9BePFClzw==$M+q7iW40IujGydd6iJMkveeOys8GmEUdGFAssrxUMmI=', 'Sachini Weerasinghe', 'tg1004@fot.example.lk', 'STUDENT', '0753456004', NULL, 1, TRUE),
                                                                                                                                            (15, 'tg1005', 'pbkdf2$65536$QBRF8Yx3wsUmDnNAMdJgPg==$UZJSSo//g7ylNs2UGstu12q4Vf/A97c2WUbgA34RuW8=', 'Tharindu Madushanka', 'tg1005@fot.example.lk', 'STUDENT', '0753456005', NULL, 1, TRUE),
                                                                                                                                            (16, 'tg1006', 'pbkdf2$65536$RC8/+pRoEq+yRg3+dJpWig==$cSBpqAI1eDGmXQdj13hWhTIlCzB7+jbyCmhSSfZcNHs=', 'Hiruni Abeysekara', 'tg1006@fot.example.lk', 'STUDENT', '0753456006', NULL, 1, TRUE),
                                                                                                                                            (17, 'tg1007', 'pbkdf2$65536$o34Vr8afcINDdwwIKXdTrw==$LXYHfd5s/A/4w/3CmUEYUM9AJZgPA9sMvRjr3jWJhbA=', 'Pasindu Senanayake', 'tg1007@fot.example.lk', 'STUDENT', '0753456007', NULL, 1, TRUE),
                                                                                                                                            (18, 'tg1008', 'pbkdf2$65536$+6QIOqcbHlt77wB9bRvCsA==$2YeqQINswnqrQgtUii2dz+uHuR9EzStZlcGC1nHFFG0=', 'Dulani Karunaratne', 'tg1008@fot.example.lk', 'STUDENT', '0753456008', NULL, 1, TRUE),
                                                                                                                                            (19, 'tg1009', 'pbkdf2$65536$C50F+tlGXBJUTvYXnXRc1Q==$rghHAjQzIBtwGcAo16O7AAPr0JkKX5bhXE7Ah8UnIDI=', 'Chamod Jayawardena', 'tg1009@fot.example.lk', 'STUDENT', '0753456009', NULL, 1, TRUE),
                                                                                                                                            (20, 'tg1010', 'pbkdf2$65536$VU7KQRDbC6tip5rnPYb8Aw==$9hGSm/wOUvrllKQjGb3a3QcPMntpaLX7gUfSP0393Cc=', 'Ishara Samarasinghe', 'tg1010@fot.example.lk', 'STUDENT', '0753456010', NULL, 1, TRUE),
                                                                                                                                            (21, 'tg1011', 'pbkdf2$65536$HvDoPK+GvaEfuWVlX3l2CQ==$Np7ob0KimJQpGildo1DrZCQsE5i6STzYQ7fJYRf7hpw=', 'Lahiru Wickramasinghe', 'tg1011@fot.example.lk', 'STUDENT', '0753456011', NULL, 1, TRUE),
                                                                                                                                            (22, 'tg1012', 'pbkdf2$65536$SV0mFvTDSlxE39XoB07U5A==$vs41mmqW0LiJc7WH4C802MGhA08Lo1glD8QfJayGzWk=', 'Nadeesha Ekanayake', 'tg1012@fot.example.lk', 'STUDENT', '0753456012', NULL, 1, TRUE),
                                                                                                                                            (23, 'tg1013', 'pbkdf2$65536$COhWyexGtkp01pxhzy5qYg==$XMvLn6sL5BeRJh0woMZ9BVL9iPEyMvEbrd8a21AwkfI=', 'Ravindu Hettiarachchi', 'tg1013@fot.example.lk', 'STUDENT', '0753456013', NULL, 1, TRUE),
                                                                                                                                            (24, 'tg1014', 'pbkdf2$65536$0L5bUtRcxBJ2lAvpLhiGoQ==$d0JvZ8G7UCYpTVCB5Ly44O8HURcegYM2O3zyHBiPHVg=', 'Sewwandi Liyanage', 'tg1014@fot.example.lk', 'STUDENT', '0753456014', NULL, 1, TRUE),
                                                                                                                                            (25, 'tg1015', 'pbkdf2$65536$eggkrUcIXS6++3TAlOlElg==$fp6UVU9KY5yGo1DHyBZCEygM8IyM3hhtdkZodx6vgHI=', 'Yasiru Amarasinghe', 'tg1015@fot.example.lk', 'STUDENT', '0753456015', NULL, 1, TRUE),
                                                                                                                                            (26, 'tg1016', 'pbkdf2$65536$XhDCOMRG3w+0UeFDli0FGA==$psnVYfReg5bS6JpH5FWv0pzgPnaLSPbr5ejwKwiYkJs=', 'Thilini Kodikara', 'tg1016@fot.example.lk', 'STUDENT', '0753456016', NULL, 1, TRUE),
                                                                                                                                            (27, 'tg0950', 'pbkdf2$65536$kBdxWfj9X5cmhVeUJDiEMw==$hrWkTi/cBc3aDKwryBQWhUFVXvUiaH8qrfRi6gwMhRc=', 'Malith Gamage', 'tg0950@fot.example.lk', 'STUDENT', '0753456017', NULL, 1, TRUE),
                                                                                                                                            (28, 'tg0951', 'pbkdf2$65536$qcNEmR2dXDhvhwy+XPEsLw==$yy/XBsUz904JbkWDdTqUqnBqK94a89IoVczp/hkMkEk=', 'Oshadi Ranasinghe', 'tg0951@fot.example.lk', 'STUDENT', '0753456018', NULL, 1, TRUE),
                                                                                                                                            (29, 'tg0960', 'pbkdf2$65536$0v1dfZ83PJU8QmiOESF2Mw==$AKlXiYTLWtfNPZeURuFY66BlOAcL04keI+yv5poObfA=', 'Dinuka Pathirana', 'tg0960@fot.example.lk', 'STUDENT', '0753456019', NULL, 1, TRUE),
                                                                                                                                            (30, 'tg0961', 'pbkdf2$65536$HEBpRwEg45mre7VPOz10DA==$YGUIRpPpxqQXU+yBNNuQe2g5nVAkp2qu9S+S+VshcW0=', 'Hansani Munasinghe', 'tg0961@fot.example.lk', 'STUDENT', '0753456020', NULL, 1, TRUE);

INSERT INTO lecturers (user_id, designation) VALUES
                                                 (2, 'Senior Lecturer'),
                                                 (3, 'Lecturer'),
                                                 (4, 'Lecturer'),
                                                 (5, 'Senior Lecturer'),
                                                 (6, 'Lecturer (Probationary)');

-- REGULAR = batch B09, REPEAT and BATCH_MISSED = students from batch B08 following B09 modules
INSERT INTO students (user_id, registration_no, batch, student_status) VALUES
                                                                           (11, 'TG/2023/1001', 'B09', 'REGULAR'),
                                                                           (12, 'TG/2023/1002', 'B09', 'REGULAR'),
                                                                           (13, 'TG/2023/1003', 'B09', 'REGULAR'),
                                                                           (14, 'TG/2023/1004', 'B09', 'REGULAR'),
                                                                           (15, 'TG/2023/1005', 'B09', 'REGULAR'),
                                                                           (16, 'TG/2023/1006', 'B09', 'REGULAR'),
                                                                           (17, 'TG/2023/1007', 'B09', 'REGULAR'),
                                                                           (18, 'TG/2023/1008', 'B09', 'REGULAR'),
                                                                           (19, 'TG/2023/1009', 'B09', 'REGULAR'),
                                                                           (20, 'TG/2023/1010', 'B09', 'REGULAR'),
                                                                           (21, 'TG/2023/1011', 'B09', 'REGULAR'),
                                                                           (22, 'TG/2023/1012', 'B09', 'REGULAR'),
                                                                           (23, 'TG/2023/1013', 'B09', 'REGULAR'),
                                                                           (24, 'TG/2023/1014', 'B09', 'REGULAR'),
                                                                           (25, 'TG/2023/1015', 'B09', 'REGULAR'),
                                                                           (26, 'TG/2023/1016', 'B09', 'REGULAR'),
                                                                           (27, 'TG/2022/0950', 'B08', 'REPEAT'),
                                                                           (28, 'TG/2022/0951', 'B08', 'REPEAT'),
                                                                           (29, 'TG/2022/0960', 'B08', 'BATCH_MISSED'),
                                                                           (30, 'TG/2022/0961', 'B08', 'BATCH_MISSED');

-- -----------------------------------------------------------------------------
-- Courses (Level 2 Semester 1 ICT modules + examples from other departments)
-- Last digit of the code = credits.
-- -----------------------------------------------------------------------------
INSERT INTO courses (course_id, course_code, course_name, credits, academic_level, semester, course_type, department_id, lecturer_id) VALUES
                                                                                                                                          (1, 'ICT2113', 'Data Structures and Algorithms',            3, 2, 1, 'THEORY_AND_PRACTICAL', 1, 2),
                                                                                                                                          (2, 'ICT2122', 'Object Oriented Programming',               2, 2, 1, 'THEORY',               1, 3),
                                                                                                                                          (3, 'ICT2132', 'Object Oriented Programming Practicum',     2, 2, 1, 'PRACTICAL',            1, 3),
                                                                                                                                          (4, 'ICT2142', 'Object Oriented Analysis and Design',       2, 2, 1, 'THEORY_AND_PRACTICAL', 1, 4),
                                                                                                                                          (5, 'ICT2152', 'E-Commerce Implementation and Management',  2, 2, 1, 'THEORY',               1, 2),
                                                                                                                                          (6, 'ENG2122', 'English III',                               2, 2, 1, 'THEORY',               4, 6),
                                                                                                                                          (7, 'ETE2113', 'Electrical Circuit Analysis',               3, 2, 1, 'THEORY_AND_PRACTICAL', 2, 5),
                                                                                                                                          (8, 'BST2112', 'Plant Biotechnology',                       2, 2, 1, 'THEORY',               3, NULL);