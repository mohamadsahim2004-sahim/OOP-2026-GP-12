package com.facultyams.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.controller.CourseController;
import com.facultyams.controller.Navigator;
import com.facultyams.service.StudentCourseService;
import com.facultyams.service.FileSubmissionService;
import com.facultyams.service.SubmissionService;

/**
 * Root container for the Moodle-style Courses area.
 * Hosts Screen A (Overview), Screen B (Course Page), Screen C (Assignment),
 * and Screen D (Legacy Course Enrollment), wired via Navigator.
 */
public class CoursesContainerView extends JPanel {
    private final CardLayout cardLayout = new CardLayout();
    private final Navigator navigator;
    private final CourseController courseController;

    private final CourseOverviewView overviewView;
    private final CoursePageView pageView;
    private final AssignmentView assignmentView;

    public CoursesContainerView(JComponent legacyEnrollmentComponent) {
        this(String.valueOf(com.facultyams.security.StudentAccess.currentStudentId()),
                new com.facultyams.service.DbStudentCourseService(), new FileSubmissionService(), legacyEnrollmentComponent);
    }

    public CoursesContainerView(String studentId, StudentCourseService courseService,
                                SubmissionService submissionService, JComponent legacyEnrollmentComponent) {
        setLayout(cardLayout);
        setOpaque(false);

        navigator = new Navigator(this, cardLayout, "Overview");
        courseController = new CourseController(courseService, submissionService, navigator, studentId);

        // Screen A: Course Overview
        overviewView = new CourseOverviewView(
                courseController::openCourse,
                courseController::openManageEnrollment
        );

        // Screen B: Course Page
        pageView = new CoursePageView(
                courseService,
                submissionService,
                studentId,
                courseController::goBack,
                courseController::openAssignment
        );

        // Screen C: Assignment View
        assignmentView = new AssignmentView(
                submissionService,
                studentId,
                courseController::navigateToOverview,
                courseController::navigateToCoursePage
        );

        // Screen D: Legacy Enrollment Wrapper (with back button)
        JPanel legacyWrapper = createLegacyWrapper(legacyEnrollmentComponent);

        // Register with Navigator
        navigator.registerView("Overview", overviewView);
        navigator.registerView("CoursePage", pageView);
        navigator.registerView("AssignmentPage", assignmentView);
        navigator.registerView("LegacyEnrollment", legacyWrapper);

        courseController.setViews(overviewView, pageView, assignmentView, legacyWrapper);
        courseController.loadCourses();
    }

    private JPanel createLegacyWrapper(JComponent legacyComponent) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        if (legacyComponent != null) {
            if (legacyComponent instanceof com.facultyams.ui.views.CourseEnrollmentView enrollmentView) {
                enrollmentView.setBackAction(courseController::navigateToOverview);
            }
            wrapper.add(legacyComponent, BorderLayout.CENTER);
        }
        return wrapper;
    }

    public void setOnOpenMaterials(Runnable action) {
        overviewView.setOnOpenMaterials(action);
    }

    public void resetToOverview() {
        courseController.navigateToOverview();
    }

    public boolean openCourse(String courseCode) {
        return courseController.openCourseByCode(courseCode);
    }

    public boolean showSubmission(String courseCode, String submissionId) {
        return courseController.showSubmission(courseCode, submissionId);
    }
}
