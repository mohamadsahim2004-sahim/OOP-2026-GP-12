package com.facultyams.ui;

import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.components.ModernButton;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

public class LoginFrame extends JFrame {

    public LoginFrame() {
        setTitle("SRMS - Login Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));

        // Left Branding Banner
        JPanel leftBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 96, 242), getWidth(), getHeight(), new Color(20, 60, 190));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        leftBanner.setLayout(new GridBagLayout());

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        JLabel logoIcon = new JLabel(IconHelper.getIcon("graduation-cap", 56, Color.WHITE));
        logoIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoTitle = new JLabel("SRMS");
        logoTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logoTitle.setForeground(Color.WHITE);
        logoTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoSub = new JLabel("Student Result Management System");
        logoSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logoSub.setForeground(new Color(224, 235, 255));
        logoSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel quote = new JLabel("<html><center>Empowering Academic Excellence & Student Success</center></html>");
        quote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        quote.setForeground(new Color(191, 219, 254));
        quote.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandContent.add(logoIcon);
        brandContent.add(Box.createVerticalStrut(10));
        brandContent.add(logoTitle);
        brandContent.add(Box.createVerticalStrut(4));
        brandContent.add(logoSub);
        brandContent.add(Box.createVerticalStrut(20));
        brandContent.add(quote);

        leftBanner.add(brandContent);

        // Right Login Form
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(Theme.BG_MAIN);

        CardPanel card = new CardPanel(16);
        card.setPreferredSize(new Dimension(360, 420));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.createEmptyPadding(28, 28, 28, 28));

        JLabel welcomeLbl = new JLabel("Welcome Back! \uD83D\uDC4B");
        welcomeLbl.setFont(Theme.FONT_TITLE);
        welcomeLbl.setForeground(Theme.TEXT_MAIN);

        JLabel instructionLbl = new JLabel("Sign in to your undergraduate account");
        instructionLbl.setFont(Theme.FONT_BODY);
        instructionLbl.setForeground(Theme.TEXT_MUTED);

        JTextField userField = new JTextField("abhishek");
        userField.setFont(Theme.FONT_BODY);
        userField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPasswordField passField = new JPasswordField("password123");
        passField.setFont(Theme.FONT_BODY);
        passField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Undergraduate Student", "Lecturer", "Admin", "Technical Officer"});
        roleBox.setFont(Theme.FONT_BODY);

        ModernButton loginBtn = new ModernButton("Sign In to Portal", ModernButton.StyleType.PRIMARY);
        loginBtn.setPreferredSize(new Dimension(300, 42));
        loginBtn.setMaximumSize(new Dimension(300, 42));
        loginBtn.addActionListener(e -> {
            dispose();
            UndergraduateDashboardFrame dashboard = new UndergraduateDashboardFrame();
            dashboard.setVisible(true);
        });

        card.add(welcomeLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(instructionLbl);
        card.add(Box.createVerticalStrut(18));

        card.add(createFieldLabel("Username / Student ID"));
        card.add(Box.createVerticalStrut(4));
        card.add(userField);
        card.add(Box.createVerticalStrut(12));

        card.add(createFieldLabel("Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(passField);
        card.add(Box.createVerticalStrut(12));

        card.add(createFieldLabel("Select Role"));
        card.add(Box.createVerticalStrut(4));
        card.add(roleBox);
        card.add(Box.createVerticalStrut(20));

        card.add(loginBtn);

        rightPanel.add(card);

        root.add(leftBanner);
        root.add(rightPanel);
        setContentPane(root);
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame lf = new LoginFrame();
            lf.setVisible(true);
        });
    }
}
