
package com.facultyams.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LecturerDashboard extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContent = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();

    private final Color navy = new Color(23, 54, 92);
    private final Color sidebarColor = new Color(229, 236, 245);
    private final Color backgroundColor = new Color(244, 247, 251);
    private final Color textColor = new Color(40, 69, 99);

    public LecturerDashboard() {
        setTitle("FacultyAMS - Lecturer Portal");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createHeader();
        createSidebar();
        createDashboard();
    }

    private void createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("FacultyAMS - Lecturer Portal");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JButton notification = new JButton("Notices");
        notification.addActionListener(e -> showSection("Notices"));

        header.add(title, BorderLayout.WEST);
        header.add(notification, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void createSidebar() {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(sidebarColor);
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));
        sidebar.setPreferredSize(new Dimension(275, 0));

        JLabel menuTitle = new JLabel("MAIN MENU");
        menuTitle.setFont(new Font("Arial", Font.BOLD, 12));
        menuTitle.setForeground(textColor);
        menuTitle.setBorder(new EmptyBorder(0, 8, 15, 0));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuTitle);

        String[] items = {
                "Dashboard",
                "My Profile",
                "Course Materials",
                "Upload Exam Marks",
                "Undergraduate Details",
                "Exam Eligibility",
                "Marks, Grades and GPA",
                "Attendance and Medical Records",
                "Notices"
        };

        for (String item : items) {
            JButton button = createMenuButton(item);

            button.addActionListener(e -> showSection(item));

            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }

        JButton backButton = createMenuButton("Back");
        backButton.addActionListener(e -> dispose());

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(backButton);

        add(sidebar, BorderLayout.WEST);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setPreferredSize(new Dimension(250, 40));
        button.setBackground(sidebarColor);
        button.setForeground(textColor);
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void createDashboard() {
        mainContent.setBackground(backgroundColor);

        mainContent.add(createHomePanel(), "Dashboard");
        mainContent.add(createProfilePanel(), "My Profile");
        mainContent.add(createUploadMarksPanel(), "Upload Exam Marks");

        String[] sections = {
                "Course Materials",
                "Undergraduate Details",
                "Exam Eligibility",
                "Marks, Grades and GPA",
                "Attendance and Medical Records",
                "Notices"
        };

        for (String section : sections) {
            mainContent.add(createPlaceholder(section), section);
        }

        add(mainContent, BorderLayout.CENTER);
        showSection("Dashboard");
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel heading = new JLabel("Lecturer Dashboard");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(navy);

        JLabel welcome = new JLabel("Welcome back, Lecturer!");
        welcome.setFont(new Font("Arial", Font.PLAIN, 15));
        welcome.setForeground(textColor);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 8));
        top.setOpaque(false);
        top.add(heading);
        top.add(welcome);

        JPanel cards = new JPanel(new GridLayout(2, 2, 15, 15));
        cards.setOpaque(false);

        addDashboardCard(cards, "Course Materials");
        addDashboardCard(cards, "Upload Exam Marks");
        addDashboardCard(cards, "Undergraduate Details");
        addDashboardCard(cards, "Marks, Grades and GPA");

        panel.add(top, BorderLayout.NORTH);
        panel.add(cards, BorderLayout.CENTER);

        return panel;
    }

    private void addDashboardCard(JPanel panel, String destination) {
        JButton card = new JButton(
                "<html><center><b>" + destination
                        + "</b><br>Open section</center></html>"
        );

        card.setFont(new Font("Arial", Font.PLAIN, 16));
        card.setBackground(Color.WHITE);
        card.setForeground(navy);
        card.setFocusPainted(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        card.addActionListener(e -> showSection(destination));

        panel.add(card);
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("My Profile");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JLabel message = new JLabel(
                "Lecturer profile details will appear here."
        );
        message.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 12));
        content.setOpaque(false);
        content.add(heading);
        content.add(message);

        panel.add(content, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createUploadMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Upload Exam Marks");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField marksIdField = new JTextField();
        JTextField caMarksField = new JTextField();
        JTextField finalMarksField = new JTextField();
        JTextField gradeField = new JTextField();

        form.add(new JLabel("Marks ID:"));
        form.add(marksIdField);

        form.add(new JLabel("CA Marks (0-100):"));
        form.add(caMarksField);

        form.add(new JLabel("Final Marks (0-100):"));
        form.add(finalMarksField);

        form.add(new JLabel("Grade:"));
        form.add(gradeField);

        JButton validateButton = new JButton("Validate Marks");

        validateButton.addActionListener(e -> {
            try {
                int marksID = Integer.parseInt(
                        marksIdField.getText().trim()
                );

                double caMarks = Double.parseDouble(
                        caMarksField.getText().trim()
                );

                double finalMarks = Double.parseDouble(
                        finalMarksField.getText().trim()
                );

                if (marksID <= 0) {
                    throw new IllegalArgumentException(
                            "Marks ID must be greater than zero."
                    );
                }

                if (caMarks < 0 || caMarks > 100
                        || finalMarks < 0 || finalMarks > 100) {
                    throw new IllegalArgumentException(
                            "Marks must be between 0 and 100."
                    );
                }

                if (gradeField.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Please enter a grade."
                    );
                }

                JOptionPane.showMessageDialog(
                        panel,
                        "Marks input validated successfully."
                );

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        panel,
                        "Enter a valid Marks ID and numeric marks.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                );

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        panel,
                        ex.getMessage(),
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(validateButton);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPlaceholder(String section) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel(section);
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JLabel message = new JLabel(
                "The " + section + " section is ready for development."
        );
        message.setFont(new Font("Arial", Font.PLAIN, 14));
        message.setForeground(textColor);

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 12));
        content.setOpaque(false);
        content.add(heading);
        content.add(message);

        panel.add(content, BorderLayout.NORTH);

        return panel;
    }

    private void showSection(String name) {
        cardLayout.show(mainContent, name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LecturerDashboard dashboard = new LecturerDashboard();
            dashboard.setVisible(true);
        });
    }
}
