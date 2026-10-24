package com.facultyams.ui.views;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.CourseAttendance;
import com.facultyams.model.SessionAttendance;
import com.facultyams.service.AttendanceService;
import com.facultyams.ui.components.ExpandableCourseRow;

/**
 * Attendance Overview with 3 compact stat cards (no icon boxes) and
 * sorted expandable course rows.
 */
public class AttendanceOverviewPanel extends JPanel {
    private final AttendanceService attendanceService;
    private final String studentId;

    private final JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
    private final JPanel coursesListPanel = new JPanel();

    public AttendanceOverviewPanel(AttendanceService attendanceService, String studentId) {
        this.attendanceService = attendanceService;
        this.studentId = studentId;

        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 16, 0));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);

        // 1. Stats Row (No colored icon boxes)
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 90));
        scrollContent.add(statsRow);
        scrollContent.add(Box.createVerticalStrut(20));

        // 2. Courses Section Card
        Ui.Card coursesCard = new Ui.Card();
        coursesCard.setLayout(new BorderLayout());
        coursesCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel titleLbl = new JLabel("Course Attendance Breakdown");
        titleLbl.setFont(Theme.FONT_SUBTITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Sorted by at-risk courses first • Click any course to view absence details and smart recovery tips");
        subLbl.setFont(Theme.FONT_SMALL);
        subLbl.setForeground(Theme.TEXT_MUTED);

        header.add(titleLbl, BorderLayout.NORTH);
        header.add(subLbl, BorderLayout.SOUTH);
        coursesCard.add(header, BorderLayout.NORTH);

        coursesListPanel.setLayout(new BoxLayout(coursesListPanel, BoxLayout.Y_AXIS));
        coursesListPanel.setOpaque(false);
        coursesCard.add(coursesListPanel, BorderLayout.CENTER);

        scrollContent.add(coursesCard);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        Ui.applyThinScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadData() {
        new SwingWorker<List<CourseAttendance>, Void>() {
            @Override
            protected List<CourseAttendance> doInBackground() {
                return attendanceService.summary(studentId);
            }

            @Override
            protected void done() {
                try {
                    List<CourseAttendance> list = get();
                    buildStats(list);
                    buildCourseRows(list);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void buildStats(List<CourseAttendance> list) {
        statsRow.removeAll();

        int totalAttended = 0;
        int totalEff = 0;
        int eligibleCount = 0;
        List<String> atRiskNames = new ArrayList<>();

        for (CourseAttendance ca : list) {
            int eff = ca.totalSessions() - ca.medicalApproved();
            totalAttended += ca.attended();
            totalEff += eff;
            if (ca.isEligible()) {
                eligibleCount++;
            } else {
                atRiskNames.add(ca.courseCode() + " " + ca.courseTitle() + " (" + String.format(Locale.ENGLISH, "%.1f%%", ca.percent()) + ")");
            }
        }

        double overallPercent = totalEff > 0 ? (totalAttended * 100.0) / totalEff : 100.0;

        // Card 1: Overall Attendance
        statsRow.add(createStatCard(
                "Overall Attendance",
                String.format(Locale.ENGLISH, "%.0f%%", overallPercent),
                "Cohort average 82%",
                Theme.TEXT_MUTED // Muted, not red!
        ));

        // Card 2: Eligible Courses
        statsRow.add(createStatCard(
                "Eligible Courses",
                eligibleCount + " of " + list.size(),
                "Criteria >= 80% met",
                Theme.SUCCESS
        ));

        // Card 3: At Risk
        String atRiskSub = atRiskNames.isEmpty() ? "No courses at risk" : String.join(", ", atRiskNames);
        statsRow.add(createStatCard(
                "At Risk",
                atRiskNames.size() + (atRiskNames.size() == 1 ? " Course" : " Courses"),
                atRiskSub,
                atRiskNames.isEmpty() ? Theme.SUCCESS : Theme.WARNING
        ));

        statsRow.revalidate();
        statsRow.repaint();
    }

    private JPanel createStatCard(String label, String value, String subtext, Color subtextColor) {
        Ui.Card card = new Ui.Card();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(Theme.FONT_SMALL);
        titleLbl.setForeground(Theme.TEXT_MUTED);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(Theme.FONT_TITLE);
        valLbl.setForeground(Theme.TEXT_MAIN);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(Theme.FONT_TINY);
        subLbl.setForeground(subtextColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    private void buildCourseRows(List<CourseAttendance> list) {
        coursesListPanel.removeAll();

        // Sort at-risk first, then by lowest percentage
        List<CourseAttendance> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            boolean aElig = a.isEligible();
            boolean bElig = b.isEligible();
            if (aElig != bElig) return aElig ? 1 : -1;
            return Double.compare(a.percent(), b.percent());
        });

        for (CourseAttendance ca : sorted) {
            List<SessionAttendance> sessions = attendanceService.sessionsForCourse(studentId, ca.courseCode());
            ExpandableCourseRow row = new ExpandableCourseRow(ca, sessions);
            coursesListPanel.add(row);
        }

        coursesListPanel.revalidate();
        coursesListPanel.repaint();
    }
}
