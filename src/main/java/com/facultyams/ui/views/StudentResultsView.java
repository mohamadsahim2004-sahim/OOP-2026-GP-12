package com.facultyams.ui.views;

import com.facultyams.model.Marks;
import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentResultsView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private JComboBox<String> semesterFilter;
    private JPanel tableContainer;

    public StudentResultsView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Academic Summary Cards
        content.add(createSummaryMetrics());
        content.add(Box.createVerticalStrut(18));

        // Results Card with Filter
        content.add(createResultsCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel createSummaryMetrics() {
        Student s = dataService.getCurrentStudent();
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        row.add(new StatCard("cgpa", Theme.ACCENT_BLUE_BG, Theme.ACCENT_BLUE,
                "Cumulative GPA", String.format("%.2f", s.getCgpa()), "Out of 4.0 / 10.0 scale", Theme.PRIMARY));

        row.add(new StatCard("trophy", Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER,
                "Batch Rank", "#" + s.getOverallRank(), "Top 10% of Cohort", Theme.ACCENT_AMBER));

        row.add(new StatCard("courses", Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN,
                "Credits Earned", "76 Cr", "Degree requirement: 135", Theme.ACCENT_GREEN));

        row.add(new StatCard("profile", Theme.ACCENT_PURPLE_BG, Theme.ACCENT_PURPLE,
                "Academic Standing", "Dean's List", "First Class Division", Theme.ACCENT_PURPLE));

        return row;
    }

    private JPanel createResultsCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        // Filter and Action Bar
        JPanel topBar = new JPanel(new BorderLayout(14, 0));
        topBar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        JLabel filterLbl = new JLabel("Academic Period:");
        filterLbl.setFont(Theme.FONT_BODY_BOLD);
        filterLbl.setForeground(Theme.TEXT_MAIN);

        semesterFilter = new JComboBox<>(new String[]{"ALL SEMESTERS", "Semester 5 (Current)", "Semester 4", "Semester 3"});
        semesterFilter.setFont(Theme.FONT_BODY);
        semesterFilter.addActionListener(e -> refreshTable());

        leftFilters.add(filterLbl);
        leftFilters.add(semesterFilter);

        ModernButton transcriptBtn = new ModernButton("Print Academic Transcript", ModernButton.StyleType.OUTLINE);
        transcriptBtn.setIcon(IconHelper.getIcon("downloads", 16, Theme.PRIMARY));
        transcriptBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Academic Transcript generated for Abhishek Verma (TG/2021/842).\nDownloaded as PDF with Registrar's digital seal.",
                "Transcript Export", JOptionPane.INFORMATION_MESSAGE));

        topBar.add(leftFilters, BorderLayout.WEST);
        topBar.add(transcriptBtn, BorderLayout.EAST);
        card.add(topBar, BorderLayout.NORTH);

        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        card.add(tableContainer, BorderLayout.CENTER);

        return card;
    }

    private void refreshTable() {
        String filter = (String) semesterFilter.getSelectedItem();
        List<Marks> list = dataService.getAllSemesterMarks();

        if (filter != null && !filter.equals("ALL SEMESTERS")) {
            String semTag = filter.startsWith("Semester 5") ? "Semester 5" : filter;
            list = list.stream().filter(m -> m.getSemester().equalsIgnoreCase(semTag)).toList();
        }

        String[] cols = {"Course Code", "Subject Name", "Semester", "Exam Type", "Grade", "Score", "Credits", "Status"};
        Object[][] data = new Object[list.size()][8];

        for (int i = 0; i < list.size(); i++) {
            Marks m = list.get(i);
            data[i][0] = m.getCourseCode();
            data[i][1] = m.getSubject();
            data[i][2] = m.getSemester();
            data[i][3] = m.getExamType();
            data[i][4] = m.getGrade();
            data[i][5] = m.getScoreFormatted();
            data[i][6] = m.getCredits() + " Cr";
            data[i][7] = "Pass";
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

        table.getColumnModel().getColumn(4).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createGradeBadge((String) value));
            return p;
        });

        table.getColumnModel().getColumn(7).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createStatusBadge((String) value));
            return p;
        });

        tableContainer.removeAll();
        tableContainer.add(ModernTable.createModernScrollPane(table), BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }
}
