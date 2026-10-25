package com.facultyams.ui.technical;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.dao.MedicalDAO;
import com.facultyams.model.Medical;
import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

public class MedicalSubmissionPanel extends JPanel implements Refreshable {

    private AttendanceDAO attendanceDAO = new AttendanceDAO();
    private MedicalDAO medicalDAO = new MedicalDAO();

    private JTextField studentField = new JTextField(20);
    private JTextField componentField = new JTextField("1", 20);
    private JTextField sessionField = new JTextField(20);
    private JTextField reasonField = new JTextField(20);

    public MedicalSubmissionPanel() {
        setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.add(new JLabel("Student ID"));
        form.add(studentField);
        form.add(new JLabel("Component ID (1 = theory)"));
        form.add(componentField);
        form.add(new JLabel("Absent session number"));
        form.add(sessionField);
        form.add(new JLabel("Reason"));
        form.add(reasonField);

        JButton submitButton = new JButton("Submit Medical");
        JButton clearButton = new JButton("Clear");
        form.add(submitButton);
        form.add(clearButton);

        submitButton.addActionListener(e -> submitMedical());
        clearButton.addActionListener(e -> clearForm());

        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        holder.add(form);
        add(holder, BorderLayout.NORTH);
    }

    private void submitMedical() {
        try {
            int studentId = Integer.parseInt(studentField.getText().trim());
            int componentId = Integer.parseInt(componentField.getText().trim());
            int sessionNo = Integer.parseInt(sessionField.getText().trim());
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                showError("Reason cannot be empty");
                return;
            }

            int attendanceId = attendanceDAO.findAttendanceId(studentId, componentId, sessionNo);
            if (attendanceId == -1) {
                showError("No attendance record found for that student, component and session. Mark attendance first.");
                return;
            }

            String attendanceStatus = medicalDAO.getAttendanceStatus(attendanceId);
            if (!attendanceStatus.equals("absent")) {
                showError("A medical can be submitted only for an absent session");
                return;
            }

            User me = Session.getCurrentUser();
            Medical m = new Medical(attendanceId, me.getUserId(), LocalDate.now(), reason, "pending");

            if (medicalDAO.addMedical(m)) {
                JOptionPane.showMessageDialog(this, "Medical submitted (pending approval)");
                clearForm();
            } else {
                showError("Could not save. A medical may already exist for this session.");
            }
        } catch (NumberFormatException ex) {
            showError("Student ID, Component ID and Session must be numbers");
        }
    }

    private void clearForm() {
        studentField.setText("");
        sessionField.setText("");
        reasonField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
    }
}
