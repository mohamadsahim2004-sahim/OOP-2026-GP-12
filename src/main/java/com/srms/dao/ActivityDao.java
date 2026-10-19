package com.srms.dao;

import com.srms.model.Activity;
import java.util.List;

/**
 * Data Access Object contract for Activity (Announcement, Resource, Assignment) persistence.
 */
public interface ActivityDao {
    List<Activity> findBySectionId(String sectionId);
}
