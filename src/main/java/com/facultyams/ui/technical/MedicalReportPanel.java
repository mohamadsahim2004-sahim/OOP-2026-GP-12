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
import java.time.format.DateTimeParseException;
import java.util.List;

public class MedicalReportPanel extends JPanel implements Refreshable {

    private MedicalDAO dao = new MedicalDAO();

    private JComboBox<String> statusBox = new JComboBox<>(new String[]{"all", "pending", "approved", "rejected"});
    private JTextField fromField = new JTextField(LocalDate.now().minusMonths(6).toString(), 8);
    private JTextField toField = new JTextField(LocalDate.now().toString(), 8);
    private JLabel summaryLabel = new JLabel(" ");

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Student", "Name", "Session", "Date", "Reason", "Status"}, 0);
    private JTable table = new JTable(tableModel);

    public MedicalReportPanel() {
        setLayout(new BorderLayout(0, 10));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterRow.add(new JLabel("Status"));
        filterRow.add(statusBox);
        filterRow.add(new JLabel("From"));
        filterRow.add(fromField);
        filterRow.add(new JLabel("To"));
        filterRow.add(toField);
        JButton generateButton = new JButton("Generate Report");
        filterRow.add(generateButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(filterRow, BorderLayout.NORTH);
        top.add(summaryLabel, BorderLayout.SOUTH);

        generateButton.addActionListener(e -> generate());

        table.setDefaultEditor(Object.class, null);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void generate() {
        try {
            LocalDate from = LocalDate.parse(fromField.getText().trim());
            LocalDate to = LocalDate.parse(toField.getText().trim());
            if (from.isAfter(to)) {
                showError("The From date must be before the To date");
                return;
            }
            String status = (String) statusBox.getSelectedItem();

            List<Object[]> rows = dao.searchMedicals(status, 0, from, to);

            int pending = 0;
            int approved = 0;
            int rejected = 0;

            tableModel.setRowCount(0);
            for (Object[] row : rows) {
                tableModel.addRow(row);

                String rowStatus = (String) row[6];
                if (rowStatus.equals("pending")) {
                    pending++;
                } else if (rowStatus.equals("approved")) {
                    approved++;
                } else if (rowStatus.equals("rejected")) {
                    rejected++;
                }
            }
            summaryLabel.setText("Total: " + rows.size() + "     Pending: " + pending
                    + "     Approved: " + approved + "     Rejected: " + rejected);

        } catch (DateTimeParseException ex) {
            showError("Dates must be like 2026-10-25");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        generate();
    }
}