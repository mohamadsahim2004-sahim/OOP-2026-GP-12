
package com.facultyams.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LecturerDashboard extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContent = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();
    private JButton selectedButton;

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
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel logo = new JLabel("F", SwingConstants.CENTER);        logo.setFont(new Font("Arial", Font.BOLD, 25));
        logo.setForeground(navy);
        logo.setOpaque(true);
        logo.setBackground(Color.WHITE);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(40, 40));

        JLabel title = new JLabel("FacultyAMS");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Lecturer Portal");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(215, 229, 246));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(logo);
        left.add(titlePanel);

        JButton notification = new JButton("N");        notification.setForeground(Color.WHITE);
        notification.setBackground(navy);
        notification.setBorderPainted(false);
        notification.setFocusPainted(false);
        notification.setFont(new Font("Arial", Font.PLAIN, 20));
        notification.addActionListener(e ->
                showSection("Notices"));

        header.add(left, BorderLayout.WEST);
        header.add(notification, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void createSidebar() {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(sidebarColor);
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));
        sidebar.setPreferredSize(new Dimension(275, 0));

        JLabel menuTitle = new JLabel("MAIN MENU");
        menuTitle.setFont(new Font("Arial", Font.BOLD, 11));
        menuTitle.setForeground(new Color(82, 103, 130));
        menuTitle.setBorder(new EmptyBorder(0, 8, 10, 0));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuTitle);
        String[][] items = {
                {"dashboard", "Dashboard"},
                {"profile", "My Profile"},
                {"book", "Course Materials"},
                {"edit", "Upload Exam Marks"},
                {"students", "Undergraduate Details"},
                {"check", "Exam Eligibility"},
                {"chart", "Marks, Grades and GPA"},
                {"calendar", "Attendance and Medical Records"},
                {"notice", "Notices"}
        };


        for (String[] item : items) {

            JButton button = createMenuButton("", item[1]);

            button.setIcon(new MenuIcon(item[0], textColor));
            button.setIconTextGap(12); button.addActionListener(e -> {
                selectMenuButton(button);
                showSection(item[1]);
            });

            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }

        JButton backButton = createMenuButton("←", "Back");
        backButton.addActionListener(e -> dispose());

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(backButton);

        add(sidebar, BorderLayout.WEST);
    }

    private JButton createMenuButton(String icon, String text) {
        JButton button = new JButton(icon + "   " + text);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setPreferredSize(new Dimension(220, 38));
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(sidebarColor);
        button.setForeground(textColor);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void selectMenuButton(JButton button) {
        for (Component component : sidebar.getComponents()) {
            if (component instanceof JButton) {
                component.setBackground(sidebarColor);
                component.setForeground(textColor);
            }
        }

        button.setBackground(navy);
        button.setForeground(Color.WHITE);
        selectedButton = button;
    }

    private void createDashboard() {
        mainContent.setBackground(backgroundColor);

        mainContent.add(createHomePanel(), "Dashboard");

        String[] sections = {
                "My Profile",
                "Course Materials",
                "Upload Exam Marks",
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
        JPanel home = new JPanel(new BorderLayout(0, 10));
        home.setBackground(backgroundColor);
        home.setBorder(new EmptyBorder(18, 30, 18, 30));

        JLabel heading = new JLabel("Dashboard");
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JLabel welcome = new JLabel("Welcome back, Lecturer");
        welcome.setFont(new Font("Arial", Font.PLAIN, 13));
        welcome.setForeground(new Color(82, 103, 130));

        JPanel welcomePanel = new JPanel(new GridLayout(2, 1, 0, 8));
        welcomePanel.setOpaque(false);
        welcomePanel.add(heading);
        welcomePanel.add(welcome);

        JPanel cards = new JPanel(new GridLayout(2, 2, 14, 12));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 300));
        cards.setOpaque(false);

        addDashboardCard(cards, "▱", "Courses",
                "Manage materials", "Course Materials");

        addDashboardCard(cards, "☑", "Exam Marks",
                "Enter and review", "Upload Exam Marks");

        addDashboardCard(cards, "♧", "Students",
                "Academic details", "Undergraduate Details");

        addDashboardCard(cards, "▥", "Results",
                "Grades and GPA", "Marks, Grades and GPA");

        JPanel quickAccess = new JPanel(new BorderLayout(0, 10));
        quickAccess.setBackground(Color.WHITE);
        quickAccess.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel quickTitle = new JLabel("Quick Access");
        quickTitle.setFont(new Font("Arial", Font.BOLD, 16));
        quickTitle.setForeground(navy);

        JPanel quickButtons = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0));
        quickButtons.setOpaque(false);

        addQuickButton(quickButtons, "Exam Eligibility");
        addQuickButton(quickButtons, "Attendance and Medical Records");
        addQuickButton(quickButtons, "Notices");

        quickAccess.add(quickTitle, BorderLayout.NORTH);
        quickAccess.add(quickButtons, BorderLayout.CENTER);


        JPanel lower = new JPanel();
        lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
        lower.setOpaque(false);

        JPanel cardsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardsWrapper.setPreferredSize(new Dimension(650, 260));        cardsWrapper.setOpaque(false);
        cardsWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsWrapper.setMaximumSize(new Dimension(650, 260));
        cards.setPreferredSize(new Dimension(650, 260));
        cardsWrapper.add(cards);

        quickAccess.setPreferredSize(new Dimension(650, 75));
        quickAccess.setMaximumSize(new Dimension(650, 75));
        quickAccess.setAlignmentX(Component.LEFT_ALIGNMENT);

        lower.add(cardsWrapper);
        lower.add(Box.createVerticalStrut(12));
        lower.add(quickAccess);

        home.add(welcomePanel, BorderLayout.NORTH);
        home.add(lower, BorderLayout.CENTER);



        return home;
    }

    private void addDashboardCard(JPanel panel, String icon,
                                  String title, String description,
                                  String destination) {
        JButton card = new JButton();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setForeground(navy);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setFocusPainted(false);
        card.setHorizontalAlignment(SwingConstants.LEFT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(
                new MenuIcon(
                        title.equals("Courses") ? "book" :
                                title.equals("Exam Marks") ? "check" :
                                        title.equals("Students") ? "students" : "chart",
                        new Color(38, 100, 165)
                )
        );  iconLabel.setFont(new Font("Arial", Font.BOLD, 21));
        iconLabel.setForeground(new Color(38, 100, 165));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(navy);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descriptionLabel.setForeground(new Color(82, 103, 130));


        JPanel cardContent = new JPanel();
        cardContent.setLayout(
                new BoxLayout(cardContent, BoxLayout.Y_AXIS)
        );
        cardContent.setOpaque(false);

        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardContent.add(iconLabel);
        cardContent.add(Box.createVerticalStrut(8));
        cardContent.add(titleLabel);
        cardContent.add(Box.createVerticalStrut(5));
        cardContent.add(descriptionLabel);

        card.add(cardContent, BorderLayout.CENTER);


        card.addActionListener(e -> {
            selectMenuByName(destination);
            showSection(destination);
        });

        panel.add(card);
    }

    private void addQuickButton(JPanel panel, String destination) {
        JButton button = new JButton(destination);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setForeground(new Color(82, 103, 130));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            selectMenuByName(destination);
            showSection(destination);
        });

        panel.add(button);
    }

    private JPanel createPlaceholder(String section) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel(section);
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(navy);

        JLabel message = new JLabel(
                "The " + section + " section is ready for development.");
        message.setFont(new Font("Arial", Font.PLAIN, 14));
        message.setForeground(textColor);

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 12));
        content.setOpaque(false);
        content.add(heading);
        content.add(message);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void selectMenuByName(String name) {
        for (Component component : sidebar.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;

                if (button.getText().endsWith(name)) {
                    selectMenuButton(button);
                    return;
                }
            }
        }
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
