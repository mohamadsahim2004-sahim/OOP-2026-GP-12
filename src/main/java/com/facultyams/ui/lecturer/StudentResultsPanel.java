package com.facultyams.ui.lecturer;

import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.MarksDAO.StudentCourseResult;
import com.facultyams.service.GPAService;
import com.facultyams.service.ResultService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Student academic results: search the students of your own courses (registration number or
 * name), view their results in your courses, and see SGPA / CGPA.
 */
public class StudentResultsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient ResultService service;
    private final JTextField searchField = new JTextField(18);
    private final JLabel status = new JLabel("Enter a registration number or part of a name and click Search.");
    private final DefaultTableModel matches = LecturerUi.readOnlyModel("Reg. No", "Name", "Batch", "Status");
    private final DefaultTableModel results = LecturerUi.readOnlyModel(
            "Course", "Credits", "CA %", "Final %", "Overall", "Grade", "Grade points", "Status");
    private final JTextArea gpaArea = new JTextArea(6, 40);
    private final javax.swing.JTable matchTable = LecturerUi.styledTable(matches);
    private final List<StudentRow> found = new ArrayList<>();

    public StudentResultsPanel() {
        this(new ResultService());
    }

    public StudentResultsPanel(ResultService service) {
        super(new BorderLayout(0, 8));
        this.service = service;
        JButton searchButton = new JButton("Search");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(new JLabel("Student:"));
        bar.add(searchField);
        bar.add(searchButton);
        searchButton.addActionListener(e -> search());
        searchField.addActionListener(e -> search());

        matchTable.getSelectionModel().addListSelectionListener(e -> {
            int row = matchTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                show(found.get(matchTable.convertRowIndexToModel(row)));
            }
        });

        gpaArea.setEditable(false);
        gpaArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel lower = new JPanel(new BorderLayout(0, 6));
        lower.setOpaque(false);
        lower.add(new JScrollPane(LecturerUi.styledTable(results)), BorderLayout.CENTER);
        lower.add(new JScrollPane(gpaArea), BorderLayout.SOUTH);

        JScrollPane matchScroll = new JScrollPane(matchTable);
        matchScroll.setPreferredSize(new java.awt.Dimension(400, 110));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, matchScroll, lower);
        split.setResizeWeight(0.2);
        split.setBorder(null);

        add(bar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        // nothing is loaded until the lecturer searches
    }

    private void search() {
        String text = searchField.getText();
        LecturerUi.load(this, status, () -> service.searchStudents(text), list -> {
            found.clear();
            found.addAll(list);
            matches.setRowCount(0);
            results.setRowCount(0);
            gpaArea.setText("");
            for (StudentRow s : list) {
                matches.addRow(new Object[]{s.registrationNo(), s.fullName(), s.batch(), s.status()});
            }
            if (list.isEmpty()) {
                status.setText("No student of your courses matches \"" + text.trim() + "\".");
            } else if (list.size() == 1) {
                matchTable.setRowSelectionInterval(0, 0);       // triggers show()
            } else {
                status.setText(list.size() + " students found - select one to view the results.");
            }
        });
    }

    private void show(StudentRow student) {
        LecturerUi.load(this, status, () -> service.getStudentResults(student), data -> {
            results.setRowCount(0);
            for (StudentCourseResult r : data.myCourses()) {
                boolean calculated = r.overall() != null;
                results.addRow(new Object[]{r.courseCode() + " - " + r.courseName(), r.credits(),
                        calculated ? LecturerUi.num(r.caPercent()) : "-",
                        calculated ? LecturerUi.num(r.finalPercent()) : "-",
                        LecturerUi.num(r.overall()), r.grade() == null ? "-" : r.grade(),
                        LecturerUi.num(r.gradePoints()),
                        !calculated ? "Incomplete" : r.published() ? "Published" : "Draft"});
            }
            gpaArea.setText(describe(student, data.gpa(), data.myCourses().isEmpty()));
            gpaArea.setCaretPosition(0);
            status.setText(student.registrationNo() + " - " + student.fullName());
        });
    }

    private static String describe(StudentRow student, GPAService.GpaSummary gpa, boolean noResultsInMyCourses) {
        StringBuilder sb = new StringBuilder();
        sb.append(student.registrationNo()).append(" - ").append(student.fullName()).append('\n');
        if (noResultsInMyCourses) {
            sb.append("No results have been saved for this student in your courses yet.\n");
        }
        if (gpa.cgpa() == null) {
            sb.append("GPA: not available - no published results.\n");
        } else {
            for (GPAService.SemesterGpa s : gpa.semesters()) {
                sb.append(s.label()).append(":  SGPA ").append(String.format("%.2f", s.sgpa()))
                        .append("  (").append(s.credits()).append(" credits)\n");
            }
            sb.append("CGPA: ").append(String.format("%.2f", gpa.cgpa()))
                    .append("  (").append(gpa.creditsCounted()).append(" credits)\n");
        }
        if (gpa.draftResults() > 0) {
            sb.append(gpa.draftResults()).append(" unpublished result(s) are not included in the GPA.\n");
        }
        sb.append("GPA = sum(grade points x credits) / sum(credits), published results only.");
        return sb.toString();
    }
}
