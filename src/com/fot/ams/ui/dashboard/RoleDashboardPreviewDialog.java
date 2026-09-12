package com.fot.ams.ui.dashboard;

import com.fot.ams.model.User;
import com.fot.ams.service.SessionManager;
import com.fot.ams.ui.components.GradientButton;
import com.fot.ams.ui.components.RoundedPanel;
import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

/**
 * RoleDashboardPreviewDialog represents the role-specific landing screen
 * displayed upon successful authentication per FR-AUTH-03.
 */
public class RoleDashboardPreviewDialog extends JDialog {

    public RoleDashboardPreviewDialog(JFrame parent, User user) {
        super(parent, "Dashboard - " + user.getDashboardTitle(), true);
        setSize(650, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(ThemeColors.WINDOW_BG);
        setLayout(new BorderLayout(16, 16));

        JPanel container = new JPanel(new BorderLayout(16, 16));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Top Header Card
        RoundedPanel headerCard = new RoundedPanel(18, Color.WHITE, ThemeColors.CARD_BORDER, 1);
        headerCard.setLayout(new BorderLayout(16, 12));
        headerCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel userMetaPanel = new JPanel();
        userMetaPanel.setOpaque(false);
        userMetaPanel.setLayout(new BoxLayout(userMetaPanel, BoxLayout.Y_AXIS));

        JLabel welcomeLabel = new JLabel("Welcome back, " + user.getFullName());
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setForeground(ThemeColors.TEXT_HEADLINE);

        JLabel roleLabel = new JLabel("Role: " + user.getRole().getDisplayName() + "  |  " + user.getRoleSpecificIdentifier());
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(ThemeColors.BUTTON_GRADIENT_START);

        String sessionTime = SessionManager.getInstance().getLoginTime() != null
                ? SessionManager.getInstance().getLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "Active";
        JLabel sessionLabel = new JLabel("Session Initiated: " + sessionTime + "  |  Dept: " + user.getDepartmentID());
        sessionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sessionLabel.setForeground(ThemeColors.TEXT_MUTED);

        userMetaPanel.add(welcomeLabel);
        userMetaPanel.add(Box.createVerticalStrut(4));
        userMetaPanel.add(roleLabel);
        userMetaPanel.add(Box.createVerticalStrut(4));
        userMetaPanel.add(sessionLabel);

        headerCard.add(userMetaPanel, BorderLayout.CENTER);
        container.add(headerCard, BorderLayout.NORTH);

        // Center Content Card - Authorized Modules
        RoundedPanel modulesCard = new RoundedPanel(18, Color.WHITE, ThemeColors.CARD_BORDER, 1);
        modulesCard.setLayout(new BoxLayout(modulesCard, BoxLayout.Y_AXIS));
        modulesCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel modulesTitle = new JLabel("Authorized Functions & Permissions (SRS Section 3)");
        modulesTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        modulesTitle.setForeground(ThemeColors.TEXT_HEADLINE);
        modulesCard.add(modulesTitle);
        modulesCard.add(Box.createVerticalStrut(12));

        for (String perm : user.getPermissions()) {
            JPanel permRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            permRow.setOpaque(false);

            JLabel dotLabel = new JLabel("●");
            dotLabel.setForeground(ThemeColors.BUTTON_GRADIENT_START);
            dotLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));

            JLabel permText = new JLabel(perm);
            permText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            permText.setForeground(ThemeColors.TEXT_LABEL);

            permRow.add(dotLabel);
            permRow.add(permText);
            modulesCard.add(permRow);
        }

        container.add(modulesCard, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel footerBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footerBar.setOpaque(false);

        JLabel infoText = new JLabel("University of Ruhuna • Faculty of Technology");
        infoText.setFont(ThemeColors.FONT_SMALL);
        infoText.setForeground(ThemeColors.TEXT_MUTED);

        GradientButton logoutBtn = new GradientButton("Secure Logout");
        logoutBtn.setPreferredSize(new Dimension(140, 38));
        logoutBtn.addActionListener(e -> {
            SessionManager.getInstance().endSession();
            dispose();
        });

        footerBar.add(infoText);
        footerBar.add(Box.createHorizontalStrut(10));
        footerBar.add(logoutBtn);

        container.add(footerBar, BorderLayout.SOUTH);
        add(container);
    }
}
