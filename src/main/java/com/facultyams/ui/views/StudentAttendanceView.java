package com.facultyams.ui.views;

import com.facultyams.model.Attendance;
import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentAttendanceView extends JPanel {
    private final DataService dataService = DataService.getInstance();

    public StudentAttendanceView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header and Metric Cards
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        content.add(createMetricsRow());
        content.add(Box.createVerticalStrut(18));

        // Table Card
        content.add(createTableCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Student Attendance Records");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Track your attendance across all registered lectures and practical sessions. Minimum requirement is 80%.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        card.add(left, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricsRow() {
        Student s = dataService.getCurrentStudent();
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        row.add(new StatCard("attendance", Theme.ACCENT_ROSE_BG, Theme.ACCENT_ROSE,
                "Overall Attendance", (int) s.getAttendancePercentage() + "%", "Overall Cohort Average: 82%", Theme.ACCENT_ROSE));

        row.add(new StatCard("courses", Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN,
                "Eligible Courses", "5 / 6", "Criteria >= 80% met", Theme.ACCENT_GREEN));

        row.add(new StatCard("exams", Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER,
                "At Risk Courses", "1 Course", "CS306 Web App Dev (77.8%)", Theme.ACCENT_AMBER));

        return row;
    }

    private JPanel createTableCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        JLabel title = new JLabel("Course-wise Attendance Breakdown (Semester 5)");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title, BorderLayout.NORTH);

        List<Attendance> list = dataService.getAttendanceRecords();
        String[] cols = {"Course Code", "Course Title", "Total Sessions", "Attended", "Absent", "Attendance %", "Requirement", "Eligibility Status"};
        Object[][] data = new Object[list.size()][8];

        for (int i = 0; i < list.size(); i++) {
            Attendance a = list.get(i);
            data[i][0] = a.getCourseCode();
            data[i][1] = a.getCourseName();
            data[i][2] = a.getTotalSessions();
            data[i][3] = a.getAttendedSessions();
            data[i][4] = a.getAbsentSessions();
            data[i][5] = String.format("%.1f%%", a.getPercentage());
            data[i][6] = (int) a.getRequiredPercentage() + "%";
            data[i][7] = a.isEligible() ? "Eligible" : "At Risk";
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ModernTable table = new ModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(85);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);

        table.getColumnModel().getColumn(7).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createStatusBadge((String) value));
            return p;
        });

        card.add(ModernTable.createModernScrollPane(table), BorderLayout.CENTER);
        return card;
    }
}
