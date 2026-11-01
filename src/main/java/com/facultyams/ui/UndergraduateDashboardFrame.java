package com.facultyams.ui;

import com.facultyams.model.Student;
import com.facultyams.security.Session;
import com.facultyams.service.StudentNoticeService;
import com.facultyams.ui.components.FooterPanel;
import com.facultyams.ui.components.SidebarPanel;
import com.facultyams.ui.components.TopBarPanel;
import com.facultyams.ui.theme.Theme;
import com.facultyams.ui.views.*;
import com.facultyams.controller.NavigationListener;
import com.facultyams.ui.components.Toast;
import java.awt.*;
import javax.swing.*;

public class UndergraduateDashboardFrame extends JFrame implements NavigationListener {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);
    private SidebarPanel sidebarPanel;
    private TopBarPanel topBarPanel;
    private boolean sidebarVisible = true;

    // View Instances
    private DashboardView dashboardView;
    private StudentProfileView profileView;
    private CourseEnrollmentView courseEnrollmentView;
    private com.facultyams.ui.views.CoursesContainerView coursesContainerView;
    private CourseMaterialsView courseMaterialsView;
    private StudentResultsView resultsView;
    private com.facultyams.ui.views.AttendanceView modernAttendanceView;
    private TimetableView timetableView;
    private NoticesView noticesView;
    private String currentView = "Dashboard";

    /**
     * Opens the portal for the logged-in student. The student comes from the authenticated session;
     * every screen reads its data through services that re-check the session on each call.
     */
    public UndergraduateDashboardFrame(Student student) {
        if (student == null || Session.getCurrentUser() == null
                || Session.getCurrentUser().getUserId() != student.getUserId()) {
            throw new IllegalStateException("Please log in as a student to open the portal.");
        }
        setTitle("Faculty AMS - Undergraduate Portal - " + student.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 880);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_MAIN);

        // Sidebar
        sidebarPanel = new SidebarPanel(this::navigateTo);
        root.add(sidebarPanel, BorderLayout.WEST);

        // Right Main Area (TopBar + Card Views + Footer)
        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setOpaque(false);

        // Top Bar
        topBarPanel = new TopBarPanel("Dashboard", this::toggleSidebar, this::handleSearch, this::navigateTo);
        rightArea.add(topBarPanel, BorderLayout.NORTH);

        // Center Views
        initViews();
        rightArea.add(cardsPanel, BorderLayout.CENTER);

        root.add(rightArea, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void initViews() {
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(Theme.createEmptyPadding(20, 24, 16, 24));

        dashboardView = new DashboardView(this::navigateTo, this);
        profileView = new StudentProfileView();
        courseEnrollmentView = new CourseEnrollmentView();
        coursesContainerView = new com.facultyams.ui.views.CoursesContainerView(courseEnrollmentView);
        courseMaterialsView = new CourseMaterialsView();
        coursesContainerView.setOnOpenMaterials(() -> navigateTo("Course Materials"));
        resultsView = new StudentResultsView();
        modernAttendanceView = new com.facultyams.ui.views.AttendanceView();
        timetableView = new TimetableView();
        timetableView.setNotificationIntegration(count -> {
            if ("Timetable".equals(currentView)) topBarPanel.setNotificationCount(count);
        }, timetableView::showReminders);
        timetableView.setClearSearchAction(topBarPanel::clearSearch);
        noticesView = new NoticesView(this);

        cardsPanel.add(dashboardView, "Dashboard");
        cardsPanel.add(profileView, "Profile");
        cardsPanel.add(coursesContainerView, "Course Enrollment");
        cardsPanel.add(courseMaterialsView, "Course Materials");
        cardsPanel.add(resultsView, "Results");
        cardsPanel.add(modernAttendanceView, "Attendance");
        cardsPanel.add(timetableView, "Timetable");
        cardsPanel.add(noticesView, "Notice Board");

        cardLayout.show(cardsPanel, "Dashboard");
    }

    public void navigateTo(String viewName) {
        String normalized = normalizeViewName(viewName);
        if ("Profile".equals(currentView) && !"Profile".equals(normalized)
                && profileView != null && !profileView.confirmNavigation()) {
            return;
        }
        if ("Course Enrollment".equals(normalized) && coursesContainerView != null) {
            coursesContainerView.resetToOverview();
        }
        boolean medical = "Medical Records".equals(normalized);
        if ("Attendance".equals(normalized) && modernAttendanceView != null) {
            modernAttendanceView.resetToOverview();
        }
        if (medical && modernAttendanceView != null) {
            modernAttendanceView.showMedicalRequests();
        }
        // Medical records live in the Attendance area (Medical requests tab).
        String card = medical ? "Attendance" : normalized;
        topBarPanel.setTitle(normalized);
        topBarPanel.setSearchInputActive(!"Profile".equals(normalized));
        currentView = card;
        sidebarPanel.setActiveView(card);
        if ("Timetable".equals(normalized)) {
            topBarPanel.setNotificationAction(timetableView::openReminderPanel);
        } else {
            topBarPanel.setNotificationAction(() -> navigateTo("Notice Board"));
        }
        cardLayout.show(cardsPanel, card);
        refreshView(card);
    }

    /** Reloads the data of the screen that was just opened, so it never shows stale rows. */
    private void refreshView(String card) {
        switch (card) {
            case "Dashboard" -> dashboardView.refresh();
            case "Profile" -> profileView.reload();
            case "Course Materials" -> courseMaterialsView.reload();
            case "Results" -> resultsView.reload();
            case "Attendance" -> modernAttendanceView.refresh();
            case "Timetable" -> timetableView.reload();
            case "Notice Board" -> noticesView.reload();
            default -> { }
        }
        refreshNoticeBadge(card);
    }

    private void refreshNoticeBadge(String card) {
        if ("Timetable".equals(card)) {
            timetableView.setNotificationIntegration(topBarPanel::setNotificationCount, timetableView::showReminders);
            return;
        }
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                return StudentNoticeService.getInstance().unreadCount();
            }
            @Override protected void done() {
                try {
                    if (!"Timetable".equals(currentView)) topBarPanel.setNotificationCount(get());
                } catch (Exception ignored) {
                    // the badge is cosmetic; a failed count must not disturb the screen
                }
            }
        }.execute();
    }

    private String normalizeViewName(String raw) {
        if (raw == null) return "Dashboard";
        String s = raw.trim().toLowerCase();
        if (s.contains("dash")) return "Dashboard";
        if (s.contains("profile")) return "Profile";
        if (s.contains("enroll") || s.contains("courses")) return "Course Enrollment";
        if (s.contains("material") || s.contains("download")) return "Course Materials";
        if (s.contains("result")) return "Results";
        if (s.contains("attend")) return "Attendance";
        if (s.contains("med")) return "Medical Records";
        if (s.contains("time") || s.contains("calendar")) return "Timetable";
        if (s.contains("notice")) return "Notice Board";
        return "Dashboard";
    }

    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebarPanel.setVisible(sidebarVisible);
        revalidate();
        repaint();
    }

    private void handleSearch(String query) {
        if ("Timetable".equals(currentView)) {
            timetableView.setSearchQuery(query);
            return;
        }
        navigateTo("Notice Board");
        noticesView.setSearchQuery(query);
    }

    @Override
    public void openCourse(String courseCode) {
        navigateTo("Course Enrollment");
        if (!coursesContainerView.openCourse(courseCode)) {
            Toast.show(cardsPanel, "Course not found");
        }
    }

    @Override
    public void openSubmission(String courseCode, String submissionId) {
        navigateTo("Course Enrollment");
        if (!coursesContainerView.showSubmission(courseCode, submissionId)) {
            Toast.show(cardsPanel, "Submission not found");
        }
    }

    @Override
    public void openTimetable() {
        navigateTo("Timetable");
    }

    /** Standalone launch goes through the normal login screen; the portal needs an authenticated student. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
