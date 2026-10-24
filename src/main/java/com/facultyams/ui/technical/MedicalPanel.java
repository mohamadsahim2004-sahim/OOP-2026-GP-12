package com.facultyams.ui.technical;

import com.facultyams.dao.MedicalDAO;
import com.facultyams.model.Medical;
import com.facultyams.model.User;
import com.facultyams.security.Session;
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
import java.time.LocalDate;
import java.util.List;

public class MedicalPanel extends JPanel implements Refreshable {

    private MedicalDAO dao = new MedicalDAO();

    private JTextField attendanceField = new JTextField(5);
    private JTextField reasonField = new JTextField(15);
    private JTextField studentField = new JTextField(5);

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Attendance ID", "Reviewed By", "Date", "Reason", "Status"}, 0);
    private JTable table = new JTable(tableModel);

    public MedicalPanel() {
        setLayout(new BorderLayout(0, 10));

        //  submit a new medical
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addRow.add(new JLabel("Attendance ID"));
        addRow.add(attendanceField);
        addRow.add(new JLabel("Reason"));
        addRow.add(reasonField);
        JButton submitButton = new JButton("Submit Medical");
        addRow.add(submitButton);

        // view a student's medicals, approve or reject
        JPanel viewRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewRow.add(new JLabel("Student ID"));
        viewRow.add(studentField);
        JButton viewButton = new JButton("View Medicals");
        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");
        viewRow.add(viewButton);
        viewRow.add(approveButton);
        viewRow.add(rejectButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(addRow, BorderLayout.NORTH);
        top.add(viewRow, BorderLayout.SOUTH);

        submitButton.addActionListener(e -> submitMedical());
        viewButton.addActionListener(e -> loadTable());
        approveButton.addActionListener(e -> changeStatus("approved"));
        rejectButton.addActionListener(e -> changeStatus("rejected"));

        table.setDefaultEditor(Object.class, null);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private int currentOfficerId() {
        User me = Session.getCurrentUser();
        return me.getUserId();
    }

    private void submitMedical() {
        try {
            int attendanceId = Integer.parseInt(attendanceField.getText().trim());
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                showError("Reason cannot be empty");
                return;
            }

            //  a medical is allowed only for an absent session
            String attendanceStatus = dao.getAttendanceStatus(attendanceId);
            if (attendanceStatus == null) {
                showError("Attendance ID " + attendanceId + " does not exist");
                return;
            }
            if (!attendanceStatus.equals("absent")) {
                showError("A medical can be submitted only for an absent session");
                return;
            }

            Medical m = new Medical(attendanceId, currentOfficerId(), LocalDate.now(), reason, "pending");

            if (dao.addMedical(m)) {
                JOptionPane.showMessageDialog(this, "Medical submitted (pending)");
                loadTable();
            } else {
                showError("Could not save. A medical may already exist for this attendance.");
            }
        } catch (NumberFormatException ex) {
            showError("Attendance ID must be a number");
        }
    }

    private void loadTable() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            List<Medical> list = dao.getMedicalsByUser(studentId);

            tableModel.setRowCount(0);
            for (Medical m : list) {
                tableModel.addRow(new Object[]{
                        m.getMedicalId(), m.getAttendanceId(), m.getReviewedId(),
                        m.getSubmittedDate(), m.getReason(), m.getApprovalStatus()});
            }
        } catch (NumberFormatException ex) {
            showError("Enter a valid Student ID");
        }
    }

    private void changeStatus(String status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a medical from the table first");
            return;
        }
        int medicalId = (int) tableModel.getValueAt(row, 0);

        if (dao.updateApprovalStatus(medicalId, status, currentOfficerId())) {
            JOptionPane.showMessageDialog(this, "Medical " + status);
            loadTable();
        } else {
            showError("Could not update the medical");
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