package com.facultyams.controller;

public interface NavigationListener {
    void openCourse(String courseCode);
    void openSubmission(String courseCode, String submissionId);
    void openTimetable();
}
