package com.facultyams.ui.lecturer;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.EvaluationComponentService;
import com.facultyams.service.LecturerAccessService;
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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Evaluation components: create, view, edit and delete the assessment scheme of a course. */
public class EvaluationComponentsPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient EvaluationComponentService service;
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Component", "Category", "Max marks", "Weightage (%)");
    private final JTable table = LecturerUi.styledTable(model);
    private final JLabel summary = new JLabel(" ");
    private final JButton addButton = new JButton("Add Component");
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private transient EvaluationComponentService.Scheme scheme;
    private final List<EvaluationComponent> rows = new ArrayList<>();

    public EvaluationComponentsPanel() {
        this(new LecturerAccessService(), new EvaluationComponentService());
    }

    public EvaluationComponentsPanel(LecturerAccessService access, EvaluationComponentService service) {
        super(access);
        this.service = service;
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(refreshButton);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(summary, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        addButton.addActionListener(e -> showForm(null));
        editButton.addActionListener(e -> {
            EvaluationComponent selected = selectedComponent();
            if (selected != null) {
                showForm(selected);
            }
        });
        deleteButton.addActionListener(e -> delete());
        setButtonsEnabled(false);
    }

    @Override
    protected void clearContent() {
        model.setRowCount(0);
        rows.clear();
        summary.setText(" ");
        setButtonsEnabled(false);
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.getScheme(course.getCourseId()), s -> {
            scheme = s;
            model.setRowCount(0);
            rows.clear();
            for (EvaluationComponent c : s.components()) {
                rows.add(c);
                model.addRow(new Object[]{c.getComponentName(), c.getCategory(),
                        AcademicRules.fmt(c.getMaxMarks()), AcademicRules.fmt(c.getWeightage())});
            }
            summary.setText("Total weightage: " + AcademicRules.fmt(s.check().totalWeight()) + "% of 100%   |   "
                    + (s.check().complete() ? "Scheme complete - marks can be entered"
                    : "Incomplete: " + s.check().problem()));
            setButtonsEnabled(true);
            addButton.setEnabled(!s.structureLocked());
            deleteButton.setEnabled(!s.structureLocked());
            status.setText(s.components().isEmpty()
                    ? "No evaluation components yet. Click \"Add Component\" to create the first one."
                    : (s.published() ? "Results are published - the scheme is locked."
                    : s.marksEntered() > 0 ? "Marks have been entered: components can be renamed but not added, removed or re-weighted."
                    : s.components().size() + " component(s)."));
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private EvaluationComponent selectedComponent() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UIUtil.showInfo(this, "Select a component in the table first.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(viewRow));
    }

    private void delete() {
        EvaluationComponent selected = selectedComponent();
        if (selected == null
                || !UIUtil.confirm(this, "Delete component \"" + selected.getComponentName() + "\"?")) {
            return;
        }
        LecturerUi.load(this, status, () -> {
            service.deleteComponent(selected.getEvalComponentId());
            return Boolean.TRUE;
        }, ok -> loadCourse(selectedCourse()));
    }

    private void showForm(EvaluationComponent existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getComponentName(), 20);
        JComboBox<String> category = new JComboBox<>(new String[]{
                EvaluationComponent.CA, EvaluationComponent.ASSIGNMENT,
                EvaluationComponent.PRACTICAL, EvaluationComponent.FINAL_EXAM});
        if (existing != null) {
            category.setSelectedItem(existing.getCategory());
        }
        JTextField max = new JTextField(existing == null ? "100" : AcademicRules.fmt(existing.getMaxMarks()));
        JTextField weight = new JTextField(existing == null ? "" : AcademicRules.fmt(existing.getWeightage()));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Component name:"));
        form.add(name);
        form.add(new JLabel("Category:"));
        form.add(category);
        form.add(new JLabel("Maximum marks:"));
        form.add(max);
        form.add(new JLabel("Weightage (% of final mark):"));
        form.add(weight);

        int choice = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add Evaluation Component" : "Edit Evaluation Component",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        double maxMarks;
        double weightage;
        try {
            maxMarks = Double.parseDouble(max.getText().trim());
            weightage = Double.parseDouble(weight.getText().trim());
        } catch (NumberFormatException e) {
            UIUtil.showException(this, new ValidationException("Maximum marks and weightage must be numbers."));
            return;
        }
        Course course = selectedCourse();
        String componentName = name.getText();
        String categoryName = (String) category.getSelectedItem();
        LecturerUi.load(this, status, () -> {
            if (existing == null) {
                service.addComponent(course.getCourseId(), componentName, categoryName, maxMarks, weightage);
            } else {
                service.updateComponent(existing.getEvalComponentId(), componentName, categoryName,
                        maxMarks, weightage);
            }
            return Boolean.TRUE;
        }, ok -> loadCourse(course));
    }
}
