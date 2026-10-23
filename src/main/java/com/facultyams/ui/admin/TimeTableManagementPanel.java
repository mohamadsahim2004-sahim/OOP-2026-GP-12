package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.Department;
import com.facultyams.model.TimeTable;
import com.facultyams.service.CourseService;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.TimeTableService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Admin screen: create / view / update / delete / search timetable slots, filtered by department and semester. */
public class TimeTableManagementPanel extends AbstractManagementPanel<TimeTable> {

    private static final DayOfWeek[] DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private final TimeTableService timeTableService = new TimeTableService();
    private final DepartmentService departmentService = new DepartmentService();
    private final CourseService courseService = new CourseService();

    // search / filter bar (null entry = "All ...")
    private final JTextField searchField = new JTextField(15);
    private final JComboBox<Department> departmentFilter = new JComboBox<>();
    private final JComboBox<Integer> semesterFilter = new JComboBox<>(new Integer[]{null, 1, 2});

    // form
    private final JComboBox<Course> courseCombo = new JComboBox<>();
    private final JComboBox<DayOfWeek> dayCombo = new JComboBox<>(DAYS);
    private final JTextField startField = new JTextField("08:00", 6);
    private final JTextField endField = new JTextField("10:00", 6);
    private final JTextField venueField = new JTextField(12);

    public TimeTableManagementPanel() {
        super(new String[]{"ID", "Day", "Time", "Course Code", "Course Name", "Department", "Sem", "Lecture Hall"});
        departmentFilter.setRenderer(nullRenderer("All Departments"));
        semesterFilter.setRenderer(nullRenderer("All Semesters"));
        dayCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                String text = value instanceof DayOfWeek d ? dayName(d) : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.SOUTH);
        AdminTheme.apply(this);
    }

    private static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton viewButton = new JButton("View");
        JButton showAllButton = new JButton("Show all");

        bar.add(new JLabel("Search course / hall / day:"));
        bar.add(searchField);
        bar.add(new JLabel("Department:"));
        bar.add(departmentFilter);
        bar.add(new JLabel("Semester:"));
        bar.add(semesterFilter);
        bar.add(viewButton);
        bar.add(showAllButton);

        viewButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            departmentFilter.setSelectedIndex(0);
            semesterFilter.setSelectedIndex(0);
            refresh();
        });
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Course:", courseCombo);
        addField(form, 0, 1, "Day:", dayCombo);
        addField(form, 1, 0, "Start (HH:mm):", startField);
        addField(form, 1, 1, "End (HH:mm):", endField);
        addField(form, 2, 0, "Lecture hall / venue:", venueField);

        JButton addButton = new JButton("Add Timetable");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        addButton.addActionListener(e -> addSlot());
        updateButton.addActionListener(e -> updateSlot());
        deleteButton.addActionListener(e -> deleteSlot());
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
        south.setBorder(BorderFactory.createTitledBorder("Timetable slot"));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        Department keepFilter = (Department) departmentFilter.getSelectedItem();
        Course keepCourse = (Course) courseCombo.getSelectedItem();

        departmentFilter.removeAllItems();
        departmentFilter.addItem(null);
        for (Department d : departmentService.getAllDepartments()) {
            departmentFilter.addItem(d);
            if (keepFilter != null && d.getDepartmentId() == keepFilter.getDepartmentId()) {
                departmentFilter.setSelectedItem(d);
            }
        }

        courseCombo.removeAllItems();
        for (Course c : courseService.getAllCourses()) {
            courseCombo.addItem(c);
            if (keepCourse != null && c.getCourseId() == keepCourse.getCourseId()) {
                courseCombo.setSelectedItem(c);
            }
        }
    }

    @Override
    protected List<TimeTable> loadItems() throws DatabaseException, AuthorizationException {
        Department department = (Department) departmentFilter.getSelectedItem();
        Integer semester = (Integer) semesterFilter.getSelectedItem();
        return timeTableService.searchTimeTable(searchField.getText(),
                department == null ? null : department.getDepartmentId(), semester);
    }

    @Override
    protected Object[] toRow(TimeTable t) {
        return new Object[]{t.getTimetableId(), dayName(t.getDayOfWeek()), t.getTimeRange(), t.getCourseCode(),
                t.getCourseName(), t.getDepartmentName(), t.getSemester(), t.getVenue()};
    }

    @Override
    protected void fillForm(TimeTable t) {
        for (int i = 0; i < courseCombo.getItemCount(); i++) {
            if (courseCombo.getItemAt(i).getCourseId() == t.getCourseId()) {
                courseCombo.setSelectedIndex(i);
                break;
            }
        }
        dayCombo.setSelectedItem(t.getDayOfWeek());
        startField.setText(t.getStartTime().toString());
        endField.setText(t.getEndTime().toString());
        venueField.setText(t.getVenue());
    }

    @Override
    protected void clearForm() {
        dayCombo.setSelectedIndex(0);
        startField.setText("08:00");
        endField.setText("10:00");
        venueField.setText("");
    }

    private TimeTable readForm() throws ValidationException {
        Course course = (Course) courseCombo.getSelectedItem();
        if (course == null) {
            throw new ValidationException("Please select a course.");
        }
        TimeTable t = new TimeTable();
        t.setCourseId(course.getCourseId());
        t.setDayOfWeek((DayOfWeek) dayCombo.getSelectedItem());
        try {
            t.setStartTime(LocalTime.parse(startField.getText().trim()));
            t.setEndTime(LocalTime.parse(endField.getText().trim()));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Times must be written as HH:mm, e.g. 08:00.", e);
        }
        t.setVenue(venueField.getText());
        return t;
    }

    private void addSlot() {
        try {
            timeTableService.addSlot(readForm());
            UIUtil.showInfo(this, "Timetable slot added.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void updateSlot() {
        TimeTable selected = requireSelection("timetable slot");
        if (selected == null) {
            return;
        }
        try {
            TimeTable t = readForm();
            t.setTimetableId(selected.getTimetableId());
            timeTableService.updateSlot(t);
            UIUtil.showInfo(this, "Timetable slot updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    @Override
    protected void deleteSelected() {
        deleteSlot();
    }

    private void deleteSlot() {
        TimeTable selected = requireSelection("timetable slot");
        if (selected == null || !UIUtil.confirm(this, "Delete timetable slot " + selected + "?")) {
            return;
        }
        try {
            timeTableService.deleteSlot(selected.getTimetableId());
            UIUtil.showInfo(this, "Timetable slot deleted.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}