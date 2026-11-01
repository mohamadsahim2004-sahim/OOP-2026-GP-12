package com.facultyams.ui.lecturer;

import com.facultyams.dao.LecturerAccessDAO.AttendanceRecord;
import com.facultyams.model.Course;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.LecturerService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Attendance of the students of an assigned course; select a student for the session records. */
public class LecturerAttendancePanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient LecturerService service;
    private final DefaultTableModel summary = LecturerUi.readOnlyModel(
            "Reg. No", "Name", "Present", "Absent", "Approved medical", "Attendance %", "Meets minimum");
    private final DefaultTableModel records = LecturerUi.readOnlyModel(
            "Component", "Session", "Date", "Status", "Medical");
    private final JTable summaryTable = LecturerUi.styledTable(summary);
    private final List<Integer> studentIds = new ArrayList<>();
    /** Theory only / practical only / both combined (PDF: attendance for each component and combined). */
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Combined (theory + practical)", "Theory only", "Practical only"});

    public LecturerAttendancePanel() {
        this(new LecturerAccessService(), new LecturerService());
    }

    public LecturerAttendancePanel(LecturerAccessService access, LecturerService service) {
        super(access);
        this.service = service;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(new JLabel("Show:"));
        toolbar.add(typeCombo);
        typeCombo.addActionListener(e -> {
            if (selectedCourse() != null) {
                loadCourse(selectedCourse());
            }
        });
        toolbar.add(refreshButton);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(summaryTable), new JScrollPane(LecturerUi.styledTable(records)));
        split.setResizeWeight(0.6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        summaryTable.getSelectionModel().addListSelectionListener(e -> {
            int row = summaryTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                loadRecords(studentIds.get(summaryTable.convertRowIndexToModel(row)));
            }
        });
    }

    @Override
    protected void clearContent() {
        summary.setRowCount(0);
        records.setRowCount(0);
        studentIds.clear();
    }

    @Override
    protected void loadCourse(Course course) {
        String type = selectedType();
        LecturerUi.load(this, status, () -> service.getAttendance(course.getCourseId(), type), view -> {
            summary.setRowCount(0);
            records.setRowCount(0);
            studentIds.clear();
            for (LecturerService.AttendanceRow r : view.rows()) {
                AcademicRules.AttendanceInfo a = r.info();
                studentIds.add(r.student().studentId());
                summary.addRow(new Object[]{r.student().registrationNo(), r.student().fullName(),
                        a.present(), a.absent(), a.approvedMedical(),
                        a.recorded() ? LecturerUi.num(a.percent()) : "Not recorded",
                        a.recorded() ? LecturerUi.yesNo(a.meetsMinimum()) : "-"});
            }
            status.setText(view.rows().isEmpty()
                    ? "No students are enrolled in this course yet."
                    : view.sessionsExpected() == 0
                    ? "No " + typeCombo.getSelectedItem().toString().toLowerCase() + " attendance sessions exist for this course."
                    : view.rows().size() + " student(s); " + view.sessionsExpected()
                    + " sessions expected. Select a student to see the session records.");
        });
    }

    /** "THEORY", "PRACTICAL" or null for combined. */
    private String selectedType() {
        return switch (typeCombo.getSelectedIndex()) {
            case 1 -> "THEORY";
            case 2 -> "PRACTICAL";
            default -> null;
        };
    }

    private void loadRecords(int studentId) {
        Course course = selectedCourse();
        LecturerUi.load(this, status, () -> service.getAttendanceRecords(course.getCourseId(), studentId), list -> {
            records.setRowCount(0);
            for (AttendanceRecord r : list) {
                records.addRow(new Object[]{r.component(), r.sessionNo(), r.date(), r.status(),
                        r.medicalStatus() == null ? "-" : r.medicalStatus()});
            }
            status.setText(list.isEmpty() ? "No sessions recorded for this student." : list.size() + " session(s).");
        });
    }
}
