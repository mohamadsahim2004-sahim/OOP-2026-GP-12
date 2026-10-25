package com.facultyams.ui.technical;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.model.Attendance;
import com.facultyams.service.AttendanceService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AttendancePanel extends JPanel implements Refreshable {

    private AttendanceDAO dao = new AttendanceDAO();
    private AttendanceService service = new AttendanceService();

    private JTextField studentField = new JTextField(5);
    private JTextField componentField = new JTextField("1", 5);
    private JTextField sessionField = new JTextField(4);
    private JTextField dateField = new JTextField(LocalDate.now().toString(), 8);
    private JComboBox<String> statusBox = new JComboBox<>(new String[]{"present", "absent"});
    private JLabel summaryLabel = new JLabel(" ");

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Student", "Component", "Session", "Date", "Status"}, 0);
    private JTable table = new JTable(tableModel);

    public AttendancePanel() {
        setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Student ID"));
        form.add(studentField);
        form.add(new JLabel("Component ID"));
        form.add(componentField);
        form.add(new JLabel("Session"));
        form.add(sessionField);
        form.add(new JLabel("Date"));
        form.add(dateField);
        form.add(statusBox);

        JButton saveButton = new JButton("Save");
        JButton viewButton = new JButton("View Records");
        form.add(saveButton);
        form.add(viewButton);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionRow.add(new JLabel("Select a row, choose present/absent above, then:"));
        JButton updateButton = new JButton("Update Status");
        actionRow.add(updateButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.NORTH);
        top.add(actionRow, BorderLayout.CENTER);
        top.add(summaryLabel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> saveAttendance());
        viewButton.addActionListener(e -> loadTable());
        updateButton.addActionListener(e -> updateStatus());

        table.setDefaultEditor(Object.class, null);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void saveAttendance() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            int componentId = Integer.parseInt(componentField.getText().trim());
            int sessionNo = Integer.parseInt(sessionField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String status = (String) statusBox.getSelectedItem();

            Attendance a = new Attendance(studentId, componentId, sessionNo, date, status);

            if (dao.addAttendance(a)) {
                JOptionPane.showMessageDialog(this, "Attendance saved");
                loadTable();
            } else {
                showError("Could not save. Check the student, the component and the session (it may already exist).");
            }
        } catch (NumberFormatException ex) {
            showError("Student ID, Component ID and Session must be numbers");
        } catch (DateTimeParseException ex) {
            showError("Date must be like 2026-10-25");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a record from the table first");
            return;
        }
        int attendanceId = (int) tableModel.getValueAt(row, 0);
        String status = (String) statusBox.getSelectedItem();

        if (dao.updateStatus(attendanceId, status)) {
            JOptionPane.showMessageDialog(this, "Status updated to " + status);
            loadTable();
        } else {
            showError("Could not update the record");
        }
    }

    private void loadTable() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            List<Attendance> list = dao.getAttendanceByUser(studentId);

            tableModel.setRowCount(0);
            for (Attendance a : list) {
                tableModel.addRow(new Object[]{
                        a.getAttendanceId(), a.getUserId(), a.getComponentId(),
                        a.getSessionNo(), a.getDate(), a.getStatus()});
            }
            showSummary(studentId);
        } catch (NumberFormatException ex) {
            showError("Enter a valid Student ID");
        }
    }

    private void showSummary(int studentId) {
        try {
            int componentId = Integer.parseInt(componentField.getText().trim());
            int total = AttendanceService.TOTAL_SESSIONS;
            int present = service.getPresentCount(studentId, componentId);
            int approved = service.getApprovedCount(studentId, componentId);
            int absent = total - present - approved;
            double percentage = service.calculatePercentage(present, approved, total);

            String eligible = "No";
            if (service.isEligible(percentage)) {
                eligible = "Yes";
            }

            summaryLabel.setText(String.format(
                    "Total sessions: %d     Present: %d     Approved medical: %d     Absent: %d     Attendance: %.1f%%     Eligible: %s",
                    total, present, approved, absent, percentage, eligible));
        } catch (NumberFormatException ex) {
            summaryLabel.setText(" ");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        if (!studentField.getText().trim().isEmpty()) {
            loadTable();
        }
    }
}