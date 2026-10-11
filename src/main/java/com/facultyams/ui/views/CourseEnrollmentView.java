package com.facultyams.ui.views;

import com.facultyams.model.Course;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class CourseEnrollmentView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private JPanel enrolledContainer;
    private JPanel availableContainer;
    private JLabel creditLabel;
    private JProgressBar creditProgressBar;

    public CourseEnrollmentView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Top Summary Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Enrolled Courses Section
        content.add(createEnrolledSection());
        content.add(Box.createVerticalStrut(18));

        // Available Electives Section
        content.add(createAvailableSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Course Enrollment - Semester 5");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Manage your enrolled modules and electives. Maximum credit limit is 20 credits per semester.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        // Credit tracker right
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(240, 60));

        creditLabel = new JLabel("18 / 20 Credits Enrolled");
        creditLabel.setFont(Theme.FONT_BODY_BOLD);
        creditLabel.setForeground(Theme.PRIMARY);

        creditProgressBar = new JProgressBar(0, 20);
        creditProgressBar.setValue(18);
        creditProgressBar.setForeground(Theme.PRIMARY);
        creditProgressBar.setBackground(new Color(226, 232, 240));
        creditProgressBar.setBorderPainted(false);
        creditProgressBar.setPreferredSize(new Dimension(220, 10));

        right.add(creditLabel);
        right.add(Box.createVerticalStrut(6));
        right.add(creditProgressBar);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel createEnrolledSection() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        JLabel title = new JLabel("Enrolled Courses (Current Semester)");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title, BorderLayout.NORTH);

        enrolledContainer = new JPanel(new BorderLayout());
        enrolledContainer.setOpaque(false);
        card.add(enrolledContainer, BorderLayout.CENTER);

        return card;
    }

    private JPanel createAvailableSection() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        JLabel title = new JLabel("Available Electives & Modules to Enroll");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title, BorderLayout.NORTH);

        availableContainer = new JPanel(new BorderLayout());
        availableContainer.setOpaque(false);
        card.add(availableContainer, BorderLayout.CENTER);

        return card;
    }

    public void refreshData() {
        // Enrolled courses table
        List<Course> enrolled = dataService.getEnrolledCourses();
        int totalCredits = dataService.getTotalEnrolledCredits();

        creditLabel.setText(totalCredits + " / 20 Credits Enrolled");
        creditProgressBar.setValue(totalCredits);

        String[] enCols = {"Code", "Course Title", "Credits", "Lecturer", "Type", "Schedule", "Status", "Action"};
        Object[][] enData = new Object[enrolled.size()][8];
        for (int i = 0; i < enrolled.size(); i++) {
            Course c = enrolled.get(i);
            enData[i][0] = c.getCode();
            enData[i][1] = c.getTitle();
            enData[i][2] = c.getCredits() + " Credits";
            enData[i][3] = c.getLecturerName();
            enData[i][4] = c.getCourseType();
            enData[i][5] = c.getScheduleTime();
            enData[i][6] = "Enrolled";
            enData[i][7] = "Drop";
        }

        DefaultTableModel enModel = new DefaultTableModel(enData, enCols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 7;
            }
        };

        ModernTable enTable = new ModernTable(enModel);
        enTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createStatusBadge((String) value));
            return p;
        });

        enTable.getColumnModel().getColumn(7).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            ModernButton btn = new ModernButton("Drop", ModernButton.StyleType.DANGER);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            return btn;
        });

        enTable.getColumnModel().getColumn(7).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final ModernButton btn = new ModernButton("Drop", ModernButton.StyleType.DANGER);
            private String selectedCode;

            {
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btn.addActionListener(e -> {
                    fireEditingStopped();
                    if (selectedCode != null) {
                        int confirm = JOptionPane.showConfirmDialog(CourseEnrollmentView.this,
                                "Are you sure you want to drop " + selectedCode + "?",
                                "Confirm Drop Course", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            dataService.dropCourse(selectedCode);
                            refreshData();
                        }
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
                selectedCode = (String) table.getValueAt(row, 0);
                return btn;
            }
        });

        enrolledContainer.removeAll();
        enrolledContainer.add(ModernTable.createModernScrollPane(enTable), BorderLayout.CENTER);
        enrolledContainer.revalidate();
        enrolledContainer.repaint();

        // Available courses table
        List<Course> available = dataService.getAvailableCourses();
        String[] avCols = {"Code", "Course Title", "Credits", "Department", "Lecturer", "Schedule", "Action"};
        Object[][] avData = new Object[available.size()][7];
        for (int i = 0; i < available.size(); i++) {
            Course c = available.get(i);
            avData[i][0] = c.getCode();
            avData[i][1] = c.getTitle();
            avData[i][2] = c.getCredits() + " Credits";
            avData[i][3] = c.getDepartment();
            avData[i][4] = c.getLecturerName();
            avData[i][5] = c.getScheduleTime();
            avData[i][6] = "Enroll";
        }

        DefaultTableModel avModel = new DefaultTableModel(avData, avCols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 6;
            }
        };

        ModernTable avTable = new ModernTable(avModel);
        avTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            ModernButton btn = new ModernButton("Enroll", ModernButton.StyleType.PRIMARY);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            return btn;
        });

        avTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final ModernButton btn = new ModernButton("Enroll", ModernButton.StyleType.PRIMARY);
            private String selectedCode;

            {
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btn.addActionListener(e -> {
                    fireEditingStopped();
                    if (selectedCode != null) {
                        dataService.enrollCourse(selectedCode);
                        JOptionPane.showMessageDialog(CourseEnrollmentView.this,
                                "Successfully enrolled in " + selectedCode + "!",
                                "Enrollment Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
                selectedCode = (String) table.getValueAt(row, 0);
                return btn;
            }
        });

        availableContainer.removeAll();
        availableContainer.add(ModernTable.createModernScrollPane(avTable), BorderLayout.CENTER);
        availableContainer.revalidate();
        availableContainer.repaint();
    }
}
