package com.facultyams.ui.technical;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.dao.MedicalDAO;
import com.facultyams.ui.AppShell;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;

public class TODashboardPanel extends JPanel implements Refreshable {

    private AttendanceDAO attendanceDAO = new AttendanceDAO();
    private MedicalDAO medicalDAO = new MedicalDAO();

    private JLabel studentsLabel = new JLabel("0", SwingConstants.CENTER);
    private JLabel presentLabel = new JLabel("0", SwingConstants.CENTER);
    private JLabel absentLabel = new JLabel("0", SwingConstants.CENTER);
    private JLabel pendingLabel = new JLabel("0", SwingConstants.CENTER);
    private JLabel approvedLabel = new JLabel("0", SwingConstants.CENTER);
    private JLabel rejectedLabel = new JLabel("0", SwingConstants.CENTER);

    public TODashboardPanel(AppShell shell) {
        setLayout(new BorderLayout(0, 20));

        JPanel cards = new JPanel(new GridLayout(2, 3, 15, 15));
        cards.add(makeCard("Total Students", studentsLabel));
        cards.add(makeCard("Present Today", presentLabel));
        cards.add(makeCard("Absent Today", absentLabel));
        cards.add(makeCard("Pending Medical", pendingLabel));
        cards.add(makeCard("Approved Medical", approvedLabel));
        cards.add(makeCard("Rejected Medical", rejectedLabel));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(makeButton("Mark Attendance", shell, "mark"));
        actions.add(makeButton("View Records", shell, "records"));
        actions.add(makeButton("Submit Medical", shell, "submission"));
        actions.add(makeButton("View Reports", shell, "attendanceReports"));

        add(cards, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel makeCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(java.awt.Color.LIGHT_GRAY));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 32));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton makeButton(String text, AppShell shell, String pageKey) {
        JButton button = new JButton(text);
        button.addActionListener(e -> shell.showPage(pageKey));
        return button;
    }

    @Override
    public void refresh() {
        LocalDate today = LocalDate.now();
        studentsLabel.setText(String.valueOf(attendanceDAO.countStudents()));
        presentLabel.setText(String.valueOf(attendanceDAO.countOnDate(today, "present")));
        absentLabel.setText(String.valueOf(attendanceDAO.countOnDate(today, "absent")));
        pendingLabel.setText(String.valueOf(medicalDAO.countByStatus("pending")));
        approvedLabel.setText(String.valueOf(medicalDAO.countByStatus("approved")));
        rejectedLabel.setText(String.valueOf(medicalDAO.countByStatus("rejected")));
    }
}