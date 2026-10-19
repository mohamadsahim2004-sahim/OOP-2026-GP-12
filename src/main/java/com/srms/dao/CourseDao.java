package com.srms.dao;

import com.srms.model.Course;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for Course persistence.
 */
public interface CourseDao {
    List<Course> findAll(String studentId);
    Optional<Course> findById(String courseId);
}
