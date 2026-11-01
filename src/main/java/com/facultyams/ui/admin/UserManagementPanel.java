package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.List;

/**
 * Admin screen: search, filter by role, add, edit, activate / deactivate, reset password and delete users.
 * Every rule (Admin-only access, unique username / e-mail, password strength, not deleting yourself ...) lives
 * in {@link UserService}; this class shows data and reports errors.
 */
public class UserManagementPanel extends AbstractManagementPanel<User> {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Full Name", "Username", "Email", "Role", "Department", "Status", "Actions"};
    private static final int STATUS_COLUMN = 6;
    private static final int ACTIONS_COLUMN = 7;

    private final transient UserService userService;
    private final transient DepartmentService departmentService;

    final JTextField searchField = searchField("Search users...");
    final JComboBox<Role> roleFilter = new JComboBox<>();

    public UserManagementPanel() {
        this(new UserService(), new DepartmentService());
    }

    public UserManagementPanel(UserService userService, DepartmentService departmentService) {
        super("user", COLUMNS);
        this.userService = userService;
        this.departmentService = departmentService;
        roleFilter.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof Role r ? r.getDisplayName() : "All Roles", index, selected, focus);
            }
        });
        roleFilter.addItem(null);
        for (Role role : Role.values()) {
            roleFilter.addItem(role);
        }
        roleFilter.getAccessibleContext().setAccessibleName("Filter by role");
        roleFilter.addActionListener(e -> refresh());
        liveSearch(searchField, this::refresh);
        setToolbar(filterRow(200, new double[] {2, 1}, searchField, roleFilter), addButton("Add User", this::addUser));

        int[] widths = {0, 130, 115, 155, 108, 120, 92};   // keep the total near 950 or the table scrolls sideways
        for (int i = 1; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(STATUS_COLUMN).setMinWidth(88);
        AdminTheme.badgeColumn(table, STATUS_COLUMN);
        RowActions.install(table, ACTIONS_COLUMN,
                RowActions.edit(this::editRow),
                new RowActions.Action(AdminIcons.Kind.KEY, new Color(0xB45309), new Color(0xFEF3C7),
                        "Reset password", this::resetPassword),
                new RowActions.Action(AdminIcons.Kind.POWER, new Color(0x475569), new Color(0xEEF2F7),
                        "Activate / Deactivate", this::toggleActive),
                RowActions.delete(this::deleteRow));
    }

    @Override
    protected List<User> loadItems() throws DatabaseException, AuthorizationException {
        return userService.searchUsers(searchField.getText(), (Role) roleFilter.getSelectedItem());
    }

    @Override
    protected Object[] toRow(User u) {
        return new Object[] {code('U', u.getUserId()), u.getFullName(), u.getUsername(), u.getEmail(),
                u.getRole().getDisplayName(), u.getDepartmentName() == null ? "-" : u.getDepartmentName(),
                u.isActive() ? "Active" : "Inactive", ""};
    }

    // ------------------------------------------------------------------ actions

    private void addUser() {
        if (UserFormPanel.showDialog(this, userService, departmentService, null)) {
            refresh();
            success("User created.");
        }
    }

    private void editRow(int row) {
        User user = itemAt(row);
        if (user != null && UserFormPanel.showDialog(this, userService, departmentService, user)) {
            refresh();
            success("User updated.");
        }
    }

    private void toggleActive(int row) {
        User user = itemAt(row);
        if (user == null) {
            return;
        }
        boolean activate = !user.isActive();
        if (!UIUtil.confirm(this, (activate ? "Activate" : "Deactivate") + " account '" + user.getUsername() + "'?")) {
            return;
        }
        try {
            userService.setUserActive(user.getUserId(), activate);
            refresh();
            success("Account " + (activate ? "activated." : "deactivated."));
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void resetPassword(int row) {
        User user = itemAt(row);
        if (user == null) {
            return;
        }
        JPasswordField newPassword = new JPasswordField(15);
        AdminTheme.field(newPassword);
        int choice = JOptionPane.showConfirmDialog(this,
                new Object[] {"New password for " + user.getUsername() + ":", newPassword},
                "Reset password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            userService.resetPassword(user.getUserId(), new String(newPassword.getPassword()));
            success("Password changed.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        } finally {
            newPassword.setText("");
        }
    }

    private void deleteRow(int row) {
        User user = itemAt(row);
        if (user == null || !UIUtil.confirm(this, "Permanently delete user '" + user.getUsername()
                + "'?\n(If the user has academic records, deactivate instead.)")) {
            return;
        }
        try {
            userService.deleteUser(user.getUserId());
            refresh();
            success("User deleted.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}