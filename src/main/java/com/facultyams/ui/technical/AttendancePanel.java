package com.facultyams.ui.technical;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.model.Attendance;
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

    private JTextField studentField = new JTextField(5);
    private JTextField componentField = new JTextField(5);
    private JTextField sessionField = new JTextField(4);
    private JTextField dateField = new JTextField(LocalDate.now().toString(), 8);
    private JComboBox<String> statusBox = new JComboBox<>(new String[]{"present", "absent"});

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

        saveButton.addActionListener(e -> saveAttendance());
        viewButton.addActionListener(e -> loadTable());

        table.setDefaultEditor(Object.class, null);   // table la type pannamudiyaadhu

        add(form, BorderLayout.NORTH);
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
            showError("Date must be like 2026-10-23");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());   // session 1 to 15 check inga varum
        }
    }

    private void loadTable() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            List<Attendance> list = dao.getAttendanceByUser(studentId);

            tableModel.setRowCount(0);   // pazhaya rows azhikkum
            for (Attendance a : list) {
                tableModel.addRow(new Object[]{
                        a.getAttendanceId(), a.getUserId(), a.getComponentId(),
                        a.getSessionNo(), a.getDate(), a.getStatus()});
            }
        } catch (NumberFormatException ex) {
            showError("Enter a valid Student ID");
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