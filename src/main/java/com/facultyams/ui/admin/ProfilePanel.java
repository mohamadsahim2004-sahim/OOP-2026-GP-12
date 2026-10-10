package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.util.Arrays;

/**
 * The logged-in administrator's own profile. Only the fields that
 * User.getEditableProfileFields() allows can be changed; username, role and
 * department are shown read-only (an Admin changes those via User Management).
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private final UserService userService = new UserService();
    private final Runnable onProfileChanged;

    private final JLabel photoLabel = new JLabel("No photo", SwingConstants.CENTER);
    private final JTextField nameField = new JTextField(24);
    private final JTextField usernameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JTextField contactField = new JTextField(24);
    private final JTextField roleField = new JTextField(24);
    private final JTextField departmentField = new JTextField(24);
    private final JPasswordField newPasswordField = new JPasswordField(24);
    private final JPasswordField confirmPasswordField = new JPasswordField(24);

    private String profilePicture;

    /** @param onProfileChanged called after a successful update so the header can show the new name */
    public ProfilePanel(Runnable onProfileChanged) {
        super(new BorderLayout(30, 0));
        this.onProfileChanged = onProfileChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (JTextField readOnly : Arrays.asList(usernameField, roleField, departmentField)) {
            readOnly.setEditable(false);
            readOnly.setBackground(AdminTheme.PANEL_BG);
        }

        photoLabel.setPreferredSize(new java.awt.Dimension(130, 130));
        photoLabel.setBorder(BorderFactory.createLineBorder(AdminTheme.BORDER));
        photoLabel.setOpaque(true);
        photoLabel.setBackground(AdminTheme.BLUE_LIGHT);
        JButton photoButton = new JButton("Change Photo");
        photoButton.addActionListener(e -> choosePhoto());
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.add(photoLabel, BorderLayout.NORTH);
        left.add(photoButton, BorderLayout.CENTER);
        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.add(left, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        addRow(form, row++, "Name", nameField);
        addRow(form, row++, "Username", usernameField);
        addRow(form, row++, "Email", emailField);
        addRow(form, row++, "Contact number", contactField);
        addRow(form, row++, "Role", roleField);
        addRow(form, row++, "Department", departmentField);

        JButton updateButton = new JButton("Update Profile");
        updateButton.addActionListener(e -> updateProfile());

        JLabel pwTitle = new JLabel("Change password");
        pwTitle.setFont(AdminTheme.SECTION);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(18, 5, 4, 5);
        form.add(pwTitle, c);
        addRow(form, row++, "New password", newPasswordField);
        addRow(form, row++, "Confirm password", confirmPasswordField);
        JButton passwordButton = new JButton("Change Password");
        passwordButton.addActionListener(e -> changePassword());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(updateButton);
        buttons.add(passwordButton);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(14, 0, 0, 0);
        form.add(buttons, c);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.add(form, BorderLayout.NORTH);

        add(leftWrap, BorderLayout.WEST);
        add(formWrap, BorderLayout.CENTER);
        AdminTheme.apply(this);
    }

    private static void addRow(JPanel form, int row, String label, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 12);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(field, c);
    }

    @Override
    public void refresh() {
        User me = Session.getCurrentUser();
        if (me == null) {
            return;
        }
        nameField.setText(me.getFullName());
        usernameField.setText(me.getUsername());
        emailField.setText(me.getEmail());
        contactField.setText(me.getContactNumber() == null ? "" : me.getContactNumber());
        roleField.setText(me.getRole().getDisplayName());
        departmentField.setText(me.getDepartmentName() == null ? "-" : me.getDepartmentName());
        profilePicture = me.getProfilePicture();
        showPhoto();
        newPasswordField.setText("");
        confirmPasswordField.setText("");
    }

    private void showPhoto() {
        photoLabel.setIcon(null);
        photoLabel.setText("No photo");
        if (profilePicture == null || profilePicture.isBlank()) {
            return;
        }
        File file = new File(profilePicture);
        if (!file.isFile()) {
            photoLabel.setText("Photo not found");
            return;
        }
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        if (icon.getIconWidth() <= 0) {
            photoLabel.setText("Not an image");
            return;
        }
        photoLabel.setText("");
        photoLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH)));
    }

    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, gif)", "png", "jpg", "jpeg", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            profilePicture = chooser.getSelectedFile().getAbsolutePath();
            showPhoto();
            UIUtil.showInfo(this, "Photo selected. Click \"Update Profile\" to save it.");
        }
    }

    private void updateProfile() {
        try {
            userService.updateOwnProfile(nameField.getText(), emailField.getText(),
                    contactField.getText(), profilePicture);
            UIUtil.showInfo(this, "Profile updated.");
            refresh();
            onProfileChanged.run();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void changePassword() {
        String newPassword = new String(newPasswordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());
        try {
            if (!newPassword.equals(confirm)) {
                throw new ValidationException("The two passwords do not match.");
            }
            userService.resetPassword(Session.getCurrentUser().getUserId(), newPassword);
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            UIUtil.showInfo(this, "Password changed. Use the new password next time you log in.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}