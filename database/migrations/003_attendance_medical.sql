USE faculty_ams;

CREATE TABLE course_component (
    component_id INT NOT NULL AUTO_INCREMENT,
    course_id    INT NOT NULL,
    type         ENUM('THEORY', 'PRACTICAL') NOT NULL,
    PRIMARY KEY (component_id),
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
);

CREATE TABLE attendance (
    attendance_id INT NOT NULL AUTO_INCREMENT,
    user_id       INT NOT NULL,
    component_id  INT NOT NULL,
    session_no    INT NOT NULL,
    date          DATE NOT NULL,
    status        VARCHAR(10) NOT NULL,
    PRIMARY KEY (attendance_id),
    FOREIGN KEY (user_id) REFERENCES students (user_id),
    FOREIGN KEY (component_id) REFERENCES course_component (component_id),
    UNIQUE (user_id, component_id, session_no)
);

CREATE TABLE medical (
    medical_id      INT NOT NULL AUTO_INCREMENT,
    attendance_id   INT NOT NULL UNIQUE,
    reviewed_by     INT NOT NULL,
    submitted_date  DATE NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    approval_status VARCHAR(10) NOT NULL DEFAULT 'pending',
    PRIMARY KEY (medical_id),
    FOREIGN KEY (attendance_id) REFERENCES attendance (attendance_id),
    FOREIGN KEY (reviewed_by) REFERENCES users (user_id)
);