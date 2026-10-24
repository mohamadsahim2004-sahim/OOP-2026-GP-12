package com.facultyams.dao;

import java.util.List;

import com.facultyams.model.Section;

/**
 * Data Access Object contract for Section persistence.
 */
public interface SectionDao {
    List<Section> findByCourseId(String courseId);
}
