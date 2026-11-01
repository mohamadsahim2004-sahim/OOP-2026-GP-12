package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Admin;
import com.facultyams.model.Department;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Student;
import com.facultyams.model.TechnicalOfficer;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Add / Edit user form, shown in a dialog. It only collects values; {@link UserService} validates them
 * (Admin-only, unique username / e-mail / registration number, password rules) and saves them. Fields that
 * belong to one role only (designation, registration number ...) are enabled just for that role.
 */
final class UserFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient UserService userService;
    /** null when adding. */
    private final transient User existing;

    final JTextField usernameField = new JTextField(14);
    final JPasswordField passwordField = new JPasswordField(14);
    final JTextField fullNameField = new JTextField(14);
    final JTextField emailField = new JTextField(14);
    final JTextField contactField = new JTextField(14);
    final JTextField pictureField = new JTextField(10);
    final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
    final JComboBox<Department> departmentCombo = new JComboBox<>();
    final JCheckBox activeCheck = new JCheckBox("Account is active", true);
    final JTextField designationField = new JTextField("Lecturer", 14);
    final JTextField registrationField = new JTextField(14);
    final JTextField batchField = new JTextField(14);
    final JComboBox<Student.StudentStatus> statusCombo = new JComboBox<>(Student.StudentStatus.values());

    UserFormPanel(UserService userService, DepartmentService departmentService, User existing)
            throws DatabaseException, AuthorizationException {
        super(new BorderLayout());
        this.userService = userService;
        this.existing = existing;
        setOpaque(false);

        departmentCombo.addItem(null);
        for (Department d : departmentService.getAllDepartments()) {
            departmentCombo.addItem(d);
        }
        departmentCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? "-- None --" : value, index,
                        isSelected, cellHasFocus);
            }
        });
        roleCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                        value instanceof Role r ? r.getDisplayName() : value, index, isSelected, cellHasFocus);
            }
        });
        roleCombo.setSelectedItem(Role.STUDENT);
        roleCombo.addActionListener(e -> updateRoleFields());

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> choosePicture());
        JPanel picture = new JPanel(new BorderLayout(6, 0));
        picture.setOpaque(false);
        picture.add(pictureField, BorderLayout.CENTER);
        picture.add(browse, BorderLayout.EAST);

        JPanel form = Forms.grid();
        Forms.add(form, 0, 0, 1, "Username *", usernameField);
        Forms.add(form, 1, 0, 1, existing == null ? "Password *" : "Password (use Reset password)", passwordField);
        Forms.add(form, 0, 1, 1, "Full name *", fullNameField);
        Forms.add(form, 1, 1, 1, "Email *", emailField);
        Forms.add(form, 0, 2, 1, "Role *", roleCombo);
        Forms.add(form, 1, 2, 1, "Department", departmentCombo);
        Forms.add(form, 0, 3, 1, "Contact number", contactField);
        Forms.add(form, 1, 3, 1, "Profile picture", picture);
        Forms.add(form, 0, 4, 1, "Designation (lecturer)", designationField);
        Forms.add(form, 1, 4, 1, "Registration no (student)", registrationField);
        Forms.add(form, 0, 5, 1, "Batch (student)", batchField);
        Forms.add(form, 1, 5, 1, "Student status", statusCombo);
        Forms.add(form, 0, 6, 2, "Account", activeCheck);
        add(form, BorderLayout.CENTER);

        if (existing != null) {
            fill(existing);
        }
        updateRoleFields();
    }

    private void fill(User u) {
        usernameField.setText(u.getUsername());
        passwordField.setEnabled(false);
        fullNameField.setText(u.getFullName());
        emailField.setText(u.getEmail());
        contactField.setText(u.getContactNumber() == null ? "" : u.getContactNumber());
        pictureField.setText(u.getProfilePicture() == null ? "" : u.getProfilePicture());
        roleCombo.setSelectedItem(u.getRole());
        for (int i = 0; i < departmentCombo.getItemCount(); i++) {
            Department d = departmentCombo.getItemAt(i);
            if (d != null && u.getDepartmentId() != null && d.getDepartmentId() == u.getDepartmentId()) {
                departmentCombo.setSelectedIndex(i);
            }
        }
        activeCheck.setSelected(u.isActive());
        if (u instanceof Lecturer l) {
            designationField.setText(l.getDesignation());
        }
        if (u instanceof Student s) {
            registrationField.setText(s.getRegistrationNumber());
            batchField.setText(s.getBatch());
            statusCombo.setSelectedItem(s.getStudentStatus());
        }
    }

    /** Enables only the fields that apply to the selected role. */
    private void updateRoleFields() {
        Role role = (Role) roleCombo.getSelectedItem();
        boolean student = role == Role.STUDENT;
        designationField.setEnabled(role == Role.LECTURER);
        registrationField.setEnabled(student);
        batchField.setEnabled(student);
        statusCombo.setEnabled(student);
    }

    private void choosePicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose profile picture");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pictureField.setText(chooser.getSelectedFile().getName());
        }
    }

    /** Builds the correct User subclass for the selected role from the form values. */
    private User readForm() {
        Role role = (Role) roleCombo.getSelectedItem();
        User user;
        switch (role) {
            case ADMIN -> user = new Admin();
            case LECTURER -> {
                Lecturer lecturer = new Lecturer();
                lecturer.setDesignation(designationField.getText());
                user = lecturer;
            }
            case TECHNICAL_OFFICER -> user = new TechnicalOfficer();
            default -> {
                Student student = new Student();
                student.setRegistrationNumber(registrationField.getText());
                student.setBatch(batchField.getText());
                student.setStudentStatus((Student.StudentStatus) statusCombo.getSelectedItem());
                user = student;
            }
        }
        user.setUsername(usernameField.getText());
        user.setFullName(fullNameField.getText());
        user.setEmail(emailField.getText());
        user.setContactNumber(contactField.getText());
        user.setProfilePicture(pictureField.getText());
        Department d = (Department) departmentCombo.getSelectedItem();
        user.setDepartmentId(d == null ? null : d.getDepartmentId());
        user.setActive(activeCheck.isSelected());
        return user;
    }

    /** Saves through the service; returns false (after showing the message) if it was refused. */
    boolean save() {
        try {
            User user = readForm();
            if (existing == null) {
                userService.createUser(user, new String(passwordField.getPassword()));
            } else {
                if (existing.getRole() != roleCombo.getSelectedItem() && !UIUtil.confirm(this,
                        "Change the role of " + existing.getUsername() + " from " + existing.getRole().getDisplayName()
                                + " to " + ((Role) roleCombo.getSelectedItem()).getDisplayName() + "?")) {
                    return false;
                }
                user.setUserId(existing.getUserId());
                userService.updateUser(user);
            }
            return true;
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
            return false;
        } finally {
            passwordField.setText("");
        }
    }

    /** @return true if a user was saved */
    static boolean showDialog(Component parent, UserService userService, DepartmentService departmentService, User existing) {
        UserFormPanel form;
        try {
            form = new UserFormPanel(userService, departmentService, existing);
        } catch (DatabaseException | AuthorizationException e) {
            UIUtil.showException(parent, e);
            return false;
        }
        return FormDialog.show(parent, existing == null ? "Add User" : "Edit User " + existing.getUsername(), form,
                existing == null ? "Add user" : "Save changes", form::save);
    }
}