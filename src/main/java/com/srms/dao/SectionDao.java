package com.srms.dao;

import com.srms.model.Section;
import java.util.List;

/**
 * Data Access Object contract for Section persistence.
 */
public interface SectionDao {
    List<Section> findByCourseId(String courseId);
}
