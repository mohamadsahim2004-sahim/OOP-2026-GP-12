package com.fot.ams.ui;

import com.fot.ams.exception.AuthenticationException;
import com.fot.ams.exception.DatabaseException;
import com.fot.ams.model.User;
import com.fot.ams.service.AuthService;
import com.fot.ams.ui.components.GradientButton;
import com.fot.ams.ui.components.GradientHeroPanel;
import com.fot.ams.ui.components.ModernPasswordField;
import com.fot.ams.ui.components.ModernTextField;
import com.fot.ams.ui.components.RoleBadgeButton;
import com.fot.ams.ui.components.RoundedPanel;
import com.fot.ams.ui.dashboard.RoleDashboardPreviewDialog;
import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * LoginFrame provides the primary login graphical user interface,
 * modeled after the modern glass/gradient card design in the reference image
 * and satisfying all SRS authentication requirements (FR-AUTH-01 to FR-AUTH-06).
 */
public class LoginFrame extends JFrame {
    private final AuthService authService;

    // Form inputs
    private ModernTextField usernameField;
    private ModernPasswordField passwordField;
    private GradientButton loginButton;
    private JPanel errorBanner;
    private JLabel errorLabel;

    public LoginFrame() {
        super("Faculty of Technology Academic Management System - University of Ruhuna");
        this.authService = new AuthService();

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 620);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        // Outer container with ambient background
        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(ThemeColors.WINDOW_BG);

        // Centered Main Card with smooth rounded corners
        RoundedPanel mainCard = new RoundedPanel(ThemeColors.CARD_CORNER_RADIUS, ThemeColors.CARD_BG, ThemeColors.CARD_BORDER, 1);
        mainCard.setPreferredSize(new Dimension(890, 540));
        mainCard.setLayout(new BorderLayout(14, 0));
        mainCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. Left Side: Gradient Hero Card
        GradientHeroPanel heroPanel = new GradientHeroPanel(22);
        heroPanel.setPreferredSize(new Dimension(380, 516));
        mainCard.add(heroPanel, BorderLayout.WEST);

        // 2. Right Side: Clean Modern Form Panel
        JPanel formContainer = createFormPanel();
        mainCard.add(formContainer, BorderLayout.CENTER);

        outerPanel.add(mainCard);
        setContentPane(outerPanel);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 36, 20, 36));

        // 1. Top Logo Emblem: Small Violet Asterisk
        JPanel topLogoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientHeroPanel.drawAsterisk(g2, 10, 10, 8, ThemeColors.BUTTON_GRADIENT_START, 2.5f);
                g2.dispose();
            }
        };
        topLogoPanel.setOpaque(false);
        topLogoPanel.setPreferredSize(new Dimension(100, 22));
        topLogoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        panel.add(topLogoPanel);
        panel.add(Box.createVerticalStrut(10));

        // 2. Headline
        JLabel titleLabel = new JLabel("Sign in to your account");
        titleLabel.setFont(ThemeColors.FONT_HEADLINE);
        titleLabel.setForeground(ThemeColors.TEXT_HEADLINE);
        titleLabel.setAlignmentX(0.0f);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(6));

        // 3. Subtitle matching reference style
        JLabel subtitleLabel = new JLabel("<html><body style='width: 380px; font-family: Segoe UI, sans-serif; font-size: 12px; color: rgb(100, 116, 139); line-height: 1.35; margin: 0;'>"
                + "Access your tasks, courses, and academic records anytime, anywhere — and keep everything flowing in one place.</body></html>");
        subtitleLabel.setPreferredSize(new Dimension(380, 34));
        subtitleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        subtitleLabel.setAlignmentX(0.0f);
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(14));

        // 4. Error Message Alert Banner (Hidden by default, shown on invalid login)
        errorBanner = new RoundedPanel(10, ThemeColors.ERROR_BG, ThemeColors.ERROR_BORDER, 1);
        errorBanner.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        errorBanner.setAlignmentX(0.0f);
        errorBanner.setVisible(false);

        JPanel errorIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeColors.ERROR_TEXT);
                g2.fillOval(1, 1, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("!", 6, 12);
                g2.dispose();
            }
        };
        errorIcon.setOpaque(false);
        errorIcon.setPreferredSize(new Dimension(16, 16));

        errorLabel = new JLabel("");
        errorLabel.setFont(ThemeColors.FONT_SMALL);
        errorLabel.setForeground(ThemeColors.ERROR_TEXT);

        errorBanner.add(errorIcon);
        errorBanner.add(errorLabel);
        panel.add(errorBanner);
        panel.add(Box.createVerticalStrut(10));

        // 5. Input Field 1: Username / ID
        JLabel userLabel = new JLabel("Your user ID or username");
        userLabel.setFont(ThemeColors.FONT_LABEL);
        userLabel.setForeground(ThemeColors.TEXT_LABEL);
        userLabel.setAlignmentX(0.0f);
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(6));

        usernameField = new ModernTextField("e.g. TG/2024/2105 or admin");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setPreferredSize(new Dimension(380, 40));
        usernameField.setAlignmentX(0.0f);
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocusInWindow();
                }
            }
        });
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(14));

        // 6. Input Field 2: Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(ThemeColors.FONT_LABEL);
        passLabel.setForeground(ThemeColors.TEXT_LABEL);
        passLabel.setAlignmentX(0.0f);
        panel.add(passLabel);
        panel.add(Box.createVerticalStrut(6));

        passwordField = new ModernPasswordField("••••••••••••");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setPreferredSize(new Dimension(380, 40));
        passwordField.setAlignmentX(0.0f);
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(20));

        // 7. Action Button: Get Started / Sign In
        loginButton = new GradientButton("Get Started");
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setPreferredSize(new Dimension(380, 44));
        loginButton.setAlignmentX(0.0f);
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(16));

        // 8. Divider: "── or continue with ──" (matching image)
        JPanel dividerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                String dividerText = "or quick demo role";
                g2.setFont(ThemeColors.FONT_SMALL);
                int textW = g2.getFontMetrics().stringWidth(dividerText);
                int textX = (w - textW) / 2;
                int textY = (h - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();

                g2.setColor(ThemeColors.INPUT_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawLine(20, h / 2, textX - 10, h / 2);
                g2.drawLine(textX + textW + 10, h / 2, w - 20, h / 2);

                g2.setColor(ThemeColors.TEXT_MUTED);
                g2.drawString(dividerText, textX, textY);
                g2.dispose();
            }
        };
        dividerPanel.setOpaque(false);
        dividerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        dividerPanel.setPreferredSize(new Dimension(380, 20));
        dividerPanel.setAlignmentX(0.0f);
        panel.add(dividerPanel);
        panel.add(Box.createVerticalStrut(12));

        // 9. Demo Role Badges with equal balanced widths
        JPanel roleBadgesPanel = new JPanel(new GridLayout(1, 4, 8, 0));
        roleBadgesPanel.setOpaque(false);
        roleBadgesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        roleBadgesPanel.setPreferredSize(new Dimension(380, 32));
        roleBadgesPanel.setAlignmentX(0.0f);

        RoleBadgeButton adminBtn = new RoleBadgeButton("Admin");
        adminBtn.addActionListener(e -> fillCredentials("admin", "admin123"));

        RoleBadgeButton lecBtn = new RoleBadgeButton("Lecturer");
        lecBtn.addActionListener(e -> fillCredentials("lec_ict01", "lec123"));

        RoleBadgeButton toBtn = new RoleBadgeButton("Tech Officer");
        toBtn.addActionListener(e -> fillCredentials("to_ict01", "to123"));

        RoleBadgeButton studentBtn = new RoleBadgeButton("Student");
        studentBtn.addActionListener(e -> fillCredentials("TG/2024/2105", "student123"));

        roleBadgesPanel.add(adminBtn);
        roleBadgesPanel.add(lecBtn);
        roleBadgesPanel.add(toBtn);
        roleBadgesPanel.add(studentBtn);
        panel.add(roleBadgesPanel);
        panel.add(Box.createVerticalStrut(14));

        // 10. Sign Up Link matching reference image ("Don't have an account? Sign up")
        JPanel signupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        signupPanel.setOpaque(false);
        signupPanel.setAlignmentX(0.0f);

        JLabel signupPrompt = new JLabel("Don't have an account? ");
        signupPrompt.setFont(ThemeColors.FONT_SMALL);
        signupPrompt.setForeground(ThemeColors.TEXT_MUTED);

        JLabel signupLink = new JLabel("Sign up");
        signupLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        signupLink.setForeground(ThemeColors.BUTTON_GRADIENT_START);
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegistrationScreen();
            }
        });

        signupPanel.add(signupPrompt);
        signupPanel.add(signupLink);
        panel.add(signupPanel);
        panel.add(Box.createVerticalStrut(4));

        // 11. Footer note (FR-AUTH-06: Passwords reset only by Admin)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(0.0f);

        JLabel footPrompt = new JLabel("Forgot password? ");
        footPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footPrompt.setForeground(ThemeColors.TEXT_MUTED);

        JLabel footLink = new JLabel("Contact Administrator");
        footLink.setFont(new Font("Segoe UI", Font.BOLD, 10));
        footLink.setForeground(ThemeColors.TEXT_MUTED);
        footLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showAdminContactInfo();
            }
        });

        footerPanel.add(footPrompt);
        footerPanel.add(footLink);
        panel.add(footerPanel);

        return panel;
    }

    public void prefillUsername(String username) {
        clearError();
        usernameField.setText(username);
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    private void openRegistrationScreen() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            RegistrationFrame regFrame = new RegistrationFrame();
            regFrame.setVisible(true);
        });
    }

    private void fillCredentials(String username, String password) {
        clearError();
        usernameField.setText(username);
        passwordField.setText(password);
        passwordField.requestFocusInWindow();
    }

    private void handleLogin() {
        clearError();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User authenticatedUser = authService.login(username, password);

            // Successful authentication -> redirect to role dashboard (FR-AUTH-03)
            showRoleDashboard(authenticatedUser);

        } catch (AuthenticationException ex) {
            showError(ex.getMessage());
        } catch (DatabaseException ex) {
            showError("System error: Unable to verify credentials. Please try again.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearError() {
        errorBanner.setVisible(false);
        revalidate();
        repaint();
    }

    private void showRoleDashboard(User user) {
        // Clear fields for security
        passwordField.setText("");

        // Open role-specific dashboard dialog/view
        RoleDashboardPreviewDialog dialog = new RoleDashboardPreviewDialog(this, user);
        dialog.setVisible(true);
    }

    private void showAdminContactInfo() {
        JOptionPane.showMessageDialog(
            this,
            "Per SRS Requirement FR-AUTH-06:\n"
            + "Only System Administrators may reset account passwords.\n\n"
            + "Contact:\n"
            + "• Department: Dept. of ICT, Faculty of Technology, University of Ruhuna\n"
            + "• Admin Email: admin@fot.ruh.ac.lk\n"
            + "• Phone: +94 41 222 3344",
            "Password Reset Policy",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
}
