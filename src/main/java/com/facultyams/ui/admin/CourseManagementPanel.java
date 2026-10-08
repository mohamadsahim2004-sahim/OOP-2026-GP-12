package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.Department;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.service.CourseService;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.UserService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;

/** Admin screen: add / view / update / delete / search / filter courses. */
public class CourseManagementPanel extends AbstractManagementPanel<Course> {

    private static final Integer[] LEVELS = {1, 2, 3, 4};
    private static final Integer[] SEMESTERS = {1, 2};

    private final CourseService courseService = new CourseService();
    private final DepartmentService departmentService = new DepartmentService();
    private final UserService userService = new UserService();

    // search / filter bar
    private final JTextField searchField = new JTextField(15);
    private final JComboBox<Department> departmentFilter = new JComboBox<>();
    private final JComboBox<Integer> levelFilter = new JComboBox<>();
    private final JComboBox<Integer> semesterFilter = new JComboBox<>();

    // form
    private final JTextField codeField = new JTextField(10);
    private final JTextField nameField = new JTextField(25);
    private final JSpinner creditsSpinner = new JSpinner(
            new SpinnerNumberModel(2, CourseService.MIN_CREDITS, CourseService.MAX_CREDITS, 1));
    private final JComboBox<Integer> levelCombo = new JComboBox<>(LEVELS);
    private final JComboBox<Integer> semesterCombo = new JComboBox<>(SEMESTERS);
    private final JComboBox<Course.CourseType> typeCombo = new JComboBox<>(Course.CourseType.values());
    private final JComboBox<Department> departmentCombo = new JComboBox<>();
    private final JComboBox<User> lecturerCombo = new JComboBox<>();

    public CourseManagementPanel() {
        super(new String[]{"ID", "Code", "Name", "Credits", "Level", "Semester", "Type", "Department", "Lecturer"});
        departmentFilter.setRenderer(nullRenderer("All departments"));
        departmentFilter.setPrototypeDisplayValue(new Department(0, "DICT", "Department of Information"));
        levelFilter.setRenderer(nullRenderer("All levels"));
        semesterFilter.setRenderer(nullRenderer("All semesters"));
        departmentCombo.setRenderer(nullRenderer("-- Select department --"));
        lecturerCombo.setRenderer(nullRenderer("-- Not assigned --"));

        levelFilter.addItem(null);
        for (Integer level : LEVELS) {
            levelFilter.addItem(level);
        }
        semesterFilter.addItem(null);
        for (Integer semester : SEMESTERS) {
            semesterFilter.addItem(semester);
        }

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.SOUTH);
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton resetButton = new JButton("Reset");
        bar.add(new JLabel("Code / name:"));
        bar.add(searchField);
        bar.add(departmentFilter);
        bar.add(levelFilter);
        bar.add(semesterFilter);
        bar.add(searchButton);
        bar.add(resetButton);
        searchButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        resetButton.addActionListener(e -> {
            searchField.setText("");
            departmentFilter.setSelectedItem(null);
            levelFilter.setSelectedItem(null);
            semesterFilter.setSelectedItem(null);
            refresh();
        });
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Course code:", codeField);
        addField(form, 0, 1, "Course name:", nameField);
        addField(form, 1, 0, "Credits:", creditsSpinner);
        addField(form, 1, 1, "Type:", typeCombo);
        addField(form, 2, 0, "Level:", levelCombo);
        addField(form, 2, 1, "Semester:", semesterCombo);
        addField(form, 3, 0, "Department:", departmentCombo);
        addField(form, 3, 1, "Lecturer in charge:", lecturerCombo);

        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        addButton.addActionListener(e -> addCourse());
        updateButton.addActionListener(e -> updateCourse());
        deleteButton.addActionListener(e -> deleteCourse());
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
        south.setBorder(BorderFactory.createTitledBorder("Course details"));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        Department selectedFilter = (Department) departmentFilter.getSelectedItem();
        List<Department> departments = departmentService.getAllDepartments();

        departmentFilter.removeAllItems();
        departmentCombo.removeAllItems();
        departmentFilter.addItem(null);
        departmentCombo.addItem(null);
        for (Department d : departments) {
            departmentFilter.addItem(d);
            departmentCombo.addItem(d);
        }
        departmentFilter.setSelectedItem(selectedFilter != null && departments.contains(selectedFilter)
                ? selectedFilter : null);

        lecturerCombo.removeAllItems();
        lecturerCombo.addItem(null);
        for (User lecturer : userService.getUsersByRole(Role.LECTURER)) {
            if (lecturer.isActive()) {
                lecturerCombo.addItem(lecturer);
            }
        }
    }

    @Override
    protected List<Course> loadItems() throws DatabaseException, AuthorizationException {
        Department department = (Department) departmentFilter.getSelectedItem();
        return courseService.searchCourses(searchField.getText(),
                department == null ? null : department.getDepartmentId(),
                (Integer) levelFilter.getSelectedItem(),
                (Integer) semesterFilter.getSelectedItem());
    }

    @Override
    protected Object[] toRow(Course c) {
        return new Object[]{c.getCourseId(), c.getCourseCode(), c.getCourseName(), c.getCredits(),
                c.getAcademicLevel(), c.getSemester(), c.getCourseType(), c.getDepartmentName(),
                c.getLecturerName() == null ? "-" : c.getLecturerName()};
    }

    @Override
    protected void fillForm(Course c) {
        codeField.setText(c.getCourseCode());
        nameField.setText(c.getCourseName());
        creditsSpinner.setValue(c.getCredits());
        levelCombo.setSelectedItem(c.getAcademicLevel());
        semesterCombo.setSelectedItem(c.getSemester());
        typeCombo.setSelectedItem(c.getCourseType());
        selectDepartment(c.getDepartmentId());
        selectLecturer(c.getLecturerId());
    }

    @Override
    protected void clearForm() {
        codeField.setText("");
        nameField.setText("");
        creditsSpinner.setValue(2);
        levelCombo.setSelectedIndex(0);
        semesterCombo.setSelectedIndex(0);
        typeCombo.setSelectedItem(Course.CourseType.THEORY_AND_PRACTICAL);
        departmentCombo.setSelectedItem(null);
        lecturerCombo.setSelectedItem(null);
    }

    private void selectDepartment(int departmentId) {
        departmentCombo.setSelectedItem(null);
        for (int i = 0; i < departmentCombo.getItemCount(); i++) {
            Department d = departmentCombo.getItemAt(i);
            if (d != null && d.getDepartmentId() == departmentId) {
                departmentCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectLecturer(Integer lecturerId) {
        lecturerCombo.setSelectedItem(null);
        if (lecturerId == null) {
            return;
        }
        for (int i = 0; i < lecturerCombo.getItemCount(); i++) {
            User u = lecturerCombo.getItemAt(i);
            if (u != null && u.getUserId() == lecturerId) {
                lecturerCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private Course readForm() {
        Course c = new Course();
        c.setCourseCode(codeField.getText());
        c.setCourseName(nameField.getText());
        c.setCredits((Integer) creditsSpinner.getValue());
        c.setAcademicLevel((Integer) levelCombo.getSelectedItem());
        c.setSemester((Integer) semesterCombo.getSelectedItem());
        c.setCourseType((Course.CourseType) typeCombo.getSelectedItem());
        Department d = (Department) departmentCombo.getSelectedItem();
        c.setDepartmentId(d == null ? 0 : d.getDepartmentId());
        User lecturer = (User) lecturerCombo.getSelectedItem();
        c.setLecturerId(lecturer == null ? null : lecturer.getUserId());
        return c;
    }

    private void addCourse() {
        try {
            Course c = courseService.addCourse(readForm());
            UIUtil.showInfo(this, "Course " + c.getCourseCode() + " added.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void updateCourse() {
        Course selected = requireSelection("course");
        if (selected == null) {
            return;
        }
        Course c = readForm();
        c.setCourseId(selected.getCourseId());
        try {
            courseService.updateCourse(c);
            UIUtil.showInfo(this, "Course updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void deleteCourse() {
        Course selected = requireSelection("course");
        if (selected == null || !UIUtil.confirm(this, "Delete course " + selected + "?")) {
            return;
        }
        try {
            courseService.deleteCourse(selected.getCourseId());
            UIUtil.showInfo(this, "Course deleted.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}