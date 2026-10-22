
package com.facultyams.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.facultyams.model.Marks;
import com.facultyams.service.MarksService;
import com.facultyams.service.LecturerService;//lecturerDashboard to use lectuerService
import com.facultyams.model.Lecturer;//to allow the dashboard to hold lecturer information in Lecturer object
import javax.swing.Box;

public class LecturerDashboard extends JFrame {
    private final MarksService marksService = new MarksService();
    private final LecturerService lecturerService = new LecturerService();

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

        for (String section : sections) { // to connect exam eligibility menu for new screen
            if (section.equals("Marks, Grades and GPA")) {
                mainContent.add(createMarksViewPanel(), section);
            } else if(section.equals("Exam Eligibility")) {
                mainContent.add(createExamEligibilityPanel(), section);
            }else{
                mainContent.add(createPlaceholder(section), section);
            }
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
        cards.setPreferredSize(new Dimension(650, 350));
        cards.setMaximumSize(new Dimension(650, 350));
        cards.setMinimumSize(new Dimension(650, 350)); cards.setPreferredSize(new Dimension(650, 350));
        cards.setMaximumSize(new Dimension(650, 350)); cards.setOpaque(false);

        addDashboardCard(cards, "Course Materials");
        addDashboardCard(cards, "Upload Exam Marks");
        addDashboardCard(cards, "Undergraduate Details");
        addDashboardCard(cards, "Marks, Grades and GPA");

        panel.add(top, BorderLayout.NORTH);

        JPanel cardsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        cardsContainer.setOpaque(false);
        cardsContainer.add(cards);

        panel.add(cardsContainer, BorderLayout.CENTER);
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
//Dashboard -> LecturerService -> LecturerDAO -> MySQL


    private JPanel createProfilePanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("My Profile");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        try {

            Lecturer lecturer = lecturerService.findByLecturerID(1);

            profilePanel.add(createProfileRow(
                    "Lecturer ID:",
                    String.valueOf(lecturer.getLecturerID())
            ));

            profilePanel.add(Box.createVerticalStrut(6));

            profilePanel.add(createProfileRow(
                    "Name:",
                    lecturer.getFullName()
            ));

            profilePanel.add(Box.createVerticalStrut(6));

            profilePanel.add(createProfileRow(
                    "Email:",
                    lecturer.getEmail()
            ));

            profilePanel.add(Box.createVerticalStrut(6));

            profilePanel.add(createProfileRow(
                    "Phone:",
                    lecturer.getPhone()
            ));

            profilePanel.add(Box.createVerticalStrut(6));

            profilePanel.add(createProfileRow(
                    "Designation:",
                    lecturer.getDesignation()
            ));

            profilePanel.add(Box.createVerticalStrut(6));

            profilePanel.add(createProfileRow(
                    "Specialisation:",
                    lecturer.getSpecialisation()
            ));

        } catch (Exception e) {

            profilePanel.add(new JLabel("Unable to load lecturer profile."));
            profilePanel.add(new JLabel(e.getMessage()));
        }

        panel.add(heading, BorderLayout.NORTH);
        panel.add(profilePanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProfileRow(String labelText, String valueText) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 30));

        JLabel value = new JLabel(valueText);

        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);

        return row;
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

        JButton validateButton = new JButton("Save Marks");

            validateButton.addActionListener(e -> {
                try {
                    int marksID = Integer.parseInt(marksIdField.getText().trim());
                    double caMarks = Double.parseDouble(caMarksField.getText().trim());
                    double finalMarks = Double.parseDouble(finalMarksField.getText().trim());
                    String grade = gradeField.getText().trim();

                    if (marksID <= 0 || grade.isEmpty()) {
                        throw new IllegalArgumentException("Enter a valid Marks ID and grade.");
                    }

                    if (!marksService.isValidateMarks(caMarks)
                            || !marksService.isValidateMarks(finalMarks)) {
                        throw new IllegalArgumentException("Marks must be between 0 and 100.");
                    }

                    Marks marks = new Marks(
                            marksID, caMarks, finalMarks, grade,
                            marksService.isCAEligible(caMarks)
                    );

                    marksService.saveMarks(marks);

                    JOptionPane.showMessageDialog(panel, "Marks saved successfully.");

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel,
                            "Enter valid numeric values.", "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "Could not save marks: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
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
    private JPanel createMarksViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel.setBackground(backgroundColor);

        JLabel heading = new JLabel("Marks, Grades and GPA");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JTextField marksIdField = new JTextField(10);
        JButton searchButton = new JButton("Search Marks");
        JTextArea resultArea = new JTextArea(8, 30);
        resultArea.setEditable(false);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Marks ID:"));
        searchPanel.add(marksIdField);
        searchPanel.add(searchButton);

        JPanel topPanel = new JPanel(new BorderLayout(0, 15));
        topPanel.setOpaque(false);

        JPanel headingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headingPanel.setOpaque(false);
        headingPanel.add(heading);

        topPanel.add(headingPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);


        searchButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(marksIdField.getText().trim());
                Marks marks = marksService.findMarksById(id);

                if (marks == null) {
                    resultArea.setText("No marks found.");
                } else {
                    resultArea.setText(
                            "Marks ID: " + marks.getMarksId()
                                    + "\nCA Marks: " + marks.getObtainMarks()
                                    + "\nFinal Marks: " + marks.getFinalMarks()
                                    + "\nGrade: " + marks.getGrade()
                                    + "\nEligible: " + marks.isEligible()
                    );
                }
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;


    }
    private JPanel createExamEligibilityPanel() { //clculates exam eligibility
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Exam Eligibility");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JTextField caField = new JTextField(10);
        JTextField attendanceField = new JTextField(10);
        JButton checkButton = new JButton("Check Eligibility");
        JLabel resultLabel = new JLabel("Enter marks and attendance.");

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.add(new JLabel("CA Marks (0-100):"));
        form.add(caField);
        form.add(new JLabel("Attendance (0-100%):"));
        form.add(attendanceField);
        form.add(new JLabel(""));
        form.add(checkButton);

        checkButton.addActionListener(e -> {
            try {
                double ca = Double.parseDouble(caField.getText().trim());
                double attendance = Double.parseDouble(
                        attendanceField.getText().trim());

                boolean eligible =
                        marksService.isExamEligiblity(ca, attendance);

                resultLabel.setText(eligible
                        ? "Eligible for final examination"
                        : "Not eligible for final examination");

            } catch (NumberFormatException ex) {
                resultLabel.setText("Enter valid numeric values.");
            } catch (IllegalArgumentException ex) {
                resultLabel.setText(ex.getMessage());
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(resultLabel, BorderLayout.CENTER);

        return panel;
    }
}
