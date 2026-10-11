package com.facultyams.ui.views;

import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

public class StudentProfileView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField emergencyField;

    public StudentProfileView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Profile Body: Left Overview Card + Right Details Form
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left card (width ~30%)
        gbc.gridx = 0;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(createLeftProfileCard(), gbc);

        // Right details form (width ~70%)
        gbc.gridx = 1;
        gbc.weightx = 0.68;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(createDetailsFormCard(), gbc);

        content.add(body);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Student Profile & Academic Records");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("View your registered student credentials, faculty advisor, and update contact information.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        card.add(left, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLeftProfileCard() {
        Student s = dataService.getCurrentStudent();
        CardPanel card = new CardPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.createEmptyPadding(24, 20, 24, 20));

        // Avatar circle
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), new Color(99, 102, 241));
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String initials = "AV";
                int tx = (getWidth() - fm.stringWidth(initials)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(84, 84));
        avatar.setMaximumSize(new Dimension(84, 84));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel(s.getFullName(), JLabel.CENTER);
        nameLbl.setFont(Theme.FONT_TITLE);
        nameLbl.setForeground(Theme.TEXT_MAIN);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel regLbl = new JLabel(s.getRegNo(), JLabel.CENTER);
        regLbl.setFont(Theme.FONT_BODY_BOLD);
        regLbl.setForeground(Theme.PRIMARY);
        regLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel degLbl = new JLabel(s.getDegreeProgram(), JLabel.CENTER);
        degLbl.setFont(Theme.FONT_BODY);
        degLbl.setForeground(Theme.TEXT_MUTED);
        degLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        BadgeLabel activeBadge = BadgeLabel.createStatusBadge("Active Student");
        activeBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Academic snapshot list
        JPanel snapshot = new JPanel(new GridLayout(3, 1, 0, 10));
        snapshot.setOpaque(false);
        snapshot.setBorder(Theme.createEmptyPadding(18, 8, 18, 8));

        snapshot.add(createMiniStat("Current CGPA", String.format("%.2f", s.getCgpa())));
        snapshot.add(createMiniStat("Department", s.getDepartment()));
        snapshot.add(createMiniStat("Academic Advisor", s.getAcademicAdvisor()));

        card.add(avatar);
        card.add(Box.createVerticalStrut(12));
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(regLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(degLbl);
        card.add(Box.createVerticalStrut(10));
        card.add(activeBadge);
        card.add(Box.createVerticalStrut(16));
        card.add(snapshot);

        return card;
    }

    private JPanel createMiniStat(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(Theme.FONT_BODY_BOLD);
        v.setForeground(Theme.TEXT_MAIN);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel createDetailsFormCard() {
        Student s = dataService.getCurrentStudent();
        CardPanel card = new CardPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JLabel title = new JLabel("Contact & Personal Information");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title);
        card.add(Box.createVerticalStrut(16));

        // Form Fields (2 columns layout)
        JPanel formGrid = new JPanel(new GridLayout(4, 2, 18, 14));
        formGrid.setOpaque(false);

        // University Email (Read-only)
        formGrid.add(createFieldItem("University Email", s.getEmail(), false, null));

        // Personal Email (Editable)
        emailField = new JTextField("abhishek.personal@gmail.com");
        formGrid.add(createFieldItem("Personal Email", null, true, emailField));

        // Phone Number (Editable)
        phoneField = new JTextField(s.getPhoneNumber());
        formGrid.add(createFieldItem("Mobile Number", null, true, phoneField));

        // Emergency Contact (Editable)
        emergencyField = new JTextField(s.getEmergencyContact());
        formGrid.add(createFieldItem("Emergency Contact", null, true, emergencyField));

        // Residential Address (Editable)
        addressField = new JTextField(s.getAddress());
        formGrid.add(createFieldItem("Residential Address", null, true, addressField));

        // Academic Program (Read-only)
        formGrid.add(createFieldItem("Degree Program", s.getDegreeProgram(), false, null));

        // Current Level (Read-only)
        formGrid.add(createFieldItem("Academic Year & Level", "Level 3 - Semester 5", false, null));

        // Department (Read-only)
        formGrid.add(createFieldItem("Faculty / Department", s.getDepartment(), false, null));

        card.add(formGrid);
        card.add(Box.createVerticalStrut(20));

        // Save Button Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        ModernButton saveBtn = new ModernButton("Save Changes", ModernButton.StyleType.PRIMARY);
        saveBtn.addActionListener(e -> {
            dataService.updateStudentProfile(
                    emailField.getText(),
                    phoneField.getText(),
                    addressField.getText(),
                    emergencyField.getText()
            );
            JOptionPane.showMessageDialog(this,
                    "Student profile updated successfully!",
                    "Profile Updated", JOptionPane.INFORMATION_MESSAGE);
        });

        btnRow.add(saveBtn);
        card.add(btnRow);

        return card;
    }

    private JPanel createFieldItem(String label, String value, boolean editable, JTextField customField) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);

        JComponent field;
        if (editable) {
            customField.setFont(Theme.FONT_BODY);
            customField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));
            field = customField;
        } else {
            JTextField ro = new JTextField(value);
            ro.setFont(Theme.FONT_BODY);
            ro.setEditable(false);
            ro.setBackground(new Color(248, 250, 252));
            ro.setForeground(Theme.TEXT_MAIN);
            ro.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));
            field = ro;
        }

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }
}
