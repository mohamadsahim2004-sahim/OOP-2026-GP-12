package com.facultyams.ui.views;

import com.facultyams.model.Course;
import com.facultyams.model.Medical;
import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentMedicalView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private JPanel tableContainer;

    public StudentMedicalView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Metric Row
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
        refreshTable();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Student Medical Certificates & Leave Submissions");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Submit government hospital or approved medical practitioner certificates for missed examinations and lectures.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        ModernButton submitBtn = new ModernButton("Submit New Medical", ModernButton.StyleType.PRIMARY);
        submitBtn.setIcon(IconHelper.getIcon("medical", 16, Color.WHITE));
        submitBtn.addActionListener(e -> showSubmitMedicalDialog());

        card.add(left, BorderLayout.CENTER);
        card.add(submitBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel createMetricsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        row.add(new StatCard("medical", Theme.ACCENT_BLUE_BG, Theme.ACCENT_BLUE,
                "Total Submissions", "2", "Academic Year 2023/2024", Theme.PRIMARY));

        row.add(new StatCard("attendance", Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN,
                "Approved Certificates", "1", "Verified by University CMO", Theme.ACCENT_GREEN));

        row.add(new StatCard("exams", Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER,
                "Under Review", "1", "Awaiting Medical Board sign-off", Theme.ACCENT_AMBER));

        return row;
    }

    private JPanel createTableCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        JLabel title = new JLabel("Submitted Medical Records");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title, BorderLayout.NORTH);

        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        card.add(tableContainer, BorderLayout.CENTER);

        return card;
    }

    private void refreshTable() {
        List<Medical> list = dataService.getMedicalRecords();
        String[] cols = {"Ref Number", "Course Affected", "Leave Period", "Illness / Diagnosis", "Doctor / Clinic", "Submitted On", "Status", "Reviewer Remarks"};
        Object[][] data = new Object[list.size()][8];

        for (int i = 0; i < list.size(); i++) {
            Medical m = list.get(i);
            data[i][0] = m.getRefNo();
            data[i][1] = m.getCourseCode() + " (" + m.getCourseName() + ")";
            data[i][2] = m.getStartDate() + " to " + m.getEndDate();
            data[i][3] = m.getReason();
            data[i][4] = m.getDoctorName() + ", " + m.getHospitalClinic();
            data[i][5] = m.getSubmissionDate();
            data[i][6] = m.getStatus();
            data[i][7] = m.getAdminRemarks();
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ModernTable table = new ModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);

        table.getColumnModel().getColumn(6).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
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

    private void showSubmitMedicalDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Submit Medical Certificate", true);
        dlg.setSize(520, 500);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JLabel titleLbl = new JLabel("New Medical Certificate Form");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        // Course dropdown
        JComboBox<String> courseBox = new JComboBox<>();
        for (Course c : dataService.getEnrolledCourses()) {
            courseBox.addItem(c.getCode() + " - " + c.getTitle());
        }

        JTextField reasonField = new JTextField();
        JTextField startField = new JTextField("2024-05-18");
        JTextField endField = new JTextField("2024-05-20");
        JTextField doctorField = new JTextField("Dr. K. S. Perera (MBBS)");
        JTextField hospitalField = new JTextField("National Hospital of Sri Lanka");

        p.add(titleLbl);
        p.add(Box.createVerticalStrut(14));

        p.add(createFormField("Course Unit Affected:", courseBox));
        p.add(Box.createVerticalStrut(8));
        p.add(createFormField("Medical Condition / Illness:", reasonField));
        p.add(Box.createVerticalStrut(8));

        JPanel datesPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        datesPanel.setOpaque(false);
        datesPanel.add(createFormField("Start Date (YYYY-MM-DD):", startField));
        datesPanel.add(createFormField("End Date (YYYY-MM-DD):", endField));
        p.add(datesPanel);
        p.add(Box.createVerticalStrut(8));

        p.add(createFormField("Attending Doctor:", doctorField));
        p.add(Box.createVerticalStrut(8));
        p.add(createFormField("Hospital / Medical Institution:", hospitalField));
        p.add(Box.createVerticalStrut(16));

        // Submit button
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.StyleType.GHOST);
        cancelBtn.addActionListener(e -> dlg.dispose());

        ModernButton submitBtn = new ModernButton("Submit Certificate", ModernButton.StyleType.PRIMARY);
        submitBtn.addActionListener(e -> {
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please specify the illness condition / reason.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selCourse = (String) courseBox.getSelectedItem();
            String code = selCourse != null ? selCourse.split(" - ")[0].trim() : "CS301";
            String cName = selCourse != null ? selCourse.split(" - ")[1].trim() : "Course";

            Student s = dataService.getCurrentStudent();
            String ref = "MED-2024-00" + (dataService.getMedicalRecords().size() + 1);
            String dateNow = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            Medical newMed = new Medical(ref, s.getRegNo(), code, cName,
                    startField.getText(), endField.getText(), reason,
                    doctorField.getText(), hospitalField.getText(),
                    dateNow, "Pending", "Submitted online, pending verification.");

            dataService.addMedicalRecord(newMed);
            dlg.dispose();
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Medical Certificate (" + ref + ") submitted successfully for review!",
                    "Submission Received", JOptionPane.INFORMATION_MESSAGE);
        });

        btnRow.add(cancelBtn);
        btnRow.add(submitBtn);
        p.add(btnRow);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private JPanel createFormField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);

        field.setFont(Theme.FONT_BODY);
        if (field instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
        }

        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }
}
