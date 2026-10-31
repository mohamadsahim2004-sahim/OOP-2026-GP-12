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

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.List;

/**
 * Admin "Course Management" screen: database-backed table (course code, name, department, level, semester,
 * credits, lecturer, actions) with department and level filters, search, Add Course, Edit and Delete. All rules (Admin-only access, course
 * code, credits, department and lecturer validation, safe delete) are in {@link CourseService}; this class
 * shows data and reports errors.
 */
public class CourseManagementPanel extends AbstractManagementPanel<Course> {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Course Code", "Course Name", "Department", "Level", "Sem", "Credits",
            "Lecturer", "Actions"};
    private static final int ACTIONS_COLUMN = 7;

    private final transient CourseService courseService;
    private final transient DepartmentService departmentService;
    private final transient UserService userService;

    final JComboBox<Department> departmentFilter = new JComboBox<>();
    /** null = all levels, otherwise the academic level 1-4. */
    final JComboBox<Integer> levelFilter = new JComboBox<>(new Integer[] {null, 1, 2, 3, 4});
    final JTextField searchField = searchField("Search courses...");
    private boolean loadingFilter;

    public CourseManagementPanel() {
        this(new CourseService(), new DepartmentService(), new UserService());
    }

    public CourseManagementPanel(CourseService courseService, DepartmentService departmentService,
                                 UserService userService) {
        super("course", COLUMNS);
        this.courseService = courseService;
        this.departmentService = departmentService;
        this.userService = userService;
        departmentFilter.setRenderer(nullRenderer("All Departments"));
        departmentFilter.addItem(null);
        departmentFilter.getAccessibleContext().setAccessibleName("Filter by department");
        departmentFilter.addActionListener(e -> {
            if (!loadingFilter) {
                refresh();
            }
        });
        levelFilter.setRenderer(levelRenderer());
        levelFilter.getAccessibleContext().setAccessibleName("Filter by level");
        levelFilter.addActionListener(e -> {
            if (!loadingFilter) {
                refresh();
            }
        });
        liveSearch(searchField, this::refresh);
        setToolbar(filterRow(150, new double[] {2, 1.2, 0.8}, searchField, departmentFilter, levelFilter),
                addButton("Add Course", this::addCourse));
        int[] widths = {122, 195, 155, 72, 62, 80, 130};
        int[] minimums = {118, 90, 90, 68, 58, 76, 90};   // keep the short headers readable ("Level", "Sem", "Credits")
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimums[i]);
        }
        RowActions.install(table, ACTIONS_COLUMN, this::editRow, this::deleteRow);
    }

    /** Reloads the department filter from the database, keeping the current selection. */
    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
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
        } finally {
            loadingFilter = false;
        }
    }

    @Override
    protected List<Course> loadItems() throws DatabaseException, AuthorizationException {
        Department department = (Department) departmentFilter.getSelectedItem();
        return courseService.searchCourses(searchField.getText(),
                department == null ? null : department.getDepartmentId(), (Integer) levelFilter.getSelectedItem(), null);
    }

    @Override
    protected Object[] toRow(Course c) {
        return new Object[] {c.getCourseCode(), c.getCourseName(), c.getDepartmentName(),
                c.getAcademicLevel(), c.getSemester(), c.getCredits(),
                c.getLecturerName() == null ? "-" : c.getLecturerName(), ""};
    }

    /** Shows "All Levels" for the empty choice and "Level N" otherwise. */
    private static javax.swing.DefaultListCellRenderer levelRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "All Levels" : "Level " + value,
                        index, selected, focus);
            }
        };
    }

    // ------------------------------------------------------------------ actions

    private void addCourse() {
        if (CourseFormPanel.showDialog(this, courseService, departmentService, userService, null)) {
            refresh();
            success("Course created.");
        }
    }

    private void editRow(int row) {
        Course course = itemAt(row);
        if (course != null && CourseFormPanel.showDialog(this, courseService, departmentService, userService, course)) {
            refresh();
            success("Course updated.");
        }
    }

    private void deleteRow(int row) {
        Course course = itemAt(row);
        if (course == null) {
            return;
        }
        if (!UIUtil.confirm(this, "Delete course " + course.getCourseCode() + " - " + course.getCourseName()
                + "?\nThis cannot be undone. A course with timetable slots or other records cannot be deleted.")) {
            return;
        }
        try {
            courseService.deleteCourse(course.getCourseId());
            refresh();
            success("Course deleted.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}