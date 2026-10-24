package com.facultyams.dao;

import java.util.List;
import java.util.Optional;

import com.facultyams.model.Course;

/**
 * Data Access Object contract for Course persistence.
 */
public interface CourseDao {
    List<Course> findAll(String studentId);
    Optional<Course> findById(String courseId);
}
