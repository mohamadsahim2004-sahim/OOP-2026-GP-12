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

        add(welcomeLabel, BorderLayout.NORTH);
        JPanel sidebar = new JPanel();  //create panel for a sidebar
        sidebar.setLayout(new GridLayout(4,2,15,15));    // arrange te buttons vertically
                                         //(4 rows, 2 column, horizontal gap,vertical gap

        String[] menuItems = { //an array to store the menu names
            "Lecturer Profile",
            "Course Materials",
            "Upload Exam Marks",
            "Undergraduate Details",
            "Exam Eligibility",
            "Marks / Grades /GPA",
            "attendance / Medical",
            "Notices"
        };
        for (String item : menuItems){ // creating buttons using loop
            JButton button = new JButton(item);
            sidebar.add(button);

        }

        sidebar.setBorder(BorderFactory.createEmptyBorder(30,40,30,40));
        add(sidebar,BorderLayout.CENTER);

        JButton backButton = new JButton("Back");//back button for return to admin
        backButton.setFont(new Font("Arial",Font.BOLD,16));
        backButton.setBackground(new Color(52,73,94));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setPreferredSize(new Dimension(150,40));
        backButton.addActionListener(e-> {
            dispose();//to close lecturer dashboard window
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(backButton);

        add(bottomPanel,BorderLayout.SOUTH);

    }

    
        });
    }
}
