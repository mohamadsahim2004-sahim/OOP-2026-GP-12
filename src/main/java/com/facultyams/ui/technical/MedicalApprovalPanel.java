package com.facultyams.ui.technical;

import com.facultyams.dao.MedicalDAO;
import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

public class MedicalApprovalPanel extends JPanel implements Refreshable {

    private MedicalDAO dao = new MedicalDAO();

    private JLabel countLabel = new JLabel(" ");
    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Student", "Name", "Session", "Date", "Reason", "Status"}, 0);
    private JTable table = new JTable(tableModel);

    public MedicalApprovalPanel() {
        setLayout(new BorderLayout(0, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");
        JButton refreshButton = new JButton("Refresh");
        buttons.add(new JLabel("Select a pending medical, then:"));
        buttons.add(approveButton);
        buttons.add(rejectButton);
        buttons.add(refreshButton);
        buttons.add(countLabel);

        approveButton.addActionListener(e -> changeStatus("approved"));
        rejectButton.addActionListener(e -> changeStatus("rejected"));
        refreshButton.addActionListener(e -> loadPending());

        table.setDefaultEditor(Object.class, null);

        add(buttons, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadPending() {
        List<Object[]> rows = dao.searchMedicals("pending", 0, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));

        tableModel.setRowCount(0);
        for (Object[] row : rows) {
            tableModel.addRow(row);
        }
        countLabel.setText("Pending: " + rows.size());
    }

    private void changeStatus(String status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a medical from the table first");
            return;
        }
        int medicalId = (int) tableModel.getValueAt(row, 0);
        User me = Session.getCurrentUser();

        if (dao.updateApprovalStatus(medicalId, status, me.getUserId())) {
            JOptionPane.showMessageDialog(this, "Medical " + status);
            loadPending();
        } else {
            showError("Could not update the medical");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        loadPending();
    }
}