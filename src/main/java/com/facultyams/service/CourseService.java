package com.facultyams.service;

import java.util.List;

import com.facultyams.model.Activity;
import com.facultyams.model.Course;
import com.facultyams.model.Section;

/**
 * Service contract for fetching courses, sections, and activities.
 */
public interface CourseService {
    List<Course> findCourses(String studentId);
    List<Section> sections(String courseId);
    List<Activity> activities(String sectionId);
}
