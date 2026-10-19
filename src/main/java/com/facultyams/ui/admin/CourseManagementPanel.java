package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.Department;
import com.facultyams.service.CourseService;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin "Course Management" screen: database-backed table (course code, name, department, credits,
 * lecturer, actions) with a department filter, search, Add Course, Edit and Delete. All rules (Admin-only
 * access, course code, credits, department and lecturer validation, safe delete) are in
 * {@link CourseService}; this class shows data and reports errors.
 */
public class CourseManagementPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Course Code", "Course Name", "Department", "Credits", "Lecturer", "Actions"};
    private static final int ACTIONS_COLUMN = 5;

    private final transient CourseService courseService;
    private final transient DepartmentService departmentService;
    private final transient UserService userService;

    final JComboBox<Department> departmentFilter = new JComboBox<>();
    final JTextField searchField = new HintTextField("Search course...", 16);
    final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable table = new JTable(tableModel);
    final JLabel entriesLabel = new JLabel();

    private transient List<Course> courses = new ArrayList<>();
    private boolean loadingFilter;

    public CourseManagementPanel() {
        this(new CourseService(), new DepartmentService(), new UserService());
    }

    public CourseManagementPanel(CourseService courseService, DepartmentService departmentService,
                                 UserService userService) {
        super(new BorderLayout(0, 12));
        this.courseService = courseService;
        this.departmentService = departmentService;
        this.userService = userService;
        departmentFilter.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? "All Departments" : value,
                        index, isSelected, cellHasFocus);
            }
        });
        departmentFilter.addItem(null);
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        entriesLabel.setForeground(AdminTheme.TEXT_MUTED);
        add(entriesLabel, BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JButton search = new JButton("Search");
        JButton add = new JButton("+ Add Course");
        search.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        departmentFilter.addActionListener(e -> {
            if (!loadingFilter) {
                refresh();
            }
        });
        add.addActionListener(e -> addCourse());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(new JLabel("Department"));
        departmentFilter.setPrototypeDisplayValue(new Department(0, "DICT", "Information and Communication"));
        left.add(departmentFilter);
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
        table.getColumnModel().getColumn(0).setPreferredWidth(95);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(ACTIONS_COLUMN).setPreferredWidth(140);
        RowActions.install(table, ACTIONS_COLUMN, this::editRow, this::deleteRow);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        return scroll;
    }

    // ------------------------------------------------------------------ data

    @Override
    public void refresh() {
        loadDepartmentFilter();
        Department department = (Department) departmentFilter.getSelectedItem();
        try {
            courses = courseService.searchCourses(searchField.getText(),
                    department == null ? null : department.getDepartmentId(), null, null);
        } catch (AuthorizationException | DatabaseException e) {
            courses = new ArrayList<>();
            UIUtil.showException(this, e);
        }
        tableModel.setRowCount(0);
        for (Course c : courses) {
            tableModel.addRow(new Object[] {c.getCourseCode(), c.getCourseName(), c.getDepartmentName(),
                    c.getCredits(), c.getLecturerName() == null ? "-" : c.getLecturerName(), ""});
        }
        entriesLabel.setText(courses.isEmpty() ? "No courses found"
                : "Showing " + courses.size() + (courses.size() == 1 ? " course" : " courses"));
    }

    /** Reloads the department filter from the database, keeping the current selection. */
    private void loadDepartmentFilter() {
        Department keep = (Department) departmentFilter.getSelectedItem();
        loadingFilter = true;
        try {
            departmentFilter.removeAllItems();
            departmentFilter.addItem(null);
            for (Department d : departmentService.getAllDepartments()) {
                departmentFilter.addItem(d);
                if (d.equals(keep)) {
                    departmentFilter.setSelectedItem(d);
                }
            }
        } catch (AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        } finally {
            loadingFilter = false;
        }
    }

    // ------------------------------------------------------------------ actions

    private void addCourse() {
        if (CourseFormPanel.showDialog(this, courseService, departmentService, userService, null)) {
            refresh();
        }
    }

    private void editRow(int row) {
        if (row < 0 || row >= courses.size()) {
            return;
        }
        if (CourseFormPanel.showDialog(this, courseService, departmentService, userService, courses.get(row))) {
            refresh();
        }
    }

    private void deleteRow(int row) {
        if (row < 0 || row >= courses.size()) {
            return;
        }
        Course course = courses.get(row);
        if (!UIUtil.confirm(this, "Delete course " + course.getCourseCode() + " - " + course.getCourseName()
                + "?\nThis cannot be undone. A course with timetable slots or other records cannot be deleted.")) {
            return;
        }
        try {
            courseService.deleteCourse(course.getCourseId());
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}