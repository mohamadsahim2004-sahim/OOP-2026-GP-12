package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.ResultService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * CA marks summary of a course: one column per CA / assignment / practical component, the CA % and whether it
 * reaches the CA minimum. Shows the whole batch; the search box and batch box narrow it down to one student.
 */
public class CaSummaryPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient ResultService service;
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> batchCombo = new JComboBox<>(new String[]{"All batches"});
    private final JLabel rule = new JLabel(" ");
    private DefaultTableModel model = LecturerUi.readOnlyModel("Reg. No", "Name", "Batch", "CA %", "CA eligible");
    private JTable table = LecturerUi.styledTable(model);
    private final JScrollPane scroll = new JScrollPane(table);

    public CaSummaryPanel() {
        this(new LecturerAccessService(), new ResultService());
    }

    public CaSummaryPanel(LecturerAccessService access, ResultService service) {
        super(access);
        this.service = service;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(new JLabel("Batch:"));
        toolbar.add(batchCombo);
        toolbar.add(new JLabel("Student (reg. no. or name):"));
        toolbar.add(searchField);
        toolbar.add(refreshButton);
        rule.setText("CA part = CA + assignment + practical components. Eligible for the final exam when CA is at least "
                + AcademicRules.fmt(AcademicRules.CA_MIN_PERCENT) + "%.");
        add(scroll, BorderLayout.CENTER);
        // CoursePanel holds the toolbar (NORTH) and the status line (SOUTH): put the rule above the status line
        remove(status);
        javax.swing.JPanel south = new javax.swing.JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(rule, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
        install();
    }

    private void install() {
        LecturerUi.installFilter(table, searchField, batchCombo, 2);
    }

    @Override
    protected void clearContent() {
        model.setRowCount(0);
        LecturerUi.fillBatches(batchCombo, List.of());
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.getCaSummary(course.getCourseId()), summary -> {
            List<String> columns = new ArrayList<>(List.of("Reg. No", "Name", "Batch"));
            for (EvaluationComponent c : summary.parts()) {
                columns.add(c.getComponentName() + " (/" + AcademicRules.fmt(c.getMaxMarks()) + ", "
                        + AcademicRules.fmt(c.getWeightage()) + "%)");
            }
            columns.add("CA %");
            columns.add("CA eligible");
            model = LecturerUi.readOnlyModel(columns.toArray(new String[0]));
            table.setModel(model);
            table.setRowSorter(new javax.swing.table.TableRowSorter<>(model));
            install();
            searchField.setText("");

            Set<String> batches = new TreeSet<>();
            int eligible = 0;
            int notEligible = 0;
            int pending = 0;
            for (ResultService.CaRow r : summary.rows()) {
                List<Object> row = new ArrayList<>();
                row.add(r.student().registrationNo());
                row.add(r.student().fullName());
                row.add(r.student().batch());
                batches.add(r.student().batch());
                for (EvaluationComponent c : summary.parts()) {
                    row.add(LecturerUi.num(r.marks().get(c.getEvalComponentId())));
                }
                row.add(LecturerUi.num(r.caPercent()));
                if (r.caPercent() == null) {
                    row.add("Pending (marks missing)");
                    pending++;
                } else if (r.caEligible()) {
                    row.add("Eligible");
                    eligible++;
                } else {
                    row.add("Not eligible");
                    notEligible++;
                }
                model.addRow(row.toArray());
            }
            LecturerUi.fillBatches(batchCombo, batches);
            status.setText(summary.rows().isEmpty()
                    ? "No students are enrolled in this course yet."
                    : summary.parts().isEmpty()
                    ? "This course has no CA components yet. Add them under Evaluation Components."
                    : eligible + " CA eligible, " + notEligible + " not eligible, " + pending + " pending.");
        });
    }
}
