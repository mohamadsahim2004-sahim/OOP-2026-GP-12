CREATE DATABASE IF NOT EXISTS fot_system;
USE fot_system;


-- testTables
CREATE TABLE USER (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL
);

CREATE TABLE COURSE_COMPONENT (
    component_id INT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(10) NOT NULL
);

CREATE TABLE TECHNICALOFFICER (
    user_id INT PRIMARY KEY,
    department VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES USER(user_id)
);

-- myTabels
CREATE TABLE ATTENDANCE (
    attendance_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    component_id INT NOT NULL,
    session_no INT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES USER(user_id),
    FOREIGN KEY (component_id) REFERENCES COURSE_COMPONENT(component_id),
    UNIQUE (user_id, component_id, session_no)
);

CREATE TABLE MEDICAL (
    medical_id INT PRIMARY KEY AUTO_INCREMENT,
    attendance_id INT NOT NULL UNIQUE,
    reviewed_by INT NOT NULL,
    submitted_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    approval_status VARCHAR(10) DEFAULT 'pending',
    FOREIGN KEY (attendance_id) REFERENCES ATTENDANCE(attendance_id),
    FOREIGN KEY (reviewed_by) REFERENCES TECHNICALOFFICER(user_id)
);