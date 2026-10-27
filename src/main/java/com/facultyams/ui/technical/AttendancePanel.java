package com.facultyams.ui.technical;

import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.AttendanceRow;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.StudentSummaryRow;
import com.facultyams.service.AttendanceService;
import com.facultyams.util.Display;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Attendance Records / Summary. Two views of the same filters: the history of saved sessions, and the
 * per-student summary (present / late / absent / medical counts and attendance percentage). Both tables
 * can be sorted by clicking a column heading; the summary can open one student's session-by-session detail.
 */
public class AttendancePanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private record Lookups(List<CourseInfo> courses, List<String> batches) {
    }

    private record Result(List<AttendanceRow> history, List<StudentSummaryRow> summary) {
    }

    private final transient AttendanceService service = new AttendanceService();
    private final HintField studentField = new HintField("Student name or reg no", 14);
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<Object> batchBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>(new String[] {"All statuses", "Present", "Absent", "Late"});
    private final HintField fromField = new HintField("From (yyyy-MM-dd)", 9);
    private final HintField toField = new HintField("To (yyyy-MM-dd)", 9);

    private final ReadOnlyTableModel historyModel = ReadOnlyTableModel.ofStrings(
            "Date", "Reg No", "Student", "Batch", "Course", "Type", "Session", "Status", "Medical");
    private final ReadOnlyTableModel summaryModel = new ReadOnlyTableModel(
            new String[] {"Reg No", "Student", "Batch", "Course", "Type", "Present", "Late", "Absent",
                    "Medical", "Recorded", "Attendance %", "Eligible"},
            new Class<?>[] {String.class, String.class, String.class, String.class, String.class, Integer.class,
                    Integer.class, Integer.class, Integer.class, Integer.class, Double.class, String.class});
    private final JTable historyTable = table(historyModel);
    private final JTable summaryTable = table(summaryModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private transient List<StudentSummaryRow> summaryRows = List.of();
    private transient List<AttendanceRow> historyRows = List.of();
    private String pendingSuccess;
    private boolean fillingLookups;

    public AttendancePanel() {
        JButton search = button("Search", "Apply the filters", KeyEvent.VK_S, this::search);
        JButton reset = button("Reset", "Clear all filters", KeyEvent.VK_R, this::reset);
        JButton details = button("Student Details", "Show the sessions behind the selected summary row", KeyEvent.VK_D,
                this::showDetails);
        JButton correct = button("Correct Status", "Change the status of the selected attendance record", KeyEvent.VK_C,
                this::correctStatus);
        statusBox.setToolTipText("The status filter applies to the history tab only");
        studentField.addActionListener(e -> search());
        fromField.addActionListener(e -> search());
        toField.addActionListener(e -> search());

        JPanel filters = filterBar();
        filters.add(studentField);
        filters.add(labeled("Course", courseBox));
        filters.add(labeled("Batch", batchBox));
        filters.add(labeled("Status", statusBox));
        filters.add(fromField);
        filters.add(toField);
        filters.add(search);
        filters.add(reset);
        filters.add(details);
        filters.add(correct);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(filters);
        top.add(statusArea());

        historyTable.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());
        historyTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        summaryTable.getColumnModel().getColumn(10).setCellRenderer(new PercentRenderer());
        summaryTable.getColumnModel().getColumn(11).setCellRenderer(new StatusRenderer());

        tabs.addTab("Attendance History", new JScrollPane(historyTable));
        tabs.addTab("Student Summary", new JScrollPane(summaryTable));
        alignLeft(top);
        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        load("Loading filters...", () -> new Lookups(service.getCourses(), service.getBatches()), l -> {
            fillingLookups = true;
            fillCombo(courseBox, "All courses", l.courses());
            fillCombo(batchBox, "All batches", l.batches());
            fillingLookups = false;
            search();
        });
    }

    private void reset() {
        studentField.setText("");
        fromField.setText("");
        toField.setText("");
        statusBox.setSelectedIndex(0);
        courseBox.setSelectedIndex(0);
        batchBox.setSelectedIndex(0);
        search();
    }

    private AttendanceFilter currentFilter() throws com.facultyams.exception.ValidationException {
        CourseInfo course = selectedOf(courseBox, CourseInfo.class);
        String batch = batchBox.getSelectedIndex() > 0 ? String.valueOf(batchBox.getSelectedItem()) : null;
        String status = statusBox.getSelectedIndex() > 0
                ? String.valueOf(statusBox.getSelectedItem()).toLowerCase(java.util.Locale.ROOT) : null;
        return new AttendanceFilter(studentField.getText(), null, course == null ? null : course.courseId(), null,
                batch, parseDate(fromField.getText(), "The 'from' date"), parseDate(toField.getText(), "The 'to' date"),
                status);
    }

    private void search() {
        if (fillingLookups) {
            return;
        }
        AttendanceFilter f;
        try {
            f = currentFilter();
        } catch (com.facultyams.exception.ValidationException e) {
            showError(e.getMessage());
            return;
        }
        load("Loading attendance...", () -> new Result(service.getHistory(f), service.getSummary(f)), r -> {
            historyRows = r.history();
            List<Object[]> history = new ArrayList<>();
            for (AttendanceRow a : r.history()) {
                history.add(new Object[] {Display.date(a.date()), a.registrationNo(), a.fullName(), a.batch(),
                        a.courseCode(), Display.capitalize(a.type()), a.sessionNo(), Display.capitalize(a.status()),
                        a.medicalStatus() == null ? "" : Display.capitalize(a.medicalStatus())});
            }
            historyModel.setRows(history);
            summaryRows = r.summary();
            List<Object[]> summary = new ArrayList<>();
            for (StudentSummaryRow s : summaryRows) {
                summary.add(new Object[] {s.registrationNo(), s.fullName(), s.batch(), s.courseCode(),
                        Display.capitalize(s.type()), s.present(), s.late(), s.absent(), s.approvedMedical(),
                        s.recorded(), s.percentage(), Display.yesNo(s.eligible())});
            }
            summaryModel.setRows(summary);
            String basis = f.hasDateRange() ? "sessions recorded in the date range" : AttendanceService.TOTAL_SESSIONS + " sessions";
            if (history.isEmpty()) {
                showInfo("No attendance records match these filters.");
            } else {
                showInfo(history.size() + " attendance records, " + summary.size() + " student summaries. "
                        + "Attendance % = (present + late + approved medical) / " + basis + "."
                        + (history.size() >= AttendanceService.HISTORY_LIMIT ? " Showing the newest " + history.size() + " records only." : ""));
            }
            if (pendingSuccess != null) {
                showSuccess(pendingSuccess);
                pendingSuccess = null;
            }
        });
    }

    private void correctStatus() {
        int row = selectedModelRow(historyTable);
        if (row < 0) {
            tabs.setSelectedIndex(0);
            showError("Select an attendance record on the Attendance History tab first.");
            return;
        }
        AttendanceRow a = historyRows.get(row);
        String[] options = {"Present", "Absent", "Late"};
        Object choice = JOptionPane.showInputDialog(this,
                a.fullName() + " - " + a.courseCode() + " " + Display.capitalize(a.type()) + ", session " + a.sessionNo()
                        + " on " + Display.date(a.date()) + "\nCurrent status: " + Display.capitalize(a.status())
                        + "\n\nNew status:", "Correct attendance", JOptionPane.PLAIN_MESSAGE, null, options,
                Display.capitalize(a.status()));
        if (choice == null || choice.toString().equalsIgnoreCase(a.status())) {
            return;
        }
        String newStatus = choice.toString().toLowerCase(java.util.Locale.ROOT);
        act("Saving the correction...", () -> {
            service.correctStatus(a.attendanceId(), newStatus);
            return newStatus;
        }, st -> {
            pendingSuccess = "Attendance corrected to " + Display.capitalize(st) + ". Summaries were recalculated.";
            search();
        });
    }

    private void showDetails() {
        int row = selectedModelRow(summaryTable);
        if (row < 0) {
            tabs.setSelectedIndex(1);
            showError("Select a student on the Student Summary tab first.");
            return;
        }
        StudentSummaryRow s = summaryRows.get(row);
        AttendanceFilter f;
        try {
            AttendanceFilter base = currentFilter();
            f = new AttendanceFilter(null, s.userId(), null, s.componentId(), null, base.from(), base.to(), null);
        } catch (com.facultyams.exception.ValidationException e) {
            showError(e.getMessage());
            return;
        }
        load("Loading " + s.fullName() + "...", () -> service.getHistory(f), sessions -> {
            ReadOnlyTableModel m = ReadOnlyTableModel.ofStrings("Date", "Session", "Status", "Medical");
            for (AttendanceRow a : sessions) {
                m.addRow(new Object[] {Display.date(a.date()), String.valueOf(a.sessionNo()),
                        Display.capitalize(a.status()), a.medicalStatus() == null ? "" : Display.capitalize(a.medicalStatus())});
            }
            JTable t = table(m);
            t.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
            t.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
            com.facultyams.ui.admin.AdminTheme.styleTable(t);
            JLabel head = new JLabel("<html><b>" + escape(s.fullName()) + "</b> (" + escape(s.registrationNo()) + ")<br>"
                    + escape(s.courseCode() + " " + Display.capitalize(s.type())) + "<br>"
                    + "Present " + s.present() + " | Late " + s.late() + " | Absent " + s.absent()
                    + " (" + s.approvedMedical() + " with approved medical) | Recorded " + s.recorded() + "<br>"
                    + "<b>Attendance " + Display.percent(s.percentage()) + "</b> of " + s.basis() + " sessions - "
                    + (s.eligible() ? "eligible" : "NOT eligible") + "</html>");
            JScrollPane scroll = new JScrollPane(t);
            scroll.setPreferredSize(new Dimension(520, 260));
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.add(head, BorderLayout.NORTH);
            p.add(scroll, BorderLayout.CENTER);
            p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            JOptionPane.showMessageDialog(this, p, "Student attendance details", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}