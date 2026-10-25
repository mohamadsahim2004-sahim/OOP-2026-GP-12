package com.facultyams.ui.technical;

import com.facultyams.dao.MedicalDAO;
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
import java.util.List;

public class MedicalRecordsPanel extends JPanel implements Refreshable {

    private MedicalDAO dao = new MedicalDAO();

    private JTextField studentField = new JTextField(6);
    private JComboBox<String> statusBox = new JComboBox<>(new String[]{"all", "pending", "approved", "rejected"});
    private JLabel countLabel = new JLabel(" ");

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Student", "Name", "Session", "Date", "Reason", "Status"}, 0);
    private JTable table = new JTable(tableModel);

    public MedicalRecordsPanel() {
        setLayout(new BorderLayout(0, 10));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Student ID (empty = all students)"));
        searchRow.add(studentField);
        searchRow.add(new JLabel("Status"));
        searchRow.add(statusBox);
        JButton searchButton = new JButton("Search");
        searchRow.add(searchButton);
        searchRow.add(countLabel);

        searchButton.addActionListener(e -> search());

        table.setDefaultEditor(Object.class, null);

        add(searchRow, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void search() {
        try {
            int studentId = 0;   
            if (!studentField.getText().trim().isEmpty()) {
                studentId = Integer.parseInt(studentField.getText().trim());
            }
            String status = (String) statusBox.getSelectedItem();

            List<Object[]> rows = dao.searchMedicals(status, studentId,
                    LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));

            tableModel.setRowCount(0);
            for (Object[] row : rows) {
                tableModel.addRow(row);
            }
            countLabel.setText("Showing: " + rows.size());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Student ID must be a number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        search();
    }
}