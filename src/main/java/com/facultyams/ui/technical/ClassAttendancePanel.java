package com.facultyams.ui.technical;

import com.facultyams.model.ComponentInfo;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.SheetRow;
import com.facultyams.service.AttendanceService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.Display;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Attendance Management: choose course, subject (theory / practical), batch, session and date, load the
 * students, mark each Present / Absent / Late and save. Saving a session that already exists updates it, so
 * duplicate records cannot be created (the database also enforces one row per student, subject and session).
 */
public class ClassAttendancePanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;
    private static final String[] STATUS_LABELS = {"Present", "Absent", "Late"};
    private static final int STATUS_COLUMN = 3;

    /** What the loaded table belongs to; saving uses this, not the (possibly changed) drop-downs. */
    private record Context(int componentId, int sessionNo, String batch, String description) {
    }

    private record Lookups(List<CourseInfo> courses, List<String> batches) {
    }

    private final transient AttendanceService service = new AttendanceService();
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<Object> subjectBox = new JComboBox<>();
    private final JComboBox<Object> batchBox = new JComboBox<>();
    private final JComboBox<Integer> sessionBox = new JComboBox<>();
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final HintField searchField = new HintField("Search student or reg no", 16);
    private final ReadOnlyTableModel model = new ReadOnlyTableModel(
            new String[] {"Reg No", "Student", "Batch", "Status", "Saved", "Medical"},
            new Class<?>[] {String.class, String.class, String.class, String.class, String.class, String.class},
            STATUS_COLUMN);
    private final JTable table = table(model);
    private final JLabel contextLabel = heading(" ");
    private final JLabel totals = label(" ");

    private transient List<SheetRow> sheet = List.of();
    private transient Context context;
    private boolean fillingLookups;

    public ClassAttendancePanel() {
        for (int i = 1; i <= AttendanceService.TOTAL_SESSIONS; i++) {
            sessionBox.addItem(i);
        }
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        dateSpinner.setToolTipText("Session date (yyyy-MM-dd); cannot be in the future");
        courseBox.setPrototypeDisplayValue("ICT2113 - Data Structures and Algorithms");
        subjectBox.setPrototypeDisplayValue("Select a subject");
        batchBox.setPrototypeDisplayValue("Select a batch");
        courseBox.addActionListener(e -> {
            if (!fillingLookups) {
                loadSubjects();
            }
        });

        JButton loadButton = button("Load Students", "Show the students of the chosen batch for this session",
                KeyEvent.VK_L, this::loadStudents);
        JPanel selectors = filterBar();
        selectors.add(labeled("Course", courseBox));
        selectors.add(labeled("Subject", subjectBox));
        selectors.add(labeled("Batch", batchBox));
        selectors.add(labeled("Session", sessionBox));
        selectors.add(labeled("Date", dateSpinner));
        selectors.add(loadButton);

        JButton allPresent = button("Mark All Present", "Mark every student currently listed as present", KeyEvent.VK_P,
                () -> markVisible("Present"));
        JButton allAbsent = button("Mark All Absent", "Mark every student currently listed as absent", KeyEvent.VK_A,
                () -> markVisible("Absent"));
        JButton clear = button("Clear Search", "Show all students again", 0, () -> searchField.setText(""));
        searchField.getDocument().addDocumentListener((SimpleDocumentListener) this::applySearch);
        JPanel tools = filterBar();
        tools.add(searchField);
        tools.add(clear);
        tools.add(allPresent);
        tools.add(allAbsent);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(selectors);
        top.add(tools);
        top.add(statusArea());
        top.add(contextLabel);

        JComboBox<String> editor = new JComboBox<>(STATUS_LABELS);
        table.getColumnModel().getColumn(STATUS_COLUMN).setCellEditor(new DefaultCellEditor(editor));
        table.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        model.addTableModelListener(e -> updateTotals());

        JButton save = button("Save Attendance", "Save or update the attendance of this session", KeyEvent.VK_S, this::save);
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        bottom.add(totals, BorderLayout.CENTER);
        JPanel saveWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        saveWrap.add(save);
        bottom.add(saveWrap, BorderLayout.EAST);

        alignLeft(top);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        load("Loading courses and batches...", () -> new Lookups(service.getCourses(), service.getBatches()), l -> {
            fillingLookups = true;
            fillCombo(courseBox, "Select a course", l.courses());
            fillCombo(batchBox, "Select a batch", l.batches());
            fillingLookups = false;
            loadSubjects();
            if (l.courses().isEmpty()) {
                showInfo("No courses exist yet. Ask an administrator to add courses first.");
            }
        });
    }

    private void loadSubjects() {
        CourseInfo course = selectedOf(courseBox, CourseInfo.class);
        if (course == null) {
            fillCombo(subjectBox, "Select a course first", List.of());
            return;
        }
        load("Preparing the subjects of " + course.code() + "...", () -> service.getComponents(course.courseId()), list -> {
            fillCombo(subjectBox, "Select a subject", list);
            if (list.size() == 1) {
                subjectBox.setSelectedIndex(1);
            }
        });
    }

    private void loadStudents() {
        CourseInfo course = selectedOf(courseBox, CourseInfo.class);
        ComponentInfo subject = selectedOf(subjectBox, ComponentInfo.class);
        String batch = batchBox.getSelectedIndex() > 0 ? String.valueOf(batchBox.getSelectedItem()) : null;
        if (course == null || subject == null || batch == null) {
            showError("Select a course, a subject and a batch first.");
            return;
        }
        int session = (Integer) sessionBox.getSelectedItem();
        Context ctx = new Context(subject.componentId(), session, batch,
                course.code() + " " + subject + "  |  Batch " + batch + "  |  Session " + session);
        load("Loading students...", () -> service.loadSheet(ctx.componentId(), ctx.sessionNo(), ctx.batch()), rows -> {
            populate(rows, ctx);
            long recorded = rows.stream().filter(SheetRow::isRecorded).count();
            if (rows.isEmpty()) {
                showInfo("There are no active students in batch " + batch + ".");
            } else if (recorded > 0) {
                showInfo(recorded + " of " + rows.size() + " students already have attendance for this session. "
                        + "Saving will update it.");
            } else {
                showInfo("No attendance saved yet for this session. Everyone starts as Present; change the exceptions.");
            }
        });
    }

    private void populate(List<SheetRow> rows, Context ctx) {
        sheet = rows;
        context = ctx;
        contextLabel.setText("Marking: " + ctx.description());
        List<Object[]> data = new ArrayList<>();
        for (SheetRow r : rows) {
            data.add(new Object[] {r.registrationNo(), r.fullName(), r.batch(),
                    r.isRecorded() ? Display.capitalize(r.status()) : "Present",
                    r.isRecorded() ? Display.capitalize(r.status()) : "Not recorded",
                    r.hasMedical() ? "Yes" : ""});
            if (r.isRecorded() && r.date() != null) {
                dateSpinner.setValue(Date.from(r.date().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
        }
        model.setRows(data);
        updateTotals();
    }

    private void applySearch() {
        @SuppressWarnings("unchecked")
        TableRowSorter<ReadOnlyTableModel> sorter = (TableRowSorter<ReadOnlyTableModel>) table.getRowSorter();
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 0, 1));
    }

    private void markVisible(String status) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        for (int view = 0; view < table.getRowCount(); view++) {
            model.setValueAt(status, table.convertRowIndexToModel(view), STATUS_COLUMN);
        }
    }

    private void updateTotals() {
        int present = 0;
        int absent = 0;
        int late = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            switch (String.valueOf(model.getValueAt(r, STATUS_COLUMN))) {
                case "Present" -> present++;
                case "Absent" -> absent++;
                default -> late++;
            }
        }
        totals.setText(model.getRowCount() == 0 ? "No students loaded"
                : "Present " + present + "   |   Absent " + absent + "   |   Late " + late
                  + "   |   Total " + model.getRowCount());
    }

    private LocalDate selectedDate() {
        return LocalDate.ofInstant(((Date) dateSpinner.getValue()).toInstant(), ZoneId.systemDefault());
    }

    private void save() {
        if (context == null || sheet.isEmpty()) {
            showError("Load the students of a session first.");
            return;
        }
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        Map<Integer, String> statuses = new LinkedHashMap<>();
        int present = 0;
        int absent = 0;
        int late = 0;
        int medicalConflicts = 0;
        boolean anyRecorded = false;
        for (int r = 0; r < model.getRowCount(); r++) {
            String status = String.valueOf(model.getValueAt(r, STATUS_COLUMN)).toLowerCase(Locale.ROOT);
            SheetRow row = sheet.get(r);
            statuses.put(row.userId(), status);
            anyRecorded |= row.isRecorded();
            switch (status) {
                case AttendanceService.PRESENT -> present++;
                case AttendanceService.ABSENT -> absent++;
                default -> late++;
            }
            if (row.hasMedical() && !AttendanceService.ABSENT.equals(status)) {
                medicalConflicts++;
            }
        }
        LocalDate date = selectedDate();
        StringBuilder msg = new StringBuilder("Save attendance for " + statuses.size() + " students ("
                + present + " present, " + absent + " absent, " + late + " late)\n"
                + context.description() + "\nDate: " + date);
        if (anyRecorded) {
            msg.append("\n\nExisting records of this session will be updated.");
        }
        if (medicalConflicts > 0) {
            msg.append("\n\n").append(medicalConflicts).append(" student(s) with a medical are no longer marked Absent; "
                    + "their medical will stop counting.");
        }
        if (!UIUtil.confirm(this, msg.toString())) {
            return;
        }
        Context ctx = context;
        act("Saving attendance...",
                () -> service.saveSession(ctx.componentId(), ctx.sessionNo(), ctx.batch(), date, statuses),
                saved -> load("Reloading the saved attendance...",
                        () -> service.loadSheet(ctx.componentId(), ctx.sessionNo(), ctx.batch()),
                        rows -> {
                            populate(rows, ctx);
                            showSuccess("Attendance saved for " + saved + " students.");
                        }));
    }
}