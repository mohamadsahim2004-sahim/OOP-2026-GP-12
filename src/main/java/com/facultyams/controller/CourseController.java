package com.facultyams.controller;

import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingWorker;

import com.facultyams.model.Assignment;
import com.facultyams.model.CourseSummary;
import com.facultyams.service.StudentCourseService;
import com.facultyams.service.SubmissionService;
import com.facultyams.ui.views.AssignmentView;
import com.facultyams.ui.views.CourseOverviewView;
import com.facultyams.ui.views.CoursePageView;

/**
 * Controller orchestrating course navigation, async data loading, and view state transitions.
 */
public class CourseController {
    private final StudentCourseService courseService;
    private final SubmissionService submissionService;
    private final Navigator navigator;
    private final String studentId;

    private CourseOverviewView overviewView;
    private CoursePageView pageView;
    private AssignmentView assignmentView;
    private JComponent enrollmentLegacyView;
    private CourseSummary currentCourse;

    public CourseController(StudentCourseService courseService, SubmissionService submissionService,
                            Navigator navigator, String studentId) {
        this.courseService = courseService;
        this.submissionService = submissionService;
        this.navigator = navigator;
        this.studentId = studentId;

        // Register observer listener for real-time status updates across screens
        this.submissionService.addSubmissionListener((sId, aId) -> {
            if (pageView != null) {
                pageView.refreshActivityStatuses();
            }
            if (assignmentView != null) {
                assignmentView.refreshStatus();
            }
        });
    }

    public void setViews(CourseOverviewView overviewView, CoursePageView pageView,
                         AssignmentView assignmentView, JComponent enrollmentLegacyView) {
        this.overviewView = overviewView;
        this.pageView = pageView;
        this.assignmentView = assignmentView;
        this.enrollmentLegacyView = enrollmentLegacyView;
    }

    public void loadCourses() {
        new SwingWorker<List<CourseSummary>, Void>() {
            @Override
            protected List<CourseSummary> doInBackground() {
                return courseService.findCourses(studentId);
            }

            @Override
            protected void done() {
                try {
                    List<CourseSummary> courses = get();
                    if (overviewView != null) {
                        overviewView.setCourses(courses);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (overviewView != null) {
                        overviewView.setCourses(List.of());
                        javax.swing.JOptionPane.showMessageDialog(overviewView,
                                cause.getMessage() == null ? "Courses could not be loaded." : cause.getMessage(),
                                "Courses", javax.swing.JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    public void openCourse(CourseSummary course) {
        this.currentCourse = course;
        if (pageView != null) {
            pageView.loadCourse(course);
        }
        navigator.pushView("CoursePage");
    }

    public boolean openCourseByCode(String courseCode) {
        CourseSummary course = courseService.findCourses(studentId).stream()
                .filter(item -> item.code().equalsIgnoreCase(courseCode))
                .findFirst().orElse(null);
        if (course == null) return false;
        openCourse(course);
        return true;
    }

    public boolean showSubmission(String courseCode, String submissionId) {
        CourseSummary course = courseService.findCourses(studentId).stream()
                .filter(item -> item.code().equalsIgnoreCase(courseCode))
                .findFirst().orElse(null);
        if (course == null) return false;

        Assignment matchingAssignment = null;
        for (var section : courseService.sections(course.id())) {
            for (var activity : courseService.activities(section.id())) {
                if (activity instanceof Assignment assignment && assignment.id().equals(submissionId)) {
                    matchingAssignment = assignment;
                    break;
                }
            }
            if (matchingAssignment != null) break;
        }
        if (matchingAssignment == null) return false;

        currentCourse = course;
        pageView.loadCourse(course);
        assignmentView.loadAssignment(course, matchingAssignment);
        assignmentView.highlightCurrentSubmission();
        navigator.pushView("AssignmentPage");
        return true;
    }

    public void openAssignment(Assignment assignment) {
        if (assignmentView != null && currentCourse != null) {
            assignmentView.loadAssignment(currentCourse, assignment);
        }
        navigator.pushView("AssignmentPage");
    }

    public void openManageEnrollment() {
        navigator.pushView("LegacyEnrollment");
    }

    public void navigateToOverview() {
        navigator.resetToRoot();
        loadCourses();   // enrollment may have changed
    }

    public void navigateToCoursePage() {
        if (currentCourse != null && pageView != null) {
            navigator.pushView("CoursePage");
        } else {
            navigateToOverview();
        }
    }

    public void goBack() {
        navigator.popView();
    }
}
