package com.facultyams.ui.technical;

import com.facultyams.model.AttendanceSummary;
import com.facultyams.service.AttendanceService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class AttendanceReportPanel extends JPanel implements Refreshable {

    private AttendanceService service = new AttendanceService();

    private JTextField componentField = new JTextField("1", 4);
    private JTextField practicalField = new JTextField("2", 4);
    private JTextField studentField = new JTextField(5);
    private JLabel resultLabel = new JLabel(" ");

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Student", "Attendance %", "Eligible"}, 0);
    private JTable table = new JTable(tableModel);

    public AttendanceReportPanel() {
        setLayout(new BorderLayout(0, 10));

        // row 1: whole batch
        JPanel batchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        batchRow.add(new JLabel("Component ID"));
        batchRow.add(componentField);
        JButton batchButton = new JButton("Batch Summary");
        batchRow.add(batchButton);

        // row 2: individual student
        JPanel studentRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentRow.add(new JLabel("Student ID"));
        studentRow.add(studentField);
        JButton individualButton = new JButton("Individual");
        studentRow.add(individualButton);
        studentRow.add(new JLabel("Practical Component ID"));
        studentRow.add(practicalField);
        JButton combinedButton = new JButton("Combined (Theory + Practical)");
        studentRow.add(combinedButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(batchRow, BorderLayout.NORTH);
        top.add(studentRow, BorderLayout.CENTER);
        top.add(resultLabel, BorderLayout.SOUTH);

        batchButton.addActionListener(e -> showBatch());
        individualButton.addActionListener(e -> showIndividual());
        combinedButton.addActionListener(e -> showCombined());

        table.setDefaultEditor(Object.class, null);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void showBatch() {
        try {
            int componentId = Integer.parseInt(componentField.getText().trim());
            List<AttendanceSummary> list = service.getBatchSummary(componentId);

            tableModel.setRowCount(0);
            for (AttendanceSummary s : list) {
                String eligible = "No";
                if (s.isEligible()) {
                    eligible = "Yes";
                }
                tableModel.addRow(new Object[]{
                        s.getUserId(), String.format("%.1f%%", s.getPercentage()), eligible});
            }
            resultLabel.setText("Students: " + list.size());
        } catch (NumberFormatException ex) {
            showError("Component ID must be a number");
        }
    }

    private void showIndividual() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            int componentId = Integer.parseInt(componentField.getText().trim());
            double p = service.getStudentPercentage(studentId, componentId);
            resultLabel.setText("Student " + studentId + " (component " + componentId + "): "
                    + String.format("%.1f%%", p) + "   Eligible: " + service.isEligible(p));
        } catch (NumberFormatException ex) {
            showError("Student ID and Component ID must be numbers");
        }
    }

    private void showCombined() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            int theoryId = Integer.parseInt(componentField.getText().trim());
            int practicalId = Integer.parseInt(practicalField.getText().trim());
            double p = service.getCombinedPercentage(studentId, theoryId, practicalId);
            resultLabel.setText("Student " + studentId + " combined: "
                    + String.format("%.1f%%", p) + "   Eligible: " + service.isEligible(p));
        } catch (NumberFormatException ex) {
            showError("IDs must be numbers");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
    }
}