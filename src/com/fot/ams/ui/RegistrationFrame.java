package com.fot.ams.ui;

import com.fot.ams.exception.AuthenticationException;
import com.fot.ams.exception.DatabaseException;
import com.fot.ams.model.*;
import com.fot.ams.service.AuthService;
import com.fot.ams.ui.components.*;
import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RegistrationFrame provides a symmetrically aligned registration interface
 * with equal spacing and balanced 2-column grid layout across all academic roles.
 */
public class RegistrationFrame extends JFrame {
    private final AuthService authService;

    // Common fields
    private ModernTextField fullNameField;
    private ModernTextField emailField;
    private ModernTextField phoneField;
    private ModernPasswordField passwordField;
    private ModernPasswordField confirmPasswordField;

    // Role-specific fields
    private RoleTabSelector roleTabSelector;
    private CardLayout roleCardLayout;
    private JPanel roleCardPanel;

    // Undergraduate specific
    private ModernTextField ugIndexField;
    private ModernComboBox<String> ugBatchCombo;
    private ModernComboBox<String> ugStatusCombo;

    // Lecturer specific
    private ModernTextField lecUsernameField;
    private ModernComboBox<String> lecDesignationCombo;
    private ModernTextField lecSpecField;

    // Technical Officer specific
    private ModernTextField toUsernameField;
    private ModernComboBox<String> toDeptCombo;
    private ModernTextField toLabField;

    // Admin specific
    private ModernTextField adminUsernameField;
    private ModernTextField adminDeptField;
    private ModernTextField adminScopeField;

    // Feedback
    private JPanel errorBanner;
    private JLabel errorLabel;

    public RegistrationFrame() {
        super("Create an Account - Faculty of Technology Academic Management System");
        this.authService = new AuthService();

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(ThemeColors.WINDOW_BG);

        RoundedPanel mainCard = new RoundedPanel(ThemeColors.CARD_CORNER_RADIUS, ThemeColors.CARD_BG, ThemeColors.CARD_BORDER, 1);
        mainCard.setPreferredSize(new Dimension(950, 600));
        mainCard.setLayout(new BorderLayout(16, 0));
        mainCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. Left Hero Banner (Matching reference design)
        GradientHeroPanel heroPanel = new GradientHeroPanel(22);
        heroPanel.setPreferredSize(new Dimension(380, 576));
        mainCard.add(heroPanel, BorderLayout.WEST);

        // 2. Right Registration Form Panel (Equal spacing & balanced alignment)
        JPanel formContainer = createRegistrationFormPanel();
        mainCard.add(formContainer, BorderLayout.CENTER);

        outerPanel.add(mainCard);
        setContentPane(outerPanel);
    }

    private JPanel createRegistrationFormPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));

        // 1. Top Asterisk Logo
        JPanel topLogoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientHeroPanel.drawAsterisk(g2, 8, 8, 8, ThemeColors.BUTTON_GRADIENT_START, 2.5f);
                g2.dispose();
            }
        };
        topLogoPanel.setOpaque(false);
        topLogoPanel.setPreferredSize(new Dimension(500, 18));
        topLogoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        topLogoPanel.setAlignmentX(0.0f);
        panel.add(topLogoPanel);
        panel.add(Box.createVerticalStrut(6));

        // 2. Heading "Create an account"
        JLabel titleLabel = new JLabel("Create an account");
        titleLabel.setFont(ThemeColors.FONT_HEADLINE);
        titleLabel.setForeground(ThemeColors.TEXT_HEADLINE);
        titleLabel.setAlignmentX(0.0f);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));

        // 3. Subtitle with perfectly formatted text
        JLabel subtitleLabel = new JLabel("<html><div style='width: 470px; font-family: Segoe UI, sans-serif; font-size: 11.5px; color: rgb(100, 116, 139); line-height: 1.35;'>"
                + "Access your tasks, notes, and academic records anytime, anywhere — and keep everything flowing in one place.</div></html>");
        subtitleLabel.setPreferredSize(new Dimension(480, 32));
        subtitleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        subtitleLabel.setAlignmentX(0.0f);
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(12));

        // 4. Role Selector Tabs (Undergraduate | Lecturer | Tech Officer | Admin)
        roleTabSelector = new RoleTabSelector(this::onRoleSwitched);
        roleTabSelector.setAlignmentX(0.0f);
        panel.add(roleTabSelector);
        panel.add(Box.createVerticalStrut(8));

        // 5. Error Alert Banner
        errorBanner = new RoundedPanel(10, ThemeColors.ERROR_BG, ThemeColors.ERROR_BORDER, 1);
        errorBanner.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
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
        panel.add(Box.createVerticalStrut(6));

        // 6. Form Container: Symmetrically aligned with 2 equal columns per row
        JPanel formGrid = new JPanel();
        formGrid.setOpaque(false);
        formGrid.setLayout(new BoxLayout(formGrid, BoxLayout.Y_AXIS));
        formGrid.setAlignmentX(0.0f);

        // Row 1: Full Name | Email Address
        JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row1.add(createFieldColumn("Full Name", fullNameField = new ModernTextField("e.g. Mohamad Shahim")));
        row1.add(createFieldColumn("Email Address", emailField = new ModernTextField("e.g. name@fot.ruh.ac.lk")));
        formGrid.add(row1);
        formGrid.add(Box.createVerticalStrut(10));

        // Dynamic Role Panel (Row 2 & Row 3 per role, each 2 columns)
        roleCardLayout = new CardLayout();
        roleCardPanel = new JPanel(roleCardLayout);
        roleCardPanel.setOpaque(false);
        roleCardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));

        roleCardPanel.add(createUndergraduateCard(), UserRole.UNDERGRADUATE.name());
        roleCardPanel.add(createLecturerCard(), UserRole.LECTURER.name());
        roleCardPanel.add(createTechnicalOfficerCard(), UserRole.TECHNICAL_OFFICER.name());
        roleCardPanel.add(createAdminCard(), UserRole.ADMIN.name());
        formGrid.add(roleCardPanel);
        formGrid.add(Box.createVerticalStrut(10));

        // Row 4: Password | Confirm Password
        JPanel row4 = new JPanel(new GridLayout(1, 2, 14, 0));
        row4.setOpaque(false);
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row4.add(createFieldColumn("Password", passwordField = new ModernPasswordField("••••••••")));
        row4.add(createFieldColumn("Confirm Password", confirmPasswordField = new ModernPasswordField("••••••••")));
        formGrid.add(row4);

        panel.add(formGrid);
        panel.add(Box.createVerticalStrut(18));

        // 7. Primary Action Button: Create Account (Matches full grid width)
        GradientButton registerBtn = new GradientButton("Create Account");
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        registerBtn.setPreferredSize(new Dimension(480, 42));
        registerBtn.setAlignmentX(0.0f);
        registerBtn.addActionListener(e -> handleRegister());
        panel.add(registerBtn);
        panel.add(Box.createVerticalStrut(12));

        // 8. Footer link: "Already have an account? Sign in"
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(0.0f);

        JLabel footPrompt = new JLabel("Already have an account? ");
        footPrompt.setFont(ThemeColors.FONT_SMALL);
        footPrompt.setForeground(ThemeColors.TEXT_MUTED);

        JLabel footLink = new JLabel("Sign in");
        footLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footLink.setForeground(ThemeColors.BUTTON_GRADIENT_START);
        footLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLoginScreen("");
            }
        });

        footerPanel.add(footPrompt);
        footerPanel.add(footLink);
        panel.add(footerPanel);

        return panel;
    }

    private JPanel createFieldColumn(String labelText, javax.swing.JComponent inputComp) {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText);
        label.setFont(ThemeColors.FONT_LABEL);
        label.setForeground(ThemeColors.TEXT_LABEL);
        label.setAlignmentX(0.0f);

        inputComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        inputComp.setPreferredSize(new Dimension(230, 38));
        inputComp.setAlignmentX(0.0f);

        col.add(label);
        col.add(Box.createVerticalStrut(4));
        col.add(inputComp);
        return col;
    }

    // Role 1: Undergraduate (Row A: Index & Batch | Row B: Status & Phone)
    private JPanel createUndergraduateCard() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel rA = new JPanel(new GridLayout(1, 2, 14, 0));
        rA.setOpaque(false);
        rA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        ugIndexField = new ModernTextField("e.g. TG/2024/2105");
        ugBatchCombo = new ModernComboBox<>(new String[]{"B09 (Current Level II)", "B08", "B07", "B06"});
        rA.add(createFieldColumn("Student Index No", ugIndexField));
        rA.add(createFieldColumn("Academic Batch", ugBatchCombo));
        container.add(rA);
        container.add(Box.createVerticalStrut(10));

        JPanel rB = new JPanel(new GridLayout(1, 2, 14, 0));
        rB.setOpaque(false);
        rB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        ugStatusCombo = new ModernComboBox<>(new String[]{"Active", "Repeat", "Batch-missed"});
        phoneField = new ModernTextField("e.g. +94 76 111 2222");
        rB.add(createFieldColumn("Enrollment Status", ugStatusCombo));
        rB.add(createFieldColumn("Phone Number", phoneField));
        container.add(rB);

        return container;
    }

    // Role 2: Lecturer (Row A: Staff ID & Designation | Row B: Specialisation & Phone)
    private JPanel createLecturerCard() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel rA = new JPanel(new GridLayout(1, 2, 14, 0));
        rA.setOpaque(false);
        rA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        lecUsernameField = new ModernTextField("e.g. lec_perera");
        lecDesignationCombo = new ModernComboBox<>(new String[]{
            "Senior Lecturer (Gr. I)",
            "Senior Lecturer (Gr. II)",
            "Lecturer",
            "Probationary Lecturer"
        });
        rA.add(createFieldColumn("Staff ID / Username", lecUsernameField));
        rA.add(createFieldColumn("Academic Designation", lecDesignationCombo));
        container.add(rA);
        container.add(Box.createVerticalStrut(10));

        JPanel rB = new JPanel(new GridLayout(1, 2, 14, 0));
        rB.setOpaque(false);
        rB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        lecSpecField = new ModernTextField("e.g. Software Engineering");
        ModernTextField lecPhone = new ModernTextField("e.g. +94 77 123 4567");
        rB.add(createFieldColumn("Academic Specialisation", lecSpecField));
        rB.add(createFieldColumn("Contact Phone", lecPhone));
        container.add(rB);

        return container;
    }

    // Role 3: Technical Officer (Row A: Officer ID & Department | Row B: Assigned Lab & Phone)
    private JPanel createTechnicalOfficerCard() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel rA = new JPanel(new GridLayout(1, 2, 14, 0));
        rA.setOpaque(false);
        rA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        toUsernameField = new ModernTextField("e.g. to_fernando");
        toDeptCombo = new ModernComboBox<>(new String[]{"DICT (Information & Comm Tech)", "DCE (Civil Tech)", "DET (Engineering Tech)"});
        rA.add(createFieldColumn("Officer ID / Username", toUsernameField));
        rA.add(createFieldColumn("Assigned Department", toDeptCombo));
        container.add(rA);
        container.add(Box.createVerticalStrut(10));

        JPanel rB = new JPanel(new GridLayout(1, 2, 14, 0));
        rB.setOpaque(false);
        rB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        toLabField = new ModernTextField("e.g. Computer Network Lab");
        ModernTextField toPhone = new ModernTextField("e.g. +94 71 987 6543");
        rB.add(createFieldColumn("Assigned Laboratory / Facility", toLabField));
        rB.add(createFieldColumn("Contact Phone", toPhone));
        container.add(rB);

        return container;
    }

    // Role 4: Administrator (Row A: Admin ID & Department | Row B: Access Scope & Phone)
    private JPanel createAdminCard() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel rA = new JPanel(new GridLayout(1, 2, 14, 0));
        rA.setOpaque(false);
        rA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        adminUsernameField = new ModernTextField("e.g. admin_ruhuna");
        adminDeptField = new ModernTextField("DICT (Faculty Office)");
        adminDeptField.setEnabled(false);
        rA.add(createFieldColumn("Admin Username", adminUsernameField));
        rA.add(createFieldColumn("Department Scope", adminDeptField));
        container.add(rA);
        container.add(Box.createVerticalStrut(10));

        JPanel rB = new JPanel(new GridLayout(1, 2, 14, 0));
        rB.setOpaque(false);
        rB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        adminScopeField = new ModernTextField("Full Academic Management CRUD");
        adminScopeField.setEnabled(false);
        ModernTextField adminPhone = new ModernTextField("e.g. +94 41 222 3344");
        rB.add(createFieldColumn("Access Privileges", adminScopeField));
        rB.add(createFieldColumn("Contact Phone", adminPhone));
        container.add(rB);

        return container;
    }

    private void onRoleSwitched(UserRole role) {
        clearError();
        roleCardLayout.show(roleCardPanel, role.name());
        revalidate();
        repaint();
    }

    private void handleRegister() {
        clearError();
        UserRole selectedRole = roleTabSelector.getSelectedRole();

        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String phone = phoneField != null ? phoneField.getText() : "";
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        User newUser;
        switch (selectedRole) {
            case UNDERGRADUATE: {
                String index = ugIndexField.getText();
                String rawBatch = (String) ugBatchCombo.getSelectedItem();
                String batch = rawBatch != null && rawBatch.contains(" ") ? rawBatch.substring(0, rawBatch.indexOf(' ')) : rawBatch;
                String status = (String) ugStatusCombo.getSelectedItem();
                newUser = new Undergraduate(0, index, "", fullName, email, phone, "DICT", index, batch, status, 2024);
                break;
            }
            case LECTURER: {
                String username = lecUsernameField.getText();
                String desig = (String) lecDesignationCombo.getSelectedItem();
                String spec = lecSpecField != null ? lecSpecField.getText() : "Computer Technology";
                newUser = new Lecturer(0, username, "", fullName, email, phone, "DICT", desig, spec);
                break;
            }
            case TECHNICAL_OFFICER: {
                String username = toUsernameField.getText();
                newUser = new TechnicalOfficer(0, username, "", fullName, email, phone, "DICT");
                break;
            }
            case ADMIN:
            default: {
                String username = adminUsernameField.getText();
                newUser = new Admin(0, username, "", fullName, email, phone, "DICT");
                break;
            }
        }

        try {
            authService.register(newUser, password, confirmPassword);

            JOptionPane.showMessageDialog(
                this,
                "Account registered successfully for " + newUser.getFullName() + "!\n"
                + "Role: " + newUser.getRole().getDisplayName() + "\n"
                + "Username / ID: " + newUser.getUsername() + "\n\n"
                + "You can now sign in with your credentials.",
                "Registration Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            // Navigate back to Login with username prefilled
            openLoginScreen(newUser.getUsername());

        } catch (AuthenticationException ex) {
            showError(ex.getMessage());
        } catch (DatabaseException ex) {
            showError("Database error during registration: " + ex.getMessage());
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

    private void openLoginScreen(String prefillUsername) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            if (prefillUsername != null && !prefillUsername.isEmpty()) {
                loginFrame.prefillUsername(prefillUsername);
            }
            loginFrame.setVisible(true);
        });
    }
}
