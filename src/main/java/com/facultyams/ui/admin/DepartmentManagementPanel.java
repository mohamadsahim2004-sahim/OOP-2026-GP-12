package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.service.DepartmentService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;

/** Admin screen: add / view / update / delete / search departments. */
public class DepartmentManagementPanel extends AbstractManagementPanel<Department> {

    private final DepartmentService departmentService = new DepartmentService();

    private final JTextField searchField = new JTextField(20);
    private final JTextField codeField = new JTextField(10);
    private final JTextField nameField = new JTextField(30);

    public DepartmentManagementPanel() {
        super(new String[]{"ID", "Code", "Name"});
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
    protected List<Department> loadItems() throws DatabaseException, AuthorizationException {
        return departmentService.searchDepartments(searchField.getText());
    }

    @Override
    protected Object[] toRow(Department d) {
        return new Object[]{d.getDepartmentId(), d.getDepartmentCode(), d.getDepartmentName()};
    }

    @Override
    protected void fillForm(Department d) {
        codeField.setText(d.getDepartmentCode());
        nameField.setText(d.getDepartmentName());
    }

    @Override
    protected void clearForm() {
        codeField.setText("");
        nameField.setText("");
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
            UIUtil.showInfo(this, "Department updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
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