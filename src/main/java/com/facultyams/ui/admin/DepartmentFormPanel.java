package com.facultyams.ui.admin;

import com.facultyams.dao.DepartmentDAO.DepartmentHead;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.model.User;
import com.facultyams.security.Role;
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
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Add / Edit department form. It only collects values; {@link DepartmentService} validates them (code
 * format and uniqueness, name, head of department) and saves them.
 *
 * <p>The head of a department must be a lecturer of that department, so the head can only be chosen
 * when editing an existing department (a new department has no lecturers yet).
 */
final class DepartmentFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient DepartmentService departmentService;
    /** null when adding. */
    private final transient Department existing;
    /** false when migration 002 (departments.head_user_id) has not been run. */
    private final boolean headsAvailable;

    final JTextField codeField = new JTextField(16);
    final JTextField nameField = new JTextField(16);
    final JComboBox<User> headCombo = new JComboBox<>();

    /**
     * @param existing    the department to edit, or null to add one
     * @param currentHead the department's current head, or null if it has none (or when adding)
     */
    DepartmentFormPanel(DepartmentService departmentService, UserService userService, Department existing,
                        DepartmentHead currentHead, boolean headsAvailable)
            throws DatabaseException, AuthorizationException {
        super(new GridBagLayout());
        this.departmentService = departmentService;
        this.existing = existing;
        this.headsAvailable = headsAvailable;
        setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        headCombo.addItem(null);
        if (existing != null && headsAvailable) {
            for (User lecturer : userService.getUsersByRole(Role.LECTURER)) {
                boolean inDepartment = lecturer.getDepartmentId() != null
                        && lecturer.getDepartmentId() == existing.getDepartmentId();
                boolean isHead = currentHead != null && currentHead.userId() == lecturer.getUserId();
                if (inDepartment && (lecturer.isActive() || isHead)) {
                    headCombo.addItem(lecturer);
                    if (isHead) {
                        headCombo.setSelectedItem(lecturer);
                    }
                }
            }
        }
        headCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = "-- No head assigned --";
                if (value instanceof User u) {
                    text = u.getFullName() + (u.isActive() ? "" : " - inactive");
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        addRow(0, "Department code * (e.g. DICT)", codeField);
        addRow(2, "Department name *", nameField);
        addRow(4, "Head of department", headCombo);

        JLabel note = new JLabel(headNote());
        note.setForeground(AdminTheme.TEXT_MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 6, 4, 6);
        add(note, c);

        headCombo.setEnabled(existing != null && headsAvailable);
        if (existing != null) {
            codeField.setText(existing.getDepartmentCode());
            nameField.setText(existing.getDepartmentName());
        }
    }

    private String headNote() {
        if (!headsAvailable) {
            return "Head of department needs database/migrations/002_department_head.sql";
        }
        return existing == null ? "Add lecturers to the department first, then choose its head here."
                : "Only active lecturers of this department can be chosen.";
    }

    private void addRow(int gridy, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(6, 6, 0, 6);
        add(new JLabel(label), c);
        c.gridy = gridy + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(2, 6, 4, 6);
        add(field, c);
    }

    /** Saves through the service; returns false (after showing the message) if it was refused. */
    boolean save() {
        try {
            Department department = new Department();
            department.setDepartmentId(existing == null ? 0 : existing.getDepartmentId());
            department.setDepartmentCode(codeField.getText());
            department.setDepartmentName(nameField.getText());
            if (existing == null) {
                departmentService.addDepartment(department);
            } else if (headsAvailable) {
                User head = (User) headCombo.getSelectedItem();
                departmentService.updateDepartment(department, head == null ? null : head.getUserId());
            } else {
                departmentService.updateDepartment(department);
            }
            return true;
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
            return false;
        }
    }

    static boolean showDialog(Component parent, DepartmentService departmentService, UserService userService,
                              Department existing, DepartmentHead currentHead, boolean headsAvailable) {
        DepartmentFormPanel form;
        try {
            form = new DepartmentFormPanel(departmentService, userService, existing, currentHead, headsAvailable);
        } catch (DatabaseException | AuthorizationException e) {
            UIUtil.showException(parent, e);
            return false;
        }
        return FormDialog.show(parent,
                existing == null ? "Add Department" : "Edit Department " + existing.getDepartmentCode(),
                form, existing == null ? "Add department" : "Save changes", form::save);
    }
}
