package com.facultyams.dao;

import java.util.List;

import com.facultyams.model.Activity;

/**
 * Data Access Object contract for Activity (Announcement, Resource, Assignment) persistence.
 */
public interface ActivityDao {
    List<Activity> findBySectionId(String sectionId);
}
