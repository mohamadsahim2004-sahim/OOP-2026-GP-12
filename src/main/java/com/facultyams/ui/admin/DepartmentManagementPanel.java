package com.facultyams.ui.admin;

import com.facultyams.dao.DepartmentDAO.DepartmentHead;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin "Department Management" screen: database-backed table (ID, code, name, head of department, actions)
 * with search, Add Department, Edit and Delete. All rules (Admin-only access, code/name validation, head must
 * be a lecturer of the department, delete only when no courses or users belong to it) are in
 * {@link DepartmentService}; this class shows data and reports errors.
 */
public class DepartmentManagementPanel extends AbstractManagementPanel<Department> {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Code", "Department Name", "Head of Department", "Actions"};
    private static final int ACTIONS_COLUMN = 4;

    private final transient DepartmentService departmentService;
    private final transient UserService userService;

    final JTextField searchField = searchField("Search departments...");
    final JLabel noteLabel = new JLabel(" ");

    private transient Map<Integer, DepartmentHead> heads = new HashMap<>();
    private boolean headsAvailable = true;

    public DepartmentManagementPanel() {
        this(new DepartmentService(), new UserService());
    }

    public DepartmentManagementPanel(DepartmentService departmentService, UserService userService) {
        super("department", COLUMNS);
        this.departmentService = departmentService;
        this.userService = userService;
        liveSearch(searchField, this::refresh);
        setToolbar(filterRow(260, new double[0], searchField), addButton("Add Department", this::addDepartment));
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(280);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        RowActions.install(table, ACTIONS_COLUMN, this::editRow, this::deleteRow);
        noteLabel.setForeground(AdminTheme.DANGER);
        noteLabel.setFont(AdminTheme.FONT_SMALL);
        noteLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        noteLabel.setVisible(false);
        add(noteLabel, BorderLayout.SOUTH);
    }

    @Override
    protected void loadLookups() {
        try {
            heads = departmentService.getDepartmentHeads();
            headsAvailable = true;
            noteLabel.setText(" ");
            noteLabel.setVisible(false);
        } catch (AuthorizationException | DatabaseException e) {
            // the rest of the screen keeps working when migration 002 has not been run
            heads = new HashMap<>();
            headsAvailable = false;
            noteLabel.setText("Head of department needs database/migrations/002_department_head.sql");
            noteLabel.setVisible(true);
        }
    }

    @Override
    protected List<Department> loadItems() throws DatabaseException, AuthorizationException {
        return departmentService.searchDepartments(searchField.getText());
    }

    @Override
    protected Object[] toRow(Department d) {
        DepartmentHead head = heads.get(d.getDepartmentId());
        return new Object[] {formatId(d.getDepartmentId()), d.getDepartmentCode(), d.getDepartmentName(),
                head == null ? "-" : head.fullName(), ""};
    }

    /** Department ids are shown as D001, D002, ... */
    static String formatId(int departmentId) {
        return code('D', departmentId);
    }

    // ------------------------------------------------------------------ actions

    private void addDepartment() {
        if (DepartmentFormPanel.showDialog(this, departmentService, userService, null, null, headsAvailable)) {
            refresh();
        }
    }

    private void editRow(int row) {
        Department department = itemAt(row);
        if (department == null) {
            return;
        }
        if (DepartmentFormPanel.showDialog(this, departmentService, userService, department,
                heads.get(department.getDepartmentId()), headsAvailable)) {
            refresh();
        }
    }

    private void deleteRow(int row) {
        Department department = itemAt(row);
        if (department == null) {
            return;
        }
        if (!UIUtil.confirm(this, "Delete department " + department.getDepartmentCode() + " - "
                + department.getDepartmentName() + "?\nThis cannot be undone. A department that still has courses "
                + "or users cannot be deleted.")) {
            return;
        }
        try {
            departmentService.deleteDepartment(department.getDepartmentId());
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}