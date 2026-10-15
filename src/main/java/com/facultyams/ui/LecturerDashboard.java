package com.facultyams.ui;
import javax.swing.*;
import java.awt.*;
public class LecturerDashboard extends JFrame{
    public LecturerDashboard() {
        setTitle("FacultyAMS - Lecturer Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel(
                "Welcome to the Lecturer Dashboard",
                SwingConstants.CENTER
        );

        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));

        add(welcomeLabel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LecturerDashboard dashboard = new LecturerDashboard();
            dashboard.setVisible(true);
        });
    }
}
