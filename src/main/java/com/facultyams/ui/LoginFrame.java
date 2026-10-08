package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;

    private final AuthService authService;

    public LoginFrame() {

        authService = new AuthService();

        setTitle(
                "Faculty Academic Management System - Login"
        );

        setSize(450, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        JPanel mainPanel =
                new JPanel(new BorderLayout(15, 15));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        25, 35, 25, 35
                )
        );

        JLabel title =
                new JLabel(
                        "Faculty Academic Management System",
                        SwingConstants.CENTER
                );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        mainPanel.add(
                title,
                BorderLayout.NORTH
        );

        JPanel form =
                new JPanel(
                        new GridLayout(
                                3,
                                2,
                                10,
                                15
                        )
                );

        form.add(new JLabel("Username:"));

        usernameField =
                new JTextField();

        form.add(usernameField);

        form.add(new JLabel("Password:"));

        passwordField =
                new JPasswordField();

        form.add(passwordField);

        loginButton =
                new JButton("Login");

        JButton exitButton =
                new JButton("Exit");

        form.add(loginButton);
        form.add(exitButton);

        mainPanel.add(
                form,
                BorderLayout.CENTER
        );

        setContentPane(mainPanel);

        loginButton.addActionListener(
                e -> performLogin()
        );

        exitButton.addActionListener(
                e -> System.exit(0)
        );

        getRootPane().setDefaultButton(
                loginButton
        );
    }

    private void performLogin() {

        String username =
                usernameField.getText().trim();

        String password =
                new String(
                        passwordField.getPassword()
                );

        try {

            User user =
                    authService.login(
                            username,
                            password
                    );

            JOptionPane.showMessageDialog(
                    this,
                    "Login successful.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

            DashboardRouter.openDashboard(user);

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}