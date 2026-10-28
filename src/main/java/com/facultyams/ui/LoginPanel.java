package com.facultyams.ui;

import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.service.AuthService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.function.Consumer;

/** Login form: a white card on a navy-blue background. Authenticates against the database. */
public class LoginPanel extends JPanel {

    private final AuthService authService = new AuthService();
    private final Consumer<User> onSuccess;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");

    /** @param onSuccess called with the logged-in user (the session has already been started) */
    public LoginPanel(Consumer<User> onSuccess) {
        super(new GridBagLayout());
        this.onSuccess = onSuccess;

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        JLabel title = new JLabel("<html><div style='text-align:center'>Faculty Academic<br>Management System"
                + "</div></html>", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(AdminTheme.NAVY);
        JLabel subtitle = new JLabel("Login to Continue", SwingConstants.CENTER);
        subtitle.setFont(AdminTheme.FONT_BOLD);
        subtitle.setForeground(AdminTheme.TEXT_MUTED);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(2, 0, 2, 0);
        c.gridy = 0;
        card.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(2, 0, 18, 0);
        card.add(subtitle, c);
        c.insets = new Insets(8, 0, 2, 0);
        c.gridy = 2;
        card.add(label("Username"), c);
        c.insets = new Insets(0, 0, 8, 0);
        c.gridy = 3;
        card.add(field(usernameField), c);
        c.insets = new Insets(8, 0, 2, 0);
        c.gridy = 4;
        card.add(label("Password"), c);
        c.insets = new Insets(0, 0, 14, 0);
        c.gridy = 5;
        card.add(field(passwordField), c);

        AdminTheme.primary(loginButton);
        loginButton.setPreferredSize(new Dimension(100, 38));
        c.gridy = 6;
        c.insets = new Insets(4, 0, 8, 0);
        card.add(loginButton, c);

        JButton forgot = new JButton("Forgot password?");
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setForeground(AdminTheme.BLUE);
        forgot.setFont(AdminTheme.FONT);
        forgot.setFocusPainted(false);
        forgot.addActionListener(e -> UIUtil.showInfo(this,
                "Passwords are reset by the system administrator.\nPlease contact the Faculty IT office."));
        c.gridy = 7;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(forgot, c);

        JLabel footer = new JLabel("\u00A9 2026 Faculty of Technology - University of Ruhuna", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(AdminTheme.TEXT_MUTED);
        c.gridy = 8;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(footer, c);

        add(card, new GridBagConstraints());

        loginButton.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> performLogin());
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AdminTheme.FONT_BOLD);
        l.setForeground(AdminTheme.TEXT);
        return l;
    }

    private static JTextField field(JTextField f) {
        f.setFont(AdminTheme.FONT);
        f.setPreferredSize(new Dimension(260, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminTheme.BORDER), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        return f;
    }

    /** Validates the form, authenticates against the database and reports any problem to the user. */
    private void performLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        loginButton.setEnabled(false);
        try {
            User user = authService.login(username, new String(password));
            passwordField.setText("");
            onSuccess.accept(user);
        } catch (ValidationException | AuthenticationException | DatabaseException e) {
            passwordField.setText("");
            UIUtil.showException(this, e);
        } finally {
            java.util.Arrays.fill(password, '\0');
            loginButton.setEnabled(true);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setPaint(new GradientPaint(0, 0, AdminTheme.NAVY_DARK, getWidth(), getHeight(), AdminTheme.BLUE));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}