-- ==============================================================================
-- Faculty of Technology Academic Management System
-- University of Ruhuna - Department of Information and Communication Technology
-- Course: ICT2132 - Object Oriented Programming Practicum (Level II - Semester I)
-- Group 12: Shahim, Aamir, Zumra, Mariyam
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS fot_ams_db;
USE fot_ams_db;

-- 1. Base User Table (SRS Section 7.2)
CREATE TABLE IF NOT EXISTS users (
    userID INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL, -- SHA-256 hash (64 hex characters)
    role ENUM('ADMIN', 'LECTURER', 'TECHNICAL_OFFICER', 'UNDERGRADUATE') NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    profilePicture VARCHAR(255) DEFAULT 'default_avatar.png',
    departmentID VARCHAR(20) DEFAULT 'DICT',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Undergraduate Extension Table (SRS Section 7.2)
CREATE TABLE IF NOT EXISTS undergraduates (
    undergraduateID INT PRIMARY KEY,
    indexNumber VARCHAR(20) NOT NULL UNIQUE,
    batch VARCHAR(10) NOT NULL DEFAULT 'B09',
    enrollmentStatus ENUM('Active', 'Repeat', 'Batch-missed') NOT NULL DEFAULT 'Active',
    registrationYear INT NOT NULL DEFAULT 2024,
    FOREIGN KEY (undergraduateID) REFERENCES users(userID) ON DELETE CASCADE
);

-- 3. Lecturer Extension Table (SRS Section 7.2)
CREATE TABLE IF NOT EXISTS lecturers (
    lecturerID INT PRIMARY KEY,
    designation VARCHAR(50) NOT NULL,
    specialisation VARCHAR(100) NOT NULL,
    FOREIGN KEY (lecturerID) REFERENCES users(userID) ON DELETE CASCADE
);

-- 4. Technical Officer Extension Table (SRS Section 7.2)
CREATE TABLE IF NOT EXISTS technical_officers (
    officerID INT PRIMARY KEY,
    departmentID VARCHAR(20) NOT NULL DEFAULT 'DICT',
    FOREIGN KEY (officerID) REFERENCES users(userID) ON DELETE CASCADE
);

-- 5. Courses Table (SRS Section 7.2)
CREATE TABLE IF NOT EXISTS courses (
    courseID INT AUTO_INCREMENT PRIMARY KEY,
    courseCode VARCHAR(20) NOT NULL UNIQUE,
    courseName VARCHAR(100) NOT NULL,
    creditHours INT NOT NULL DEFAULT 2,
    hasTheory BOOLEAN NOT NULL DEFAULT TRUE,
    hasPractical BOOLEAN NOT NULL DEFAULT TRUE,
    lecturerID INT,
    FOREIGN KEY (lecturerID) REFERENCES users(userID) ON DELETE SET NULL
);

-- 6. Attendance Records (SRS Section 7.2, Section 4.1 15 Theory & 15 Practical sessions)
CREATE TABLE IF NOT EXISTS attendance_records (
    recordID INT AUTO_INCREMENT PRIMARY KEY,
    undergraduateID INT NOT NULL,
    courseID INT NOT NULL,
    sessionNumber INT NOT NULL, -- 1 to 15
    sessionType ENUM('THEORY', 'PRACTICAL') NOT NULL,
    sessionDate DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT') NOT NULL,
    FOREIGN KEY (undergraduateID) REFERENCES users(userID) ON DELETE CASCADE,
    FOREIGN KEY (courseID) REFERENCES courses(courseID) ON DELETE CASCADE
);

-- 7. Medical Records (SRS Section 7.2, Section 3.4.3)
CREATE TABLE IF NOT EXISTS medical_records (
    medicalID INT AUTO_INCREMENT PRIMARY KEY,
    undergraduateID INT NOT NULL,
    startDate DATE NOT NULL,
    endDate DATE NOT NULL,
    reason TEXT NOT NULL,
    documentRef VARCHAR(255),
    isApproved BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (undergraduateID) REFERENCES users(userID) ON DELETE CASCADE
);

-- 8. Marks Table (SRS Section 7.2, Section 4.2 - entered out of 100, CA threshold 40%)
CREATE TABLE IF NOT EXISTS marks (
    markID INT AUTO_INCREMENT PRIMARY KEY,
    undergraduateID INT NOT NULL,
    courseID INT NOT NULL,
    examType ENUM('ASSIGNMENT', 'MID_TERM', 'FINAL_THEORY', 'FINAL_PRACTICAL') NOT NULL,
    marksObtained DECIMAL(5, 2) NOT NULL,
    semester INT NOT NULL DEFAULT 1,
    FOREIGN KEY (undergraduateID) REFERENCES users(userID) ON DELETE CASCADE,
    FOREIGN KEY (courseID) REFERENCES courses(courseID) ON DELETE CASCADE
);

-- 9. Notices Table (SRS Section 7.2, Section 3.2.3)
CREATE TABLE IF NOT EXISTS notices (
    noticeID INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    postedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    targetRole ENUM('ALL', 'ADMIN', 'LECTURER', 'TECHNICAL_OFFICER', 'UNDERGRADUATE') DEFAULT 'ALL',
    postedBy INT NOT NULL,
    FOREIGN KEY (postedBy) REFERENCES users(userID) ON DELETE CASCADE
);

-- 10. Timetable Table (SRS Section 7.2, Section 3.2.4)
CREATE TABLE IF NOT EXISTS timetable (
    timetableID INT AUTO_INCREMENT PRIMARY KEY,
    dayOfWeek ENUM('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday') NOT NULL,
    startTime TIME NOT NULL,
    endTime TIME NOT NULL,
    courseID INT NOT NULL,
    venue VARCHAR(50) NOT NULL,
    departmentID VARCHAR(20) DEFAULT 'DICT',
    FOREIGN KEY (courseID) REFERENCES courses(courseID) ON DELETE CASCADE
);

-- ==============================================================================
-- Preloaded Demo Users (All Passwords hashed with SHA-256)
-- admin123 -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- lec123   -> e587399db278ef86b864d4b1eead8cf274cc8203f1ceae85ff771a39fa281bb8
-- to123    -> d9fa165f1ee5eb0175b2cebc4f1a2608447fc3ae6eb7932c02aa00868f02fe04
-- student123 -> cd73502828457d15655bbd7a63fb0bc8f86027ac0d514329e7d164e26107019a
-- ==============================================================================

INSERT INTO users (userID, username, password, role, fullName, email, phone, departmentID) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', 'System Administrator', 'admin@fot.ruh.ac.lk', '+94 41 222 3344', 'DICT'),
(2, 'lec_ict01', 'e587399db278ef86b864d4b1eead8cf274cc8203f1ceae85ff771a39fa281bb8', 'LECTURER', 'Dr. K. L. Perera', 'perera@fot.ruh.ac.lk', '+94 77 123 4567', 'DICT'),
(3, 'to_ict01', 'd9fa165f1ee5eb0175b2cebc4f1a2608447fc3ae6eb7932c02aa00868f02fe04', 'TECHNICAL_OFFICER', 'Mr. S. Fernando', 'fernando.to@fot.ruh.ac.lk', '+94 71 987 6543', 'DICT'),
(4, 'TG/2024/2105', 'cd73502828457d15655bbd7a63fb0bc8f86027ac0d514329e7d164e26107019a', 'UNDERGRADUATE', 'Mohamad Shahim', 'shahim.tg2105@fot.ruh.ac.lk', '+94 76 111 2222', 'DICT'),
(5, 'TG/2024/2104', 'cd73502828457d15655bbd7a63fb0bc8f86027ac0d514329e7d164e26107019a', 'UNDERGRADUATE', 'M. Aamir', 'aamir.tg2104@fot.ruh.ac.lk', '+94 76 222 3333', 'DICT'),
(6, 'TG/2024/2093', 'cd73502828457d15655bbd7a63fb0bc8f86027ac0d514329e7d164e26107019a', 'UNDERGRADUATE', 'F. Zumra', 'zumra.tg2093@fot.ruh.ac.lk', '+94 76 333 4444', 'DICT'),
(7, 'TG/2024/2076', 'cd73502828457d15655bbd7a63fb0bc8f86027ac0d514329e7d164e26107019a', 'UNDERGRADUATE', 'M. Mariyam', 'mariyam.tg2076@fot.ruh.ac.lk', '+94 76 444 5555', 'DICT')
ON DUPLICATE KEY UPDATE fullName = VALUES(fullName);

-- Insert Subclass Profiles
INSERT INTO lecturers (lecturerID, designation, specialisation) VALUES
(2, 'Senior Lecturer (Gr. I)', 'Software Engineering & Object Oriented Systems')
ON DUPLICATE KEY UPDATE designation = VALUES(designation);

INSERT INTO technical_officers (officerID, departmentID) VALUES
(3, 'DICT')
ON DUPLICATE KEY UPDATE departmentID = VALUES(departmentID);

INSERT INTO undergraduates (undergraduateID, indexNumber, batch, enrollmentStatus, registrationYear) VALUES
(4, 'TG/2024/2105', 'B09', 'Active', 2024),
(5, 'TG/2024/2104', 'B09', 'Active', 2024),
(6, 'TG/2024/2093', 'B09', 'Active', 2024),
(7, 'TG/2024/2076', 'B09', 'Active', 2024)
ON DUPLICATE KEY UPDATE enrollmentStatus = VALUES(enrollmentStatus);
