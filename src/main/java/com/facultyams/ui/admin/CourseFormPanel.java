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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;

/**
 * Add / Edit course form. It only collects values; {@link CourseService} validates them (course code
 * format and uniqueness, credits, level, semester, department and lecturer) and saves them.
 */
final class CourseFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient CourseService courseService;
    /** null when adding. */
    private final transient Course existing;

    final JTextField codeField = new JTextField(16);
    final JTextField nameField = new JTextField(16);
    final JComboBox<Department> departmentCombo = new JComboBox<>();
    final JSpinner creditsSpinner = new JSpinner(
            new SpinnerNumberModel(3, CourseService.MIN_CREDITS, CourseService.MAX_CREDITS, 1));
    final JComboBox<Integer> levelCombo = new JComboBox<>(new Integer[] {1, 2, 3, 4});
    final JComboBox<Integer> semesterCombo = new JComboBox<>(new Integer[] {1, 2});
    final JComboBox<Course.CourseType> typeCombo = new JComboBox<>(Course.CourseType.values());
    final JComboBox<User> lecturerCombo = new JComboBox<>();

    /** @param existing the course to edit, or null to add one */
    CourseFormPanel(CourseService courseService, DepartmentService departmentService, UserService userService,
                    Course existing) throws DatabaseException, AuthorizationException {
        super(new GridBagLayout());
        this.courseService = courseService;
        this.existing = existing;
        setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        departmentCombo.addItem(null);
        for (Department d : departmentService.getAllDepartments()) {
            departmentCombo.addItem(d);
        }
        departmentCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? "-- Select department --" : value,
                        index, isSelected, cellHasFocus);
            }
        });

        lecturerCombo.addItem(null);
        for (User lecturer : userService.getUsersByRole(Role.LECTURER)) {
            boolean assigned = existing != null && existing.getLecturerId() != null
                    && existing.getLecturerId() == lecturer.getUserId();
            if (lecturer.isActive() || assigned) {
                lecturerCombo.addItem(lecturer);
            }
        }
        lecturerCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = "-- Not assigned --";
                if (value instanceof User u) {
                    text = u.getFullName() + (u.getDepartmentName() == null ? "" : " (" + u.getDepartmentName() + ")")
                            + (u.isActive() ? "" : " - inactive");
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        addRow(0, "Course code * (e.g. ICT2132)", codeField, "Course name *", nameField);
        addRow(2, "Department *", departmentCombo, "Credits * (1-6)", creditsSpinner);
        addRow(4, "Level * (1-4)", levelCombo, "Semester * (1-2)", semesterCombo);
        addRow(6, "Course type *", typeCombo, "Lecturer in charge", lecturerCombo);

        if (existing != null) {
            codeField.setText(existing.getCourseCode());
            nameField.setText(existing.getCourseName());
            for (int i = 0; i < departmentCombo.getItemCount(); i++) {
                Department d = departmentCombo.getItemAt(i);
                if (d != null && d.getDepartmentId() == existing.getDepartmentId()) {
                    departmentCombo.setSelectedIndex(i);
                }
            }
            creditsSpinner.setValue(existing.getCredits());
            levelCombo.setSelectedItem(existing.getAcademicLevel());
            semesterCombo.setSelectedItem(existing.getSemester());
            typeCombo.setSelectedItem(existing.getCourseType());
            for (int i = 0; i < lecturerCombo.getItemCount(); i++) {
                User u = lecturerCombo.getItemAt(i);
                if (u != null && existing.getLecturerId() != null && u.getUserId() == existing.getLecturerId()) {
                    lecturerCombo.setSelectedIndex(i);
                }
            }
        }
    }

    private void addRow(int gridy, String leftLabel, JComponent left, String rightLabel, JComponent right) {
        addCell(gridy, 0, new JLabel(leftLabel), left);
        addCell(gridy, 1, new JLabel(rightLabel), right);
    }

    private void addCell(int gridy, int column, JLabel label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = gridy;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(6, 6, 0, 6);
        add(label, c);
        c.gridy = gridy + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(2, 6, 4, 6);
        add(field, c);
    }

    /** Saves through the service; returns false (after showing the message) if it was refused. */
    boolean save() {
        try {
            try {
                creditsSpinner.commitEdit();
            } catch (ParseException e) {
                throw new ValidationException("Credits must be a whole number between " + CourseService.MIN_CREDITS
                        + " and " + CourseService.MAX_CREDITS + ".");
            }
            Course course = new Course();
            course.setCourseId(existing == null ? 0 : existing.getCourseId());
            course.setCourseCode(codeField.getText());
            course.setCourseName(nameField.getText());
            Department department = (Department) departmentCombo.getSelectedItem();
            course.setDepartmentId(department == null ? 0 : department.getDepartmentId());
            course.setCredits((Integer) creditsSpinner.getValue());
            course.setAcademicLevel((Integer) levelCombo.getSelectedItem());
            course.setSemester((Integer) semesterCombo.getSelectedItem());
            course.setCourseType((Course.CourseType) typeCombo.getSelectedItem());
            User lecturer = (User) lecturerCombo.getSelectedItem();
            course.setLecturerId(lecturer == null ? null : lecturer.getUserId());
            if (existing == null) {
                courseService.addCourse(course);
            } else {
                courseService.updateCourse(course);
            }
            return true;
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
            return false;
        }
    }

    static boolean showDialog(Component parent, CourseService courseService, DepartmentService departmentService,
                              UserService userService, Course existing) {
        CourseFormPanel form;
        try {
            form = new CourseFormPanel(courseService, departmentService, userService, existing);
        } catch (DatabaseException | AuthorizationException e) {
            UIUtil.showException(parent, e);
            return false;
        }
        return FormDialog.show(parent, existing == null ? "Add Course" : "Edit Course " + existing.getCourseCode(),
                form, existing == null ? "Add course" : "Save changes", form::save);
    }
}