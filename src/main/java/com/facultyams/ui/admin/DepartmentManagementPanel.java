package com.facultyams.ui.admin;

import com.facultyams.dao.DepartmentDAO.DepartmentHead;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin "Department Management" screen: database-backed table (ID, code, name, head of department,
 * actions) with search, Add Department, Edit and Delete. All rules (Admin-only access, code/name
 * validation, head must be a lecturer of the department, delete only when no courses or users belong to
 * it) are in {@link DepartmentService}; this class shows data and reports errors.
 */
public class DepartmentManagementPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Code", "Department Name", "Head of Department", "Actions"};
    private static final int ACTIONS_COLUMN = 4;

    private final transient DepartmentService departmentService;
    private final transient UserService userService;

    final JTextField searchField = new HintTextField("Search department...", 24);
    final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable table = new JTable(tableModel);
    final JLabel entriesLabel = new JLabel();
    final JLabel noteLabel = new JLabel(" ");

    private transient List<Department> departments = new ArrayList<>();
    private transient Map<Integer, DepartmentHead> heads = new HashMap<>();
    private boolean headsAvailable = true;

    public DepartmentManagementPanel() {
        this(new DepartmentService(), new UserService());
    }

    public DepartmentManagementPanel(DepartmentService departmentService, UserService userService) {
        super(new BorderLayout(0, 12));
        this.departmentService = departmentService;
        this.userService = userService;
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JButton search = new JButton("Search");
        JButton add = new JButton("+ Add Department");
        search.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        add.addActionListener(e -> addDepartment());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(searchField);
        left.add(search);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(add);
        JPanel bar = new JPanel(new BorderLayout());
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(280);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        table.getColumnModel().getColumn(ACTIONS_COLUMN).setPreferredWidth(140);
        RowActions.install(table, ACTIONS_COLUMN, this::editRow, this::deleteRow);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        return scroll;
    }

    private JPanel buildFooter() {
        entriesLabel.setForeground(AdminTheme.TEXT_MUTED);
        noteLabel.setForeground(AdminTheme.DANGER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(entriesLabel, BorderLayout.WEST);
        footer.add(noteLabel, BorderLayout.EAST);
        return footer;
    }

    // ------------------------------------------------------------------ data

    @Override
    public void refresh() {
        try {
            departments = departmentService.searchDepartments(searchField.getText());
        } catch (AuthorizationException | DatabaseException e) {
            departments = new ArrayList<>();
            UIUtil.showException(this, e);
        }
        try {
            heads = departmentService.getDepartmentHeads();
            headsAvailable = true;
            noteLabel.setText(" ");
        } catch (AuthorizationException | DatabaseException e) {
            // the rest of the screen keeps working when migration 002 has not been run
            heads = new HashMap<>();
            headsAvailable = false;
            noteLabel.setText("Head of department needs database/migrations/002_department_head.sql");
        }
        tableModel.setRowCount(0);
        for (Department d : departments) {
            DepartmentHead head = heads.get(d.getDepartmentId());
            tableModel.addRow(new Object[] {formatId(d.getDepartmentId()), d.getDepartmentCode(),
                    d.getDepartmentName(), head == null ? "-" : head.fullName(), ""});
        }
        entriesLabel.setText(departments.isEmpty() ? "No departments found"
                : "Showing " + departments.size() + (departments.size() == 1 ? " department" : " departments"));
    }

    /** Department ids are shown as D001, D002, ... */
    static String formatId(int departmentId) {
        return String.format("D%03d", departmentId);
    }

    // ------------------------------------------------------------------ actions

    private void addDepartment() {
        if (DepartmentFormPanel.showDialog(this, departmentService, userService, null, null, headsAvailable)) {
            refresh();
        }
    }

    private void editRow(int row) {
        if (row < 0 || row >= departments.size()) {
            return;
        }
        Department department = departments.get(row);
        if (DepartmentFormPanel.showDialog(this, departmentService, userService, department,
                heads.get(department.getDepartmentId()), headsAvailable)) {
            refresh();
        }
    }

    private void deleteRow(int row) {
        if (row < 0 || row >= departments.size()) {
            return;
        }
        Department department = departments.get(row);
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