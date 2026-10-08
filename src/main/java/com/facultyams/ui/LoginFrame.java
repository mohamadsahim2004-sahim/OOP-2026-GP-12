package com.facultyams.ui;

import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.service.AuthService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Login screen. On success the user is routed to the dashboard for their role. */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");

    public LoginFrame() {
        super("FoT-AMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUi();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Faculty of Technology", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Academic Management System", SwingConstants.CENTER);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(title, c);
        c.gridy = 1;
        panel.add(subtitle, c);

        c.gridwidth = 1;
        c.gridy = 2;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        panel.add(usernameField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        panel.add(passwordField, c);

        JButton exitButton = new JButton("Exit");
        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(exitButton);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        panel.add(buttons, c);

        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        exitButton.addActionListener(e -> System.exit(0));
        getRootPane().setDefaultButton(loginButton);

        setContentPane(panel);
    }

    private void doLogin() {
        String username = usernameField.getText();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        java.util.Arrays.fill(passwordChars, '\0');
        try {
            User user = authService.login(username, password);
            dispose();
            DashboardRouter.openDashboard(user);
        } catch (ValidationException | AuthenticationException | DatabaseException e) {
            UIUtil.showException(this, e);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}