package com.facultyams.ui;

import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.service.AuthService;
import com.facultyams.ui.admin.AdminIcons;
import com.facultyams.ui.admin.AdminIcons.Kind;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Login screen: a welcome panel on the left and the sign-in form on the right (the form alone on narrow
 * windows). Credentials are checked against the database by {@link AuthService} on a background thread, so the
 * window stays responsive; problems are shown inline under the form.
 */
public class LoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int FORM_WIDTH = 340;
    /** Below this width the welcome panel is hidden so the form always fits (small / tablet windows). */
    private static final int HERO_MIN_WIDTH = 760;

    private final transient AuthService authService = new AuthService();
    private final transient Consumer<User> onSuccess;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");
    private final JTextArea errorText = AdminTheme.errorText();
    private final JPanel hero = new Hero();

    /** @param onSuccess called with the logged-in user (the session has already been started) */
    public LoginPanel(Consumer<User> onSuccess) {
        super(new GridBagLayout());
        this.onSuccess = onSuccess;
        setOpaque(true);
        setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.46;
        add(hero, c);
        c.gridx = 1;
        c.weightx = 0.54;
        add(buildForm(), c);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean show = getWidth() >= HERO_MIN_WIDTH;
                if (hero.isVisible() != show) {
                    hero.setVisible(show);
                    revalidate();
                }
            }
        });
        loginButton.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> performLogin());
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(FORM_WIDTH, 460));

        JLabel badge = new JLabel(AdminIcons.of(Kind.CAP, 30, AdminTheme.BLUE), JLabel.CENTER);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Welcome Back");
        title.setFont(AdminTheme.TITLE);
        title.setForeground(AdminTheme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = AdminTheme.muted("Enter your credentials to continue");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        errorText.setName("login.error");
        errorText.setVisible(false);
        errorText.getAccessibleContext().setAccessibleName("Login error");

        AdminTheme.primary(loginButton);
        loginButton.setFont(AdminTheme.FONT_BOLD.deriveFont(14f));
        loginButton.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton forgot = new JButton("Forgot Password?");
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setForeground(AdminTheme.BLUE);
        forgot.setFont(AdminTheme.FONT);
        forgot.setFocusPainted(false);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        forgot.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgot.addActionListener(e -> UIUtil.showInfo(this,
                "Passwords are reset by the system administrator.\nPlease contact the Faculty IT office."));

        JLabel footer = AdminTheme.muted("\u00A9 2026 Faculty of Technology - University of Ruhuna");
        footer.setFont(AdminTheme.FONT_SMALL);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton eye = new JButton(AdminIcons.of(Kind.EYE, 18, AdminTheme.TEXT_MUTED));
        eye.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 2));
        eye.setContentAreaFilled(false);
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        eye.getAccessibleContext().setAccessibleName("Show password");
        eye.setToolTipText("Show password");
        char hidden = passwordField.getEchoChar();
        eye.addActionListener(e -> {
            boolean show = passwordField.getEchoChar() != 0;
            passwordField.setEchoChar(show ? (char) 0 : hidden);
            eye.setIcon(AdminIcons.of(show ? Kind.EYE_OFF : Kind.EYE, 18, AdminTheme.TEXT_MUTED));
            eye.setToolTipText(show ? "Hide password" : "Show password");
            eye.getAccessibleContext().setAccessibleName(show ? "Hide password" : "Show password");
        });

        form.add(gap(8));
        form.add(badge);
        form.add(gap(14));
        form.add(title);
        form.add(gap(4));
        form.add(subtitle);
        form.add(gap(26));
        form.add(fullWidth(label("Username", usernameField), true));
        form.add(gap(6));
        form.add(new Input(Kind.PROFILE, usernameField, null));
        form.add(gap(14));
        form.add(fullWidth(label("Password", passwordField), true));
        form.add(gap(6));
        form.add(new Input(Kind.LOCK, passwordField, eye));
        form.add(gap(10));
        form.add(fullWidth(errorText, false));
        form.add(gap(10));
        form.add(loginButton);
        form.add(gap(10));
        form.add(forgot);
        form.add(Box.createVerticalGlue());
        form.add(footer);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(form);
        return wrap;
    }

    private static Component gap(int height) {
        return Box.createVerticalStrut(height);
    }

    /** A row that spans the whole form width (left-aligned when {@code left}), as tall as its content. */
    private static JPanel fullWidth(JComponent content, boolean left) {
        JPanel row = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.add(content, left ? BorderLayout.WEST : BorderLayout.CENTER);
        return row;
    }

    private static JLabel label(String text, JComponent target) {
        JLabel l = new JLabel(text);
        l.setFont(AdminTheme.FONT_BOLD);
        l.setForeground(AdminTheme.TEXT);
        l.setLabelFor(target);
        target.getAccessibleContext().setAccessibleName(text);
        return l;
    }

    // ------------------------------------------------------------------ sign-in

    /** Validates the form, authenticates against the database in the background and reports problems inline. */
    private void performLogin() {
        if (!loginButton.isEnabled()) {
            return;
        }
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        showError(null);
        setBusy(true);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                try {
                    return authService.login(username, new String(password));
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    User user = get();
                    passwordField.setText("");
                    onSuccess.accept(user);
                } catch (ExecutionException e) {
                    passwordField.setText("");
                    Throwable cause = e.getCause();
                    if (cause instanceof ValidationException || cause instanceof AuthenticationException) {
                        showError(cause.getMessage());
                    } else if (cause instanceof DatabaseException) {
                        System.err.println("Database error: " + cause.getMessage());
                        showError(cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        showError("Something went wrong while signing in. Please try again.");
                    }
                    (username.isEmpty() ? usernameField : passwordField).requestFocusInWindow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Signing in..." : "Login");
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
    }

    private void showError(String message) {
        errorText.setText(message == null ? "" : message);
        errorText.setVisible(message != null);
        errorText.getParent().revalidate();
    }

    // ------------------------------------------------------------------ pieces

    /** A rounded input row: leading icon, the text field, optional trailing button; blue ring while focused. */
    private static final class Input extends JPanel {
        private static final long serialVersionUID = 1L;

        Input(Kind icon, JTextField field, JComponent trailing) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
            setPreferredSize(new Dimension(FORM_WIDTH, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setOpaque(false);
            field.setFont(AdminTheme.FONT.deriveFont(14f));
            field.setForeground(AdminTheme.TEXT);
            field.setCaretColor(AdminTheme.TEXT);
            field.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            add(new JLabel(AdminIcons.of(icon, 18, AdminTheme.TEXT_MUTED)), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            if (trailing != null) {
                add(trailing, BorderLayout.EAST);
            }
            // keep the shell's own text field styling out of the way: it is painted by this row
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            field.setBackground(Color.WHITE);
            field.setDisabledTextColor(AdminTheme.TEXT_MUTED);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    field.requestFocusInWindow();
                }
            });
            this.focusTarget = field;
        }

        private final transient JTextField focusTarget;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focus = focusTarget.hasFocus();
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (focus) {
                g2.setColor(new Color(37, 99, 235, 60));
                g2.setStroke(new java.awt.BasicStroke(4f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(focus ? AdminTheme.BLUE : new Color(0xC9D4E5));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
        }
    }

    /** Navy welcome panel with a soft campus skyline drawn in code (no image files needed). */
    private static final class Hero extends JPanel {
        private static final long serialVersionUID = 1L;

        Hero() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(380, 400));
            setBorder(BorderFactory.createEmptyBorder(0, 40, 56, 32));
            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            JLabel welcome = new JLabel("Welcome Back!");
            welcome.setFont(new Font("SansSerif", Font.BOLD, 32));
            welcome.setForeground(Color.WHITE);
            JLabel name = new JLabel("Faculty Academic Management System");
            name.setFont(new Font("SansSerif", Font.BOLD, 15));
            name.setForeground(new Color(0x9CC0FF));
            JLabel about = new JLabel("<html>Log in to manage your faculty<br>and keep everything organized.</html>");
            about.setFont(AdminTheme.FONT.deriveFont(14f));
            about.setForeground(new Color(0xDCE6F7));
            for (JLabel l : new JLabel[] {welcome, name, about}) {
                l.setAlignmentX(0f);
            }
            text.add(welcome);
            text.add(Box.createVerticalStrut(8));
            text.add(name);
            text.add(Box.createVerticalStrut(14));
            text.add(about);
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(text, BorderLayout.SOUTH);
            add(bottom, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, AdminTheme.NAVY_DARK, w, h, new Color(0x1B4DB5)));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillOval(-w / 3, -h / 6, w, w);
            g2.fillOval(w / 2, h / 5, w, w);
            // skyline of campus buildings fading into the background
            g2.setColor(new Color(255, 255, 255, 20));
            int[][] blocks = {{0, 150, 70}, {60, 210, 90}, {160, 120, 60}, {230, 240, 110}, {350, 170, 80}};
            for (int[] b : blocks) {
                int bh = (int) (b[1] * h / 640.0 * 1.1);
                g2.fillRect(b[0] * w / 420, h - bh, b[2] * w / 420, bh);
            }
            AdminIcons.of(Kind.CAP, 54, new Color(255, 255, 255, 235)).paintIcon(this, g2, 40, 48);
            g2.dispose();
        }
    }
}