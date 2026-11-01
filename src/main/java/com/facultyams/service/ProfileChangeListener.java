package com.facultyams.service;

import com.facultyams.model.student.StudentProfile;

/** Observer notified when profile details used by shared UI components change. */
@FunctionalInterface
public interface ProfileChangeListener {
    /** Receives the updated student profile. */
    void profileChanged(StudentProfile student);
}
