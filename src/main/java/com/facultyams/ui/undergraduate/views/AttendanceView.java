package com.facultyams.ui.views;

import java.awt.*;
import java.util.List;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.service.StudentAttendanceService;
import com.facultyams.service.DbStudentMedicalService;
import com.facultyams.service.DbStudentAttendanceService;
import com.facultyams.security.StudentAccess;
import com.facultyams.service.StudentMedicalService;
import com.facultyams.ui.components.UnderlineTabs;

/**
 * Root Attendance View supporting minimal overview, medical requests tab,
 * and the 3-step medical submission wizard.
 */
public class AttendanceView extends JPanel {
    private final StudentAttendanceService attendanceService;
    private final StudentMedicalService medicalService;
    private final String studentId;

    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootCardContainer = new JPanel(rootLayout);

    // Main page panels
    private final JPanel mainAttendancePanel = new JPanel(new BorderLayout(0, 14));
    private final CardLayout tabsCardLayout = new CardLayout();
    private final JPanel tabsContentPanel = new JPanel(tabsCardLayout);

    private final UnderlineTabs underlineTabs;
    private final AttendanceOverviewPanel overviewPanel;
    private final MedicalRequestsPanel requestsPanel;
    private MedicalWizardView wizardView;

    public AttendanceView() {
        this(String.valueOf(StudentAccess.currentStudentId()), new DbStudentAttendanceService(), null);
    }

    public AttendanceView(String studentId, StudentAttendanceService attendanceService, StudentMedicalService medicalService) {
        this.studentId = studentId;
        this.attendanceService = attendanceService != null ? attendanceService : new DbStudentAttendanceService();
        this.medicalService = medicalService != null ? medicalService : new DbStudentMedicalService(this.attendanceService);

        setLayout(new BorderLayout());
        setOpaque(false);

        // Sub-panels
        this.overviewPanel = new AttendanceOverviewPanel(this.attendanceService, this.studentId);
        this.requestsPanel = new MedicalRequestsPanel(this.medicalService, this.studentId);

        this.underlineTabs = new UnderlineTabs(List.of("Overview", "Medical requests"), this::switchTab);

        initMainUI();
        initObserverListeners();

        rootCardContainer.setOpaque(false);
        rootCardContainer.add(mainAttendancePanel, "MainAttendance");
        add(rootCardContainer, BorderLayout.CENTER);
    }

    private void initMainUI() {
        mainAttendancePanel.setOpaque(false);
        mainAttendancePanel.setBorder(BorderFactory.createEmptyBorder(6, 4, 16, 4));

        // 1. Header row (No big card): Title, muted subtitle, and "Submit medical" button
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);

        JLabel titleLbl = new JLabel("Attendance");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Track your session attendance and submit medical excuse requests. Minimum requirement is 80%.");
        subLbl.setFont(Theme.FONT_BODY);
        subLbl.setForeground(Theme.TEXT_MUTED);

        titleStack.add(titleLbl);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(subLbl);
        headerRow.add(titleStack, BorderLayout.WEST);

        JButton submitMedBtn = new JButton("Submit medical");
        submitMedBtn.setFont(Theme.FONT_BODY_BOLD);
        submitMedBtn.setForeground(Color.WHITE);
        submitMedBtn.setBackground(Theme.PRIMARY);
        submitMedBtn.setFocusPainted(false);
        submitMedBtn.setBorderPainted(false);
        submitMedBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitMedBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        submitMedBtn.addActionListener(e -> openMedicalWizard());
        headerRow.add(submitMedBtn, BorderLayout.EAST);

        // Top section (Header + Underline tabs)
        JPanel northSection = new JPanel();
        northSection.setLayout(new BoxLayout(northSection, BoxLayout.Y_AXIS));
        northSection.setOpaque(false);
        northSection.add(headerRow);
        northSection.add(Box.createVerticalStrut(14));
        northSection.add(underlineTabs);

        mainAttendancePanel.add(northSection, BorderLayout.NORTH);

        // Center: Tab cards
        tabsContentPanel.setOpaque(false);
        tabsContentPanel.add(overviewPanel, "Tab_Overview");
        tabsContentPanel.add(requestsPanel, "Tab_Requests");
        mainAttendancePanel.add(tabsContentPanel, BorderLayout.CENTER);
    }

    private void initObserverListeners() {
        this.attendanceService.addAttendanceListener(() -> {
            overviewPanel.loadData();
            requestsPanel.loadData();
        });

        this.medicalService.addMedicalListener(() -> {
            overviewPanel.loadData();
            requestsPanel.loadData();
        });
    }

    private void switchTab(int index) {
        if (index == 0) {
            tabsCardLayout.show(tabsContentPanel, "Tab_Overview");
            overviewPanel.loadData();
        } else {
            tabsCardLayout.show(tabsContentPanel, "Tab_Requests");
            requestsPanel.loadData();
        }
    }

    public void openMedicalWizard() {
        if (wizardView != null) {
            rootCardContainer.remove(wizardView);
        }
        wizardView = new MedicalWizardView(
                attendanceService,
                medicalService,
                studentId,
                () -> {
                    // On complete -> return to Attendance with Medical requests tab
                    rootLayout.show(rootCardContainer, "MainAttendance");
                    underlineTabs.setSelectedIndex(1);
                },
                () -> {
                    // On cancel -> return to Attendance
                    rootLayout.show(rootCardContainer, "MainAttendance");
                }
        );
        rootCardContainer.add(wizardView, "Wizard");
        rootLayout.show(rootCardContainer, "Wizard");
    }

    /** Shows the Medical requests tab of the attendance area. */
    public void showMedicalRequests() {
        rootLayout.show(rootCardContainer, "MainAttendance");
        underlineTabs.setSelectedIndex(1);
    }

    /** Reloads attendance and medical data from the database. */
    public void refresh() {
        overviewPanel.loadData();
        requestsPanel.loadData();
    }

    public void resetToOverview() {
        rootLayout.show(rootCardContainer, "MainAttendance");
        underlineTabs.setSelectedIndex(0);
    }
}
