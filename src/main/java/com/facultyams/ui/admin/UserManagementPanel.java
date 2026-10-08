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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;

/**
 * Admin screen: create / view / update / deactivate / delete / search users and assign roles.
 */
public class UserManagementPanel extends AbstractManagementPanel<User> {

    private final UserService userService = new UserService();
    private final DepartmentService departmentService = new DepartmentService();

    // search bar
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<Role> roleFilter = new JComboBox<>();

    // common fields
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JTextField fullNameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField contactField = new JTextField(15);
    private final JTextField pictureField = new JTextField(15);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
    private final JComboBox<Department> departmentCombo = new JComboBox<>();
    private final JCheckBox activeCheck = new JCheckBox("Active", true);

    // role specific fields
    private final JTextField designationField = new JTextField(15);
    private final JTextField registrationField = new JTextField(15);
    private final JTextField batchField = new JTextField(8);
    private final JComboBox<Student.StudentStatus> statusCombo = new JComboBox<>(Student.StudentStatus.values());

    public UserManagementPanel() {
        super(new String[]{"ID", "Username", "Full name", "Role", "Email", "Contact", "Department",
                "Reg. no / Designation", "Status"});
        roleFilter.setRenderer(nullRenderer("All roles"));
        roleFilter.addItem(null);
        for (Role role : Role.values()) {
            roleFilter.addItem(role);
        }
        departmentCombo.setRenderer(nullRenderer("-- None --"));
        roleCombo.addActionListener(e -> updateRoleFields());

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.SOUTH);
        updateRoleFields();
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");
        bar.add(new JLabel("Name / username / email / reg. no:"));
        bar.add(searchField);
        bar.add(roleFilter);
        bar.add(searchButton);
        bar.add(showAllButton);
        searchButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        roleFilter.addActionListener(e -> refresh());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            roleFilter.setSelectedItem(null);
            refresh();
        });
        return bar;
    }

    private JPanel buildForm() {
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> choosePicture());
        JPanel picturePanel = new JPanel(new BorderLayout(4, 0));
        picturePanel.add(pictureField, BorderLayout.CENTER);
        picturePanel.add(browseButton, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Username:", usernameField);
        addField(form, 0, 1, "Password (new user):", passwordField);
        addField(form, 0, 2, "Role:", roleCombo);
        addField(form, 1, 0, "Full name:", fullNameField);
        addField(form, 1, 1, "Email:", emailField);
        addField(form, 1, 2, "Department:", departmentCombo);
        addField(form, 2, 0, "Contact no:", contactField);
        addField(form, 2, 1, "Profile picture:", picturePanel);
        addField(form, 2, 2, "Account:", activeCheck);
        addField(form, 3, 0, "Designation (lecturer):", designationField);
        addField(form, 3, 1, "Registration no (student):", registrationField);
        addField(form, 3, 2, "Batch (student):", batchField);
        addField(form, 4, 2, "Status (student):", statusCombo);

        JButton addButton = new JButton("Add user");
        JButton updateButton = new JButton("Update");
        JButton toggleActiveButton = new JButton("Activate / Deactivate");
        JButton resetPasswordButton = new JButton("Reset password");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        addButton.addActionListener(e -> addUser());
        updateButton.addActionListener(e -> updateUser());
        toggleActiveButton.addActionListener(e -> toggleActive());
        resetPasswordButton.addActionListener(e -> resetPassword());
        deleteButton.addActionListener(e -> deleteUser());
        clearButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(toggleActiveButton);
        buttons.add(resetPasswordButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createTitledBorder("User details"));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    /** Enables only the fields that apply to the selected role. */
    private void updateRoleFields() {
        Role role = (Role) roleCombo.getSelectedItem();
        boolean lecturer = role == Role.LECTURER;
        boolean student = role == Role.STUDENT;
        designationField.setEnabled(lecturer);
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

    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        departmentCombo.removeAllItems();
        departmentCombo.addItem(null);
        for (Department d : departmentService.getAllDepartments()) {
            departmentCombo.addItem(d);
        }
    }

    @Override
    protected List<User> loadItems() throws DatabaseException, AuthorizationException {
        return userService.searchUsers(searchField.getText(), (Role) roleFilter.getSelectedItem());
    }

    @Override
    protected Object[] toRow(User u) {
        String extra = "";
        if (u instanceof Student) {
            Student s = (Student) u;
            extra = s.getRegistrationNumber() + " (" + s.getStudentStatus() + ")";
        } else if (u instanceof Lecturer) {
            extra = ((Lecturer) u).getDesignation();
        }
        return new Object[]{u.getUserId(), u.getUsername(), u.getFullName(), u.getRole(), u.getEmail(),
                u.getContactNumber() == null ? "" : u.getContactNumber(),
                u.getDepartmentName() == null ? "" : u.getDepartmentName(),
                extra, u.isActive() ? "Active" : "Inactive"};
    }

    @Override
    protected void fillForm(User u) {
        usernameField.setText(u.getUsername());
        passwordField.setText("");
        passwordField.setEnabled(false); // use "Reset password" for existing users
        fullNameField.setText(u.getFullName());
        emailField.setText(u.getEmail());
        contactField.setText(u.getContactNumber() == null ? "" : u.getContactNumber());
        pictureField.setText(u.getProfilePicture() == null ? "" : u.getProfilePicture());
        roleCombo.setSelectedItem(u.getRole());
        selectDepartment(u.getDepartmentId());
        activeCheck.setSelected(u.isActive());

        designationField.setText(u instanceof Lecturer ? ((Lecturer) u).getDesignation() : "");
        if (u instanceof Student) {
            Student s = (Student) u;
            registrationField.setText(s.getRegistrationNumber());
            batchField.setText(s.getBatch());
            statusCombo.setSelectedItem(s.getStudentStatus());
        } else {
            registrationField.setText("");
            batchField.setText("");
            statusCombo.setSelectedItem(Student.StudentStatus.REGULAR);
        }
        updateRoleFields();
    }

    @Override
    protected void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        passwordField.setEnabled(true);
        fullNameField.setText("");
        emailField.setText("");
        contactField.setText("");
        pictureField.setText("");
        roleCombo.setSelectedItem(Role.STUDENT);
        departmentCombo.setSelectedItem(null);
        activeCheck.setSelected(true);
        designationField.setText("Lecturer");
        registrationField.setText("");
        batchField.setText("");
        statusCombo.setSelectedItem(Student.StudentStatus.REGULAR);
        updateRoleFields();
    }

    private void selectDepartment(Integer departmentId) {
        departmentCombo.setSelectedItem(null);
        if (departmentId == null) {
            return;
        }
        for (int i = 0; i < departmentCombo.getItemCount(); i++) {
            Department d = departmentCombo.getItemAt(i);
            if (d != null && d.getDepartmentId() == departmentId) {
                departmentCombo.setSelectedIndex(i);
                return;
            }
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

    private void addUser() {
        if (getSelectedItem() != null) {
            UIUtil.showInfo(this, "A user is selected. Click 'Clear' first to enter a new user.");
            return;
        }
        try {
            User created = userService.createUser(readForm(), new String(passwordField.getPassword()));
            UIUtil.showInfo(this, created.getRole().getDisplayName() + " '" + created.getUsername() + "' created.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void updateUser() {
        User selected = requireSelection("user");
        if (selected == null) {
            return;
        }
        if (selected.getRole() != roleCombo.getSelectedItem()
                && !UIUtil.confirm(this, "Change the role of " + selected.getUsername() + " from "
                + selected.getRole() + " to " + roleCombo.getSelectedItem() + "?")) {
            return;
        }
        User user = readForm();
        user.setUserId(selected.getUserId());
        try {
            userService.updateUser(user);
            UIUtil.showInfo(this, "User updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void toggleActive() {
        User selected = requireSelection("user");
        if (selected == null) {
            return;
        }
        boolean activate = !selected.isActive();
        String action = activate ? "Activate" : "Deactivate";
        if (!UIUtil.confirm(this, action + " account '" + selected.getUsername() + "'?")) {
            return;
        }
        try {
            userService.setUserActive(selected.getUserId(), activate);
            UIUtil.showInfo(this, "Account " + (activate ? "activated." : "deactivated."));
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void resetPassword() {
        User selected = requireSelection("user");
        if (selected == null) {
            return;
        }
        JPasswordField newPassword = new JPasswordField(15);
        int choice = JOptionPane.showConfirmDialog(this, new Object[]{
                        "New password for " + selected.getUsername() + ":", newPassword},
                "Reset password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            userService.resetPassword(selected.getUserId(), new String(newPassword.getPassword()));
            UIUtil.showInfo(this, "Password changed.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void deleteUser() {
        User selected = requireSelection("user");
        if (selected == null || !UIUtil.confirm(this, "Permanently delete user '" + selected.getUsername()
                + "'?\n(If the user has academic records, deactivate instead.)")) {
            return;
        }
        try {
            userService.deleteUser(selected.getUserId());
            UIUtil.showInfo(this, "User deleted.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}