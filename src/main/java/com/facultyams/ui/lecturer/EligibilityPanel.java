package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.ResultService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/**
 * Exam eligibility from real data: CA % (from the CA, assignment and practical marks) and
 * attendance % (approved medical absences count as attended) for every student of the course.
 */
public class EligibilityPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient ResultService service;
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Reg. No", "Name", "Batch", "CA %", "Attendance %", "Eligibility", "Reason");
    private final javax.swing.JTextField searchField = new javax.swing.JTextField(14);
    private final javax.swing.JComboBox<String> batchCombo = new javax.swing.JComboBox<>(new String[]{"All batches"});
    private final javax.swing.JTable table = LecturerUi.styledTable(model);
    private final JLabel rule = new JLabel(" ");

    public EligibilityPanel() {
        this(new LecturerAccessService(), new ResultService());
    }

    public EligibilityPanel(LecturerAccessService access, ResultService service) {
        super(access);
        this.service = service;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(new JLabel("Batch:"));
        toolbar.add(batchCombo);
        toolbar.add(new JLabel("Student (reg. no. or name):"));
        toolbar.add(searchField);
        LecturerUi.installFilter(table, searchField, batchCombo, 2);
        toolbar.add(refreshButton);
        rule.setText("Rule: CA at least " + AcademicRules.fmt(AcademicRules.CA_MIN_PERCENT)
                + "% and attendance at least " + AcademicRules.fmt(AcademicRules.ATTENDANCE_MIN_PERCENT) + "%.");

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(rule, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    @Override
    protected void clearContent() {
        model.setRowCount(0);
        LecturerUi.fillBatches(batchCombo, java.util.List.of());
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.getEligibility(course.getCourseId()), sheet -> {
            model.setRowCount(0);
            java.util.Set<String> batches = new java.util.TreeSet<>();
            int eligible = 0;
            int notEligible = 0;
            int pending = 0;
            for (ResultService.EligibilityRow r : sheet.rows()) {
                AcademicRules.Eligibility e = r.eligibility();
                switch (e.status()) {
                    case ELIGIBLE -> eligible++;
                    case NOT_ELIGIBLE -> notEligible++;
                    default -> pending++;
                }
                batches.add(r.student().batch());
                model.addRow(new Object[]{r.student().registrationNo(), r.student().fullName(), r.student().batch(),
                        LecturerUi.num(e.caPercent()),
                        e.attendance().recorded() ? LecturerUi.num(e.attendance().percent()) : "Not recorded",
                        e.status().label(), e.reason()});
            }
            LecturerUi.fillBatches(batchCombo, batches);
            String schemeNote = sheet.scheme().complete() ? "" : "  Evaluation scheme incomplete: " + sheet.scheme().problem();
            status.setText(sheet.rows().isEmpty()
                    ? "No students are enrolled in this course yet."
                    : eligible + " eligible, " + notEligible + " not eligible, " + pending + " pending." + schemeNote);
        });
    }
}
