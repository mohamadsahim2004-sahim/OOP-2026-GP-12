package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.model.CourseMaterial;
import com.facultyams.service.CourseMaterialService;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.ui.admin.Refreshable;
import com.facultyams.util.UIUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Add, edit and list the course materials of the lecturer's own courses. */
public class CourseMaterialsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient CourseMaterialService service;
    private final transient LecturerAccessService access;
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Material ID", "Course", "Description", "Notes", "References");
    private final JTable table = LecturerUi.styledTable(model);
    private final JLabel status = new JLabel(" ");
    private final List<CourseMaterial> materials = new ArrayList<>();
    private final List<Course> courses = new ArrayList<>();

    public CourseMaterialsPanel() {
        this(new LecturerAccessService(), new CourseMaterialService());
    }

    public CourseMaterialsPanel(LecturerAccessService access, CourseMaterialService service) {
        super(new BorderLayout(0, 8));
        this.access = access;
        this.service = service;
        JButton add = new JButton("Add Material");
        JButton edit = new JButton("Edit Selected");
        JButton refresh = new JButton("Refresh");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(add);
        bar.add(edit);
        bar.add(refresh);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add.addActionListener(e -> showForm(null));
        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                UIUtil.showInfo(this, "Select a material in the table first.");
            } else {
                showForm(materials.get(table.convertRowIndexToModel(row)));
            }
        });
        refresh.addActionListener(e -> refresh());
    }

    @Override
    public void refresh() {
        LecturerUi.load(this, status, () -> new Object[]{access.getAssignedCourses(), service.getMyMaterials()},
                data -> {
                    courses.clear();
                    materials.clear();
                    @SuppressWarnings("unchecked")
                    List<Course> cs = (List<Course>) data[0];
                    @SuppressWarnings("unchecked")
                    List<CourseMaterial> ms = (List<CourseMaterial>) data[1];
                    courses.addAll(cs);
                    materials.addAll(ms);
                    Map<Integer, Course> byId = new HashMap<>();
                    for (Course c : cs) {
                        byId.put(c.getCourseId(), c);
                    }
                    model.setRowCount(0);
                    for (CourseMaterial m : ms) {
                        Course c = byId.get(m.getCourseID());
                        model.addRow(new Object[]{m.getMaterialID(), c == null ? m.getCourseID() : c.getCourseCode(),
                                m.getDescription(), m.getNotes(), m.getReferenceText()});
                    }
                    status.setText(ms.isEmpty() ? "No course materials yet. Click \"Add Material\"."
                            : ms.size() + " material(s).");
                });
    }

    private void showForm(CourseMaterial existing) {
        if (courses.isEmpty()) {
            UIUtil.showInfo(this, "You have no assigned courses to attach materials to.");
            return;
        }
        JComboBox<Course> course = new JComboBox<>(courses.toArray(new Course[0]));
        JTextField description = new JTextField(existing == null ? "" : existing.getDescription());
        JTextField notes = new JTextField(existing == null || existing.getNotes() == null ? "" : existing.getNotes());
        JTextField references = new JTextField(
                existing == null || existing.getReferenceText() == null ? "" : existing.getReferenceText());
        if (existing != null) {
            for (Course c : courses) {
                if (c.getCourseId() == existing.getCourseID()) {
                    course.setSelectedItem(c);
                }
            }
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course:"));
        form.add(course);
        form.add(new JLabel("Description:"));
        form.add(description);
        form.add(new JLabel("Notes:"));
        form.add(notes);
        form.add(new JLabel("References:"));
        form.add(references);
        if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Course Material" : "Edit Course Material",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        CourseMaterial material = new CourseMaterial(existing == null ? 0 : existing.getMaterialID(),
                ((Course) course.getSelectedItem()).getCourseId(), description.getText().trim(),
                notes.getText().trim(), references.getText().trim());
        LecturerUi.load(this, status, () -> {
            if (existing == null) {
                service.addMaterial(material);
            } else {
                service.updateMaterial(material);
            }
            return Boolean.TRUE;
        }, ok -> refresh());
    }
}
