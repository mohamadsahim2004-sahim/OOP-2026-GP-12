package com.facultyams.ui.lecturer;

import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.MarksDAO.StudentCourseResult;
import com.facultyams.service.GPAService;
import com.facultyams.service.ResultService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * SGPA and CGPA for the whole batch: one row per student of the lecturer's courses. Select a student to see
 * that student's subjects, grades and the SGPA of every semester. The search and batch boxes narrow the list.
 */
public class BatchGpaPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient ResultService service;
    private final JLabel status = new JLabel("Opening this page loads the SGPA / CGPA of your students.");
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> batchCombo = new JComboBox<>(new String[]{"All batches"});
    private final DefaultTableModel batchModel = LecturerUi.readOnlyModel(
            "Reg. No", "Name", "Batch", "Credits counted", "Latest SGPA", "CGPA");
    private final DefaultTableModel detailModel = LecturerUi.readOnlyModel(
            "Course", "Credits", "CA %", "Final %", "Overall", "Grade", "Grade points", "Status");
    private final JTable batchTable = LecturerUi.styledTable(batchModel);
    private final JTextArea gpaArea = new JTextArea(6, 40);
    private final List<StudentRow> students = new ArrayList<>();

    public BatchGpaPanel() {
        this(new ResultService());
    }

    public BatchGpaPanel(ResultService service) {
        super(new BorderLayout(0, 8));
        this.service = service;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(new JLabel("Batch:"));
        bar.add(batchCombo);
        bar.add(new JLabel("Student (reg. no. or name):"));
        bar.add(searchField);
        bar.add(refreshButton);
        LecturerUi.installFilter(batchTable, searchField, batchCombo, 2);

        batchTable.getSelectionModel().addListSelectionListener(e -> {
            int row = batchTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                show(students.get(batchTable.convertRowIndexToModel(row)));
            }
        });

        gpaArea.setEditable(false);
        gpaArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel lower = new JPanel(new BorderLayout(0, 6));
        lower.setOpaque(false);
        lower.add(new JScrollPane(LecturerUi.styledTable(detailModel)), BorderLayout.CENTER);
        lower.add(new JScrollPane(gpaArea), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(batchTable), lower);
        split.setResizeWeight(0.5);
        split.setBorder(null);

        add(bar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        LecturerUi.load(this, status, service::getBatchGpa, rows -> {
            students.clear();
            batchModel.setRowCount(0);
            detailModel.setRowCount(0);
            gpaArea.setText("");
            Set<String> batches = new TreeSet<>();
            int withGpa = 0;
            for (ResultService.BatchGpaRow r : rows) {
                GPAService.GpaSummary g = r.gpa();
                students.add(r.student());
                batches.add(r.student().batch());
                List<GPAService.SemesterGpa> sems = g.semesters();
                String latest = sems.isEmpty() ? "-" : String.format("%.2f", sems.get(sems.size() - 1).sgpa());
                if (g.cgpa() != null) {
                    withGpa++;
                }
                batchModel.addRow(new Object[]{r.student().registrationNo(), r.student().fullName(),
                        r.student().batch(), g.creditsCounted(), latest,
                        g.cgpa() == null ? "-" : String.format("%.2f", g.cgpa())});
            }
            LecturerUi.fillBatches(batchCombo, batches);
            status.setText(rows.isEmpty()
                    ? "None of your courses has an enrolled student yet."
                    : rows.size() + " student(s), " + withGpa + " with a published GPA. Select a student for the subjects and grades.");
        });
    }

    private void show(StudentRow student) {
        LecturerUi.load(this, status, () -> service.getStudentResults(student), data -> {
            detailModel.setRowCount(0);
            for (StudentCourseResult r : data.myCourses()) {
                boolean calculated = r.overall() != null;
                detailModel.addRow(new Object[]{r.courseCode() + " - " + r.courseName(), r.credits(),
                        calculated ? LecturerUi.num(r.caPercent()) : "-",
                        calculated ? LecturerUi.num(r.finalPercent()) : "-",
                        LecturerUi.num(r.overall()), r.grade() == null ? "-" : r.grade(),
                        LecturerUi.num(r.gradePoints()),
                        !calculated ? "Incomplete" : r.published() ? "Published" : "Draft"});
            }
            gpaArea.setText(StudentResultsPanel.describe(student, data.gpa(), data.myCourses().isEmpty()));
            gpaArea.setCaretPosition(0);
            status.setText(student.registrationNo() + " - " + student.fullName());
        });
    }
}
