package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.MarksService;
import com.facultyams.util.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marks entry: one row per student, one editable column per evaluation component. "Save Changes"
 * saves every edited row; each row is validated and saved on its own, so one bad value never
 * blocks the others. Overall mark, grade and eligibility are calculated by the service.
 */
public class MarksEntryPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;
    private static final int FIXED_LEADING = 2;      // registration no, name

    private final transient MarksService service;
    private final JButton saveButton = new JButton("Save Changes");
    private final JLabel notice = new JLabel(" ");
    private final JScrollPane scroll = new JScrollPane();
    private final Set<Integer> dirtyRows = new HashSet<>();
    private transient MarksService.EntrySheet sheet;
    private DefaultTableModel model = new DefaultTableModel();
    private boolean loading;

    public MarksEntryPanel() {
        this(new LecturerAccessService(), new MarksService());
    }

    public MarksEntryPanel(LecturerAccessService access, MarksService service) {
        super(access);
        this.service = service;
        JButton reload = new JButton("Reload");
        reload.addActionListener(e -> refresh());
        toolbar.add(saveButton);
        toolbar.add(reload);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> save());

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(notice, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    @Override
    protected void clearContent() {
        scroll.setViewportView(null);
        notice.setText(" ");
        saveButton.setEnabled(false);
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.loadEntrySheet(course.getCourseId()), s -> {
            sheet = s;
            buildTable(s);
        });
    }

    private void buildTable(MarksService.EntrySheet s) {
        List<EvaluationComponent> comps = s.components();
        List<String> headers = new ArrayList<>(List.of("Reg. No", "Name"));
        for (EvaluationComponent c : comps) {
            headers.add(c.getComponentName() + " (/" + AcademicRules.fmt(c.getMaxMarks()) + ", "
                    + AcademicRules.fmt(c.getWeightage()) + "%)");
        }
        headers.addAll(List.of("Overall", "Grade", "Status"));
        boolean entryOpen = s.scheme().complete();
        int overallCol = FIXED_LEADING + comps.size();

        loading = true;
        dirtyRows.clear();
        model = new DefaultTableModel(headers.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return entryOpen && column >= FIXED_LEADING && column < overallCol
                        && !s.rows().get(row).published();
            }
        };
        for (MarksService.EntryRow r : s.rows()) {
            List<Object> cells = new ArrayList<>();
            cells.add(r.student().registrationNo());
            cells.add(r.student().fullName());
            for (EvaluationComponent c : comps) {
                Double mark = r.marks().get(c.getEvalComponentId());
                cells.add(mark == null ? "" : AcademicRules.fmt(mark));
            }
            cells.add(r.overall() == null ? "-" : LecturerUi.num(r.overall()));
            cells.add(r.grade() == null ? "-" : r.grade());
            cells.add(r.published() ? "Published (locked)"
                    : r.hasResult() ? "Calculated (draft)"
                    : r.marks().isEmpty() ? "No marks" : "Incomplete " + r.marks().size() + "/" + comps.size());
            model.addRow(cells.toArray());
        }
        model.addTableModelListener(e -> {
            int col = e.getColumn();
            if (!loading && e.getFirstRow() >= 0 && col >= FIXED_LEADING && col < overallCol) {
                dirtyRows.add(e.getFirstRow());
                saveButton.setEnabled(true);
            }
        });
        JTable table = LecturerUi.styledTable(model);
        // sorting by a column is fine: edits are tracked by MODEL row, which matches the sheet row
        scroll.setViewportView(table);
        loading = false;

        saveButton.setEnabled(false);
        notice.setText(entryOpen
                ? "Type marks into the cells, then click Save Changes. Leave a cell empty if the mark is not available yet."
                : "Marks cannot be entered yet: " + s.scheme().problem()
                + " Fix this in \"Evaluation Components\".");
        status.setText(s.rows().isEmpty()
                ? "No students are enrolled in this course yet."
                : s.rows().size() + " student(s).");
    }

    private void save() {
        if (table() != null && table().isEditing()) {
            table().getCellEditor().stopCellEditing();
        }
        Course course = selectedCourse();
        MarksService.EntrySheet current = sheet;
        List<EvaluationComponent> comps = current.components();
        List<Integer> rowsToSave = new ArrayList<>(new java.util.TreeSet<>(dirtyRows));
        // copy the cell text now, on the Swing thread; the background job must not touch the table
        Map<Integer, String[]> typed = new LinkedHashMap<>();
        for (int row : rowsToSave) {
            String[] texts = new String[comps.size()];
            for (int i = 0; i < texts.length; i++) {
                texts[i] = String.valueOf(cell(row, FIXED_LEADING + i)).trim();
            }
            typed.put(row, texts);
        }

        LecturerUi.load(this, status, () -> {
            List<String> problems = new ArrayList<>();
            int saved = 0;
            for (int row : rowsToSave) {
                MarksService.EntryRow entry = current.rows().get(row);
                String who = entry.student().registrationNo();
                Map<Integer, Double> values = new LinkedHashMap<>();
                boolean rowOk = true;
                for (int i = 0; i < comps.size(); i++) {
                    EvaluationComponent c = comps.get(i);
                    String text = typed.get(row)[i];
                    if (text.isEmpty()) {
                        if (entry.marks().containsKey(c.getEvalComponentId())) {
                            problems.add(who + " - " + c.getComponentName() + ": a saved mark cannot be cleared.");
                            rowOk = false;
                        }
                        continue;
                    }
                    try {
                        values.put(c.getEvalComponentId(), Double.parseDouble(text));
                    } catch (NumberFormatException ex) {
                        problems.add(who + " - " + c.getComponentName() + ": \"" + text + "\" is not a number.");
                        rowOk = false;
                    }
                }
                if (!rowOk || values.isEmpty()) {
                    continue;
                }
                try {
                    service.saveStudentMarks(course.getCourseId(), entry.student().studentId(), values);
                    saved++;
                } catch (com.facultyams.exception.ValidationException | com.facultyams.exception.AuthorizationException ex) {
                    problems.add(who + ": " + ex.getMessage());
                }
            }
            return new Object[]{saved, problems};
        }, result -> {
            int saved = (Integer) result[0];
            @SuppressWarnings("unchecked")
            List<String> problems = (List<String>) result[1];
            if (!problems.isEmpty()) {
                UIUtil.showError(this, "Saved " + saved + " student(s). Problems:\n\n"
                        + String.join("\n", problems.subList(0, Math.min(problems.size(), 12)))
                        + (problems.size() > 12 ? "\n... and " + (problems.size() - 12) + " more" : ""));
            } else {
                UIUtil.showInfo(this, "Marks saved for " + saved + " student(s).");
            }
            loadCourse(course);        // reload from the database so the table shows what is stored
        });
    }

    private Object cell(int row, int col) {
        Object v = model.getValueAt(row, col);
        return v == null ? "" : v;
    }

    private JTable table() {
        return scroll.getViewport().getView() instanceof JTable t ? t : null;
    }
}
