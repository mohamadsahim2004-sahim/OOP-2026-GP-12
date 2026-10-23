package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.model.User.ProfileField;
import com.facultyams.security.Role;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.util.Arrays;

/**
 * The logged-in user's own profile. What can be edited is decided by {@link User#canEditOwn}, not by this
 * screen: name, email, contact number and photo for an Admin; username, role and department are always
 * read-only (an Admin changes those in User Management). The details are re-read from the database every
 * time the page is shown, so an edit made elsewhere (for example in User Management) is never overwritten
 * with stale values. Changing the password needs the current password.
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient UserService userService;
    private final transient Runnable onProfileChanged;

    private final JLabel photoLabel = new JLabel("No photo", SwingConstants.CENTER);
    private final JTextField nameField = new JTextField(24);
    private final JTextField usernameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JTextField contactField = new JTextField(24);
    private final JTextField roleField = new JTextField(24);
    private final JTextField departmentField = new JTextField(24);
    private final JPasswordField currentPasswordField = new JPasswordField(24);
    private final JPasswordField newPasswordField = new JPasswordField(24);
    private final JPasswordField confirmPasswordField = new JPasswordField(24);
    private final JButton photoButton = new JButton("Change Photo");
    private final JButton updateButton = new JButton("Update Profile");
    private final JButton passwordButton = new JButton("Change Password");
    private final JTextArea loadError = AdminTheme.errorText();

    private String profilePicture;

    /** @param onProfileChanged called after a successful update so the header can show the new name */
    public ProfilePanel(Runnable onProfileChanged) {
        this(onProfileChanged, null);
    }

    /**
     * @param onLogout what the Logout button does (the shell's confirm-then-logout); when null the page has
     *                 no Logout button, because a visible button must always work
     */
    public ProfilePanel(Runnable onProfileChanged, Runnable onLogout) {
        this(onProfileChanged, onLogout, new UserService());
    }

    public ProfilePanel(Runnable onProfileChanged, Runnable onLogout, UserService userService) {
        super(new BorderLayout(30, 0));
        this.userService = userService;
        this.onProfileChanged = onProfileChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        name(nameField, "profile.name");
        name(usernameField, "profile.username");
        name(emailField, "profile.email");
        name(contactField, "profile.contact");
        name(roleField, "profile.role");
        name(departmentField, "profile.department");
        name(currentPasswordField, "profile.currentPassword");
        name(newPasswordField, "profile.newPassword");
        name(confirmPasswordField, "profile.confirmPassword");
        photoButton.setName("profile.photo");
        updateButton.setName("profile.update");
        passwordButton.setName("profile.changePassword");
        loadError.setName("profile.error");
        loadError.setVisible(false);

        for (JTextField readOnly : Arrays.asList(usernameField, roleField, departmentField)) {
            setEditableLook(readOnly, false);
        }

        photoLabel.setPreferredSize(new Dimension(130, 130));
        photoLabel.setBorder(BorderFactory.createLineBorder(AdminTheme.BORDER));
        photoLabel.setOpaque(true);
        photoLabel.setBackground(AdminTheme.BLUE_LIGHT);
        photoButton.addActionListener(e -> choosePhoto());
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.add(photoLabel, BorderLayout.NORTH);
        left.add(photoButton, BorderLayout.CENTER);
        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.add(left, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 5, 6, 5);
        form.add(loadError, c);
        addRow(form, row++, "Name", nameField);
        addRow(form, row++, "Username", usernameField);
        addRow(form, row++, "Email", emailField);
        addRow(form, row++, "Contact number", contactField);
        addRow(form, row++, "Role", roleField);
        addRow(form, row++, "Department", departmentField);

        updateButton.addActionListener(e -> updateProfile());
        JPanel profileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        profileButtons.add(updateButton);
        addWide(form, row++, profileButtons, new Insets(10, 5, 0, 5));

        JLabel pwTitle = new JLabel("Change password");
        pwTitle.setFont(AdminTheme.SECTION);
        addWide(form, row++, pwTitle, new Insets(22, 5, 4, 5));
        addRow(form, row++, "Current password", currentPasswordField);
        addRow(form, row++, "New password", newPasswordField);
        addRow(form, row++, "Confirm password", confirmPasswordField);
        passwordButton.addActionListener(e -> changePassword());
        JPanel passwordButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        passwordButtons.add(passwordButton);
        addWide(form, row++, passwordButtons, new Insets(10, 5, 0, 5));

        if (onLogout != null) {
            JButton logoutButton = new JButton("Logout");
            logoutButton.setName("profile.logout");
            AdminTheme.secondary(logoutButton);
            logoutButton.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            logoutButton.addActionListener(e -> onLogout.run());
            JLabel sessionTitle = new JLabel("Session");
            sessionTitle.setFont(AdminTheme.SECTION);
            addWide(form, row++, sessionTitle, new Insets(22, 5, 4, 5));
            JPanel logoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            logoutRow.add(logoutButton);
            addWide(form, row, logoutRow, new Insets(6, 5, 0, 5));
        }

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.add(form, BorderLayout.NORTH);

        add(leftWrap, BorderLayout.WEST);
        add(formWrap, BorderLayout.CENTER);
        AdminTheme.apply(this);
    }

    private static void name(JTextComponent field, String name) {
        field.setName(name);
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

    private static void addWide(JPanel form, int row, java.awt.Component component, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = insets;
        form.add(component, c);
    }

    private static void setEditableLook(JTextField field, boolean editable) {
        field.setEditable(editable);
        field.setBackground(editable ? java.awt.Color.WHITE : AdminTheme.PANEL_BG);
    }

    @Override
    public void refresh() {
        User session = Session.getCurrentUser();
        if (session == null) {
            return; // logged out: nothing to show
        }
        User me = session;
        loadError.setText("");
        loadError.setVisible(false);
        try {
            me = userService.getUserById(session.getUserId());
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            loadError.setText("Showing the details from when you logged in; they could not be refreshed. "
                    + e.getMessage());
            loadError.setVisible(true);
        }

        nameField.setText(me.getFullName());
        usernameField.setText(me.getUsername());
        emailField.setText(me.getEmail());
        contactField.setText(me.getContactNumber() == null ? "" : me.getContactNumber());
        roleField.setText(me.getRole().getDisplayName());
        departmentField.setText(me.getDepartmentName() == null ? "-" : me.getDepartmentName());
        profilePicture = me.getProfilePicture();
        showPhoto();

        // editable exactly where the model allows it for this user
        setEditableLook(nameField, me.canEditOwn(ProfileField.FULL_NAME));
        setEditableLook(emailField, me.canEditOwn(ProfileField.EMAIL));
        setEditableLook(contactField, me.canEditOwn(ProfileField.CONTACT_NUMBER));
        photoButton.setEnabled(me.canEditOwn(ProfileField.PROFILE_PICTURE));
        updateButton.setEnabled(me.getEditableProfileFields().size() > 0);

        boolean mayChangePassword = me.getRole() == Role.ADMIN; // other roles never change passwords themselves
        for (JPasswordField f : Arrays.asList(currentPasswordField, newPasswordField, confirmPasswordField)) {
            f.setText("");
            f.setEnabled(mayChangePassword);
        }
        passwordButton.setEnabled(mayChangePassword);
        revalidate();
        repaint();
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
        char[] current = currentPasswordField.getPassword();
        char[] next = newPasswordField.getPassword();
        char[] confirm = confirmPasswordField.getPassword();
        try {
            if (!Arrays.equals(next, confirm)) {
                throw new ValidationException("The two passwords do not match.");
            }
            userService.changeOwnPassword(new String(current), new String(next));
            for (JPasswordField f : Arrays.asList(currentPasswordField, newPasswordField, confirmPasswordField)) {
                f.setText("");
            }
            UIUtil.showInfo(this, "Password changed. Use the new password next time you log in.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        } finally {
            Arrays.fill(current, '\0');
            Arrays.fill(next, '\0');
            Arrays.fill(confirm, '\0');
        }
    }
}
