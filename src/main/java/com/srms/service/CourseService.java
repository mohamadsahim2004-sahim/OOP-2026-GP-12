package com.srms.service;

import com.srms.model.Activity;
import com.srms.model.Course;
import com.srms.model.Section;
import java.util.List;

/**
 * Service contract for fetching courses, sections, and activities.
 */
public interface CourseService {
    List<Course> findCourses(String studentId);
    List<Section> sections(String courseId);
    List<Activity> activities(String sectionId);
}
