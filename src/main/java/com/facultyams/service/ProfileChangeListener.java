package com.facultyams.service;

import com.facultyams.model.Student;

/** Observer notified when profile details used by shared UI components change. */
@FunctionalInterface
public interface ProfileChangeListener {
    /** Receives the updated student profile. */
    void profileChanged(Student student);
}
