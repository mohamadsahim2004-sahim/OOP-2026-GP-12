package com.facultyams.ui.technical;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.model.Attendance;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.DefaultCellEditor;
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

public class ClassAttendancePanel extends JPanel implements Refreshable {

    private AttendanceDAO dao = new AttendanceDAO();

    private JTextField componentField = new JTextField("1", 4);
    private JTextField sessionField = new JTextField(4);
    private JTextField dateField = new JTextField(LocalDate.now().toString(), 8);

    
    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Student ID", "Name", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }
    };
    private JTable table = new JTable(tableModel);

    public ClassAttendancePanel() {
        setLayout(new BorderLayout(0, 10));

        
        JPanel sessionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sessionRow.add(new JLabel("Component ID"));
        sessionRow.add(componentField);
        sessionRow.add(new JLabel("Session (1-15)"));
        sessionRow.add(sessionField);
        sessionRow.add(new JLabel("Date"));
        sessionRow.add(dateField);
        JButton loadButton = new JButton("Load Students");
        sessionRow.add(loadButton);

       
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton allPresentButton = new JButton("All Present");
        JButton allAbsentButton = new JButton("All Absent");
        JButton saveButton = new JButton("Save Attendance");
        actionRow.add(allPresentButton);
        actionRow.add(allAbsentButton);
        actionRow.add(saveButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(sessionRow, BorderLayout.NORTH);
        top.add(actionRow, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadStudents());
        allPresentButton.addActionListener(e -> setAll("present"));
        allAbsentButton.addActionListener(e -> setAll("absent"));
        saveButton.addActionListener(e -> saveAttendance());

       
        table.getColumnModel().getColumn(2).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{"present", "absent"})));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadStudents() {
        List<Object[]> students = dao.getAllStudents();

        tableModel.setRowCount(0);
        for (Object[] s : students) {
            tableModel.addRow(new Object[]{s[0], s[1], "present"});
        }
        if (students.isEmpty()) {
            showError("No students found");
        }
    }

    private void setAll(String status) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(status, i, 2);
        }
    }

    private void saveAttendance() {
        
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        if (tableModel.getRowCount() == 0) {
            showError("Click Load Students first");
            return;
        }

        try {
            int componentId = Integer.parseInt(componentField.getText().trim());
            int sessionNo = Integer.parseInt(sessionField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());

            int saved = 0;
            int skipped = 0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int studentId = (int) tableModel.getValueAt(i, 0);
                String status = (String) tableModel.getValueAt(i, 2);

                Attendance a = new Attendance(studentId, componentId, sessionNo, date, status);
                if (dao.addAttendance(a)) {
                    saved++;
                } else {
                    skipped++;
                }
            }
            JOptionPane.showMessageDialog(this,
                    "Saved: " + saved + "\nSkipped (already marked): " + skipped);

        } catch (NumberFormatException ex) {
            showError("Component ID and Session must be numbers");
        } catch (DateTimeParseException ex) {
            showError("Date must be like 2026-10-25");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());   
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
       
    }
}