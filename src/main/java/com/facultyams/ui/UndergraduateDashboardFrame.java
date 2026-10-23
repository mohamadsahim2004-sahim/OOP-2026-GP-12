package com.facultyams.ui;

import com.facultyams.ui.components.FooterPanel;
import com.facultyams.ui.components.SidebarPanel;
import com.facultyams.ui.components.TopBarPanel;
import com.facultyams.ui.theme.Theme;
import com.facultyams.ui.views.*;
import com.srms.controller.NavigationListener;
import com.srms.ui.components.Toast;
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
    private com.srms.ui.views.CoursesContainerView coursesContainerView;
    private CourseMaterialsView courseMaterialsView;
    private StudentResultsView resultsView;
    private StudentAttendanceView attendanceView;
    private com.srms.ui.views.AttendanceView modernAttendanceView;
    private StudentMedicalView medicalView;
    private TimetableView timetableView;
    private NoticesView noticesView;
    private String currentView = "Dashboard";

    public UndergraduateDashboardFrame() {
        setTitle("Student Result Management System (SRMS) - Undergraduate Portal");
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
        coursesContainerView = new com.srms.ui.views.CoursesContainerView(courseEnrollmentView);
        courseMaterialsView = new CourseMaterialsView();
        resultsView = new StudentResultsView();
        attendanceView = new StudentAttendanceView();
        modernAttendanceView = new com.srms.ui.views.AttendanceView();
        medicalView = new StudentMedicalView();
        timetableView = new TimetableView();
        timetableView.setNotificationIntegration(topBarPanel::setNotificationCount, timetableView::showReminders);
        timetableView.setClearSearchAction(topBarPanel::clearSearch);
        noticesView = new NoticesView();

        cardsPanel.add(dashboardView, "Dashboard");
        cardsPanel.add(profileView, "Profile");
        cardsPanel.add(coursesContainerView, "Course Enrollment");
        cardsPanel.add(courseMaterialsView, "Course Materials");
        cardsPanel.add(resultsView, "Results");
        cardsPanel.add(modernAttendanceView, "Attendance");
        cardsPanel.add(medicalView, "Medical Records");
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
        if ("Attendance".equals(normalized) && modernAttendanceView != null) {
            modernAttendanceView.resetToOverview();
        }
        topBarPanel.setTitle(normalized);
        topBarPanel.setSearchInputActive(!"Profile".equals(normalized));
        currentView = normalized;
        sidebarPanel.setActiveView(normalized);
        if ("Timetable".equals(normalized)) {
            topBarPanel.setNotificationAction(timetableView::openReminderPanel);
        } else {
            topBarPanel.setNotificationAction(() -> navigateTo("Notice Board"));
        }
        cardLayout.show(cardsPanel, normalized);
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
        JOptionPane.showMessageDialog(this,
                "Searched for: '" + query + "'\nShowing results in Notice Board & Course Materials.",
                "Search", JOptionPane.INFORMATION_MESSAGE);
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

    public static void main(String[] args) {
        try {
            // Set smooth system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            UndergraduateDashboardFrame frame = new UndergraduateDashboardFrame();
            frame.setVisible(true);
        });
    }
}
