-- SRMS - Student Result Management System Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS faculty_ams_db;
USE faculty_ams_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('STUDENT', 'LECTURER', 'ADMIN', 'TECHNICAL_OFFICER') NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Students Table
CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY,
    reg_no VARCHAR(30) UNIQUE NOT NULL,
    degree_program VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    current_semester VARCHAR(20) NOT NULL,
    cgpa DECIMAL(3, 2) DEFAULT 0.00,
    overall_rank INT DEFAULT 0,
    total_students INT DEFAULT 120,
    attendance_percentage DECIMAL(5, 2) DEFAULT 0.00,
    address TEXT,
    academic_advisor VARCHAR(100),
    emergency_contact VARCHAR(100),
    department VARCHAR(100) DEFAULT 'Computer Science & Engineering',
    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Courses Table
CREATE TABLE IF NOT EXISTS courses (
    code VARCHAR(15) PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    credits INT NOT NULL,
    department VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    lecturer_name VARCHAR(100) NOT NULL,
    course_type ENUM('Core', 'Elective', 'General') DEFAULT 'Core',
    schedule_time VARCHAR(100)
);

-- 4. Enrollments Table
CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    enrollment_date DATE DEFAULT (CURRENT_DATE),
    status ENUM('Enrolled', 'Dropped', 'Completed') DEFAULT 'Enrolled',
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_code) REFERENCES courses(code),
    UNIQUE KEY uq_student_course (student_id, course_code)
);

-- 5. Course Materials Table
CREATE TABLE IF NOT EXISTS course_materials (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(15) NOT NULL,
    title VARCHAR(200) NOT NULL,
    material_type VARCHAR(50) NOT NULL,
    file_size VARCHAR(20),
    upload_date DATE DEFAULT (CURRENT_DATE),
    uploaded_by VARCHAR(100),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);

-- 6. Notices Table
CREATE TABLE IF NOT EXISTS notices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    published_date DATE DEFAULT (CURRENT_DATE),
    category VARCHAR(50) NOT NULL,
    publisher VARCHAR(100) NOT NULL,
    is_important BOOLEAN DEFAULT FALSE
);

-- 7. Timetable Table
CREATE TABLE IF NOT EXISTS timetables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day_of_week VARCHAR(15) NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    course_title VARCHAR(150) NOT NULL,
    venue VARCHAR(100) NOT NULL,
    lecturer VARCHAR(100) NOT NULL,
    session_type ENUM('Lecture', 'Practical', 'Tutorial') DEFAULT 'Lecture',
    FOREIGN KEY (course_code) REFERENCES courses(code)
);

-- 8. Marks & Results Table
CREATE TABLE IF NOT EXISTS marks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    exam_type VARCHAR(50) NOT NULL,
    grade VARCHAR(5) NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    max_score DECIMAL(5, 2) DEFAULT 100.00,
    exam_date DATE,
    semester VARCHAR(20) NOT NULL,
    credits INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);

-- 9. Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    total_sessions INT NOT NULL,
    attended_sessions INT NOT NULL,
    last_attended_date DATE,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);

-- 10. Medical Records Table
CREATE TABLE IF NOT EXISTS medical_records (
    ref_no VARCHAR(30) PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    hospital_clinic VARCHAR(150) NOT NULL,
    submission_date DATE DEFAULT (CURRENT_DATE),
    status ENUM('Approved', 'Pending', 'Rejected') DEFAULT 'Pending',
    admin_remarks TEXT,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);
