package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.dao.DepartmentDAO.DepartmentHead;
import com.facultyams.model.Department;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin screen: add / view / update / delete / search departments. */
public class DepartmentManagementPanel extends AbstractManagementPanel<Department> {

    private final DepartmentService departmentService = new DepartmentService();
    private final UserService userService = new UserService();

    /** Department id -> head. Empty when migration 002 has not been run. */
    private Map<Integer, DepartmentHead> heads = new HashMap<>();
    private boolean headsAvailable = true;
    private final JComboBox<User> headCombo = new JComboBox<>();
    private final JLabel headHint = new JLabel(" ");

    private final JTextField searchField = new JTextField(20);
    private final JTextField codeField = new JTextField(10);
    private final JTextField nameField = new JTextField(30);

    public DepartmentManagementPanel() {
        super(new String[]{"ID", "Code", "Name", "Head of Department"});
        headCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                String text = value instanceof User u ? u.getFullName() + " (" + u.getDepartmentName() + ")" : "-- None --";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.SOUTH);
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");
        bar.add(new JLabel("Search code / name:"));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(showAllButton);
        searchButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Code:", codeField);
        addField(form, 0, 1, "Name:", nameField);
        addField(form, 1, 0, "Head of Department:", headCombo);
        headHint.setForeground(AdminTheme.DANGER);
        java.awt.GridBagConstraints hint = new java.awt.GridBagConstraints();
        hint.gridx = 1;
        hint.gridy = 2;
        hint.gridwidth = 3;
        hint.anchor = java.awt.GridBagConstraints.LINE_START;
        form.add(headHint, hint);

        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        addButton.addActionListener(e -> addDepartment());
        updateButton.addActionListener(e -> updateDepartment());
        deleteButton.addActionListener(e -> deleteDepartment());
        clearButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createTitledBorder("Department details"));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        User keep = (User) headCombo.getSelectedItem();
        headCombo.removeAllItems();
        headCombo.addItem(null);
        for (User lecturer : userService.getUsersByRole(Role.LECTURER)) {
            headCombo.addItem(lecturer);
            if (keep != null && keep.getUserId() == lecturer.getUserId()) {
                headCombo.setSelectedItem(lecturer);
            }
        }
        try {
            heads = departmentService.getDepartmentHeads();
            headsAvailable = true;
            headHint.setText(" ");
        } catch (DatabaseException e) {
            // keep the rest of the screen working if migration 002 is missing
            heads = new HashMap<>();
            headsAvailable = false;
            headHint.setText("Head of Department needs database/migrations/002_department_head.sql");
        }
        headCombo.setEnabled(headsAvailable);
    }

    @Override
    protected List<Department> loadItems() throws DatabaseException, AuthorizationException {
        return departmentService.searchDepartments(searchField.getText());
    }

    @Override
    protected Object[] toRow(Department d) {
        DepartmentHead head = heads.get(d.getDepartmentId());
        return new Object[]{d.getDepartmentId(), d.getDepartmentCode(), d.getDepartmentName(),
                head == null ? "-" : head.fullName()};
    }

    @Override
    protected void fillForm(Department d) {
        codeField.setText(d.getDepartmentCode());
        nameField.setText(d.getDepartmentName());
        DepartmentHead head = heads.get(d.getDepartmentId());
        headCombo.setSelectedItem(null);
        for (int i = 0; i < headCombo.getItemCount(); i++) {
            User u = headCombo.getItemAt(i);
            if (head != null && u != null && u.getUserId() == head.userId()) {
                headCombo.setSelectedIndex(i);
            }
        }
    }

    @Override
    protected void clearForm() {
        codeField.setText("");
        nameField.setText("");
        headCombo.setSelectedItem(null);
    }

    /** Saves the head chosen in the form (skipped when migration 002 is missing). */
    private void saveHead(int departmentId) throws ValidationException, AuthorizationException, DatabaseException {
        if (!headsAvailable) {
            return;
        }
        User head = (User) headCombo.getSelectedItem();
        departmentService.setDepartmentHead(departmentId, head == null ? null : head.getUserId());
    }

    private Department readForm() {
        Department d = new Department();
        d.setDepartmentCode(codeField.getText());
        d.setDepartmentName(nameField.getText());
        return d;
    }

    private void addDepartment() {
        try {
            Department d = departmentService.addDepartment(readForm());
            saveHead(d.getDepartmentId());
            UIUtil.showInfo(this, "Department " + d.getDepartmentCode() + " added.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void updateDepartment() {
        Department selected = requireSelection("department");
        if (selected == null) {
            return;
        }
        Department d = readForm();
        d.setDepartmentId(selected.getDepartmentId());
        try {
            departmentService.updateDepartment(d);
            saveHead(d.getDepartmentId());
            UIUtil.showInfo(this, "Department updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    @Override
    protected void deleteSelected() {
        deleteDepartment();
    }

    private void deleteDepartment() {
        Department selected = requireSelection("department");
        if (selected == null || !UIUtil.confirm(this, "Delete department " + selected + "?")) {
            return;
        }
        try {
            departmentService.deleteDepartment(selected.getDepartmentId());
            UIUtil.showInfo(this, "Department deleted.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}