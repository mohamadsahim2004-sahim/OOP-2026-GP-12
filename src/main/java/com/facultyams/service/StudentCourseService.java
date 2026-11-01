package com.facultyams.service;

import com.facultyams.model.Activity;
import com.facultyams.model.CourseSummary;
import com.facultyams.model.Section;

import java.util.List;

/** The logged-in student's enrolled courses. */
public interface StudentCourseService {
    List<CourseSummary> findCourses(String studentId);
    /** The database has no course sections or activities, so these are empty. */
    List<Section> sections(String courseId);
    List<Activity> activities(String sectionId);
}
