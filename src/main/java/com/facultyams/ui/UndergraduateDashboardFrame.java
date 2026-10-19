package com.facultyams.ui;

import com.facultyams.ui.components.FooterPanel;
import com.facultyams.ui.components.SidebarPanel;
import com.facultyams.ui.components.TopBarPanel;
import com.facultyams.ui.theme.Theme;
import com.facultyams.ui.views.*;
import java.awt.*;
import javax.swing.*;

public class UndergraduateDashboardFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);
    private SidebarPanel sidebarPanel;
    private TopBarPanel topBarPanel;
    private boolean sidebarVisible = true;

    // View Instances
    private DashboardView dashboardView;
    private StudentProfileView profileView;
    private CourseEnrollmentView courseEnrollmentView;
    private CourseMaterialsView courseMaterialsView;
    private StudentResultsView resultsView;
    private StudentAttendanceView attendanceView;
    private StudentMedicalView medicalView;
    private TimetableView timetableView;
    private NoticesView noticesView;

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

        dashboardView = new DashboardView(this::navigateTo);
        profileView = new StudentProfileView();
        courseEnrollmentView = new CourseEnrollmentView();
        courseMaterialsView = new CourseMaterialsView();
        resultsView = new StudentResultsView();
        attendanceView = new StudentAttendanceView();
        medicalView = new StudentMedicalView();
        timetableView = new TimetableView();
        noticesView = new NoticesView();

        cardsPanel.add(dashboardView, "Dashboard");
        cardsPanel.add(profileView, "Profile");
        cardsPanel.add(courseEnrollmentView, "Course Enrollment");
        cardsPanel.add(courseMaterialsView, "Course Materials");
        cardsPanel.add(resultsView, "Results");
        cardsPanel.add(attendanceView, "Attendance");
        cardsPanel.add(medicalView, "Medical Records");
        cardsPanel.add(timetableView, "Timetable");
        cardsPanel.add(noticesView, "Notice Board");

        cardLayout.show(cardsPanel, "Dashboard");
    }

    public void navigateTo(String viewName) {
        String normalized = normalizeViewName(viewName);
        topBarPanel.setTitle(normalized);
        sidebarPanel.setActiveView(normalized);
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
        navigateTo("Notice Board");
        JOptionPane.showMessageDialog(this,
                "Searched for: '" + query + "'\nShowing results in Notice Board & Course Materials.",
                "Search", JOptionPane.INFORMATION_MESSAGE);
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
