package com.facultyams.ui.technical;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.MedicalRow;
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
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Medical Records: every medical submission with its student, session, reason, document, status and decision,
 * filterable by student, course, batch, status and submission date. Documents open through the service,
 * which checks the user's role first.
 */
public class MedicalRecordsPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private record Lookups(List<CourseInfo> courses, List<String> batches) {
    }

    private final transient AttendanceService lookupService = new AttendanceService();
    private final HintField studentField = new HintField("Student name or reg no", 14);
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<Object> batchBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>(new String[] {"All statuses", "Pending", "Approved", "Rejected"});
    private final HintField fromField = new HintField("From (yyyy-MM-dd)", 9);
    private final HintField toField = new HintField("To (yyyy-MM-dd)", 9);
    private final ReadOnlyTableModel model = new ReadOnlyTableModel(
            new String[] {"ID", "Reg No", "Student", "Batch", "Course", "Type", "Session", "Session date", "Submitted",
                    "Reason", "Document", "Status", "Reviewed by", "Decided at"},
            new Class<?>[] {Integer.class, String.class, String.class, String.class, String.class, String.class,
                    Integer.class, String.class, String.class, String.class, String.class, String.class,
                    String.class, String.class});
    private final JTable table = table(model);
    private transient List<MedicalRow> rows = List.of();
    private boolean fillingLookups;

    public MedicalRecordsPanel() {
        JButton search = button("Search", "Apply the filters", KeyEvent.VK_S, this::search);
        JButton reset = button("Reset", "Clear all filters", KeyEvent.VK_R, this::reset);
        JButton document = button("View Document", "Open the supporting document of the selected medical",
                KeyEvent.VK_D, this::viewDocument);
        JButton details = button("View Details", "Show everything about the selected medical", KeyEvent.VK_T,
                this::showDetails);
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
        filters.add(document);
        filters.add(details);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(filters);
        top.add(statusArea());

        table.getColumnModel().getColumn(11).setCellRenderer(new StatusRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetails();
                }
            }
        });
        alignLeft(top);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        load("Loading filters...", () -> new Lookups(lookupService.getCourses(), lookupService.getBatches()), l -> {
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

    private void search() {
        if (fillingLookups) {
            return;
        }
        MedicalFilter f;
        try {
            CourseInfo course = selectedOf(courseBox, CourseInfo.class);
            f = new MedicalFilter(studentField.getText(), null, course == null ? null : course.courseId(),
                    batchBox.getSelectedIndex() > 0 ? String.valueOf(batchBox.getSelectedItem()) : null,
                    statusBox.getSelectedIndex() > 0 ? String.valueOf(statusBox.getSelectedItem()).toLowerCase(Locale.ROOT) : null,
                    parseDate(fromField.getText(), "The 'from' date"), parseDate(toField.getText(), "The 'to' date"));
        } catch (ValidationException e) {
            showError(e.getMessage());
            return;
        }
        load("Loading medical records...", () -> medicalService.list(f), list -> {
            rows = list;
            List<Object[]> data = new ArrayList<>();
            for (MedicalRow m : list) {
                data.add(new Object[] {m.medicalId(), m.registrationNo(), m.studentName(), m.batch(), m.courseCode(),
                        Display.capitalize(m.type()), m.sessionNo(), Display.date(m.sessionDate()),
                        Display.date(m.submittedDate()), m.reason(),
                        m.hasEvidence() ? Display.orDash(m.evidenceName()) : "None", Display.capitalize(m.status()),
                        Display.orDash(m.reviewerName()), Display.dateTime(m.decidedAt())});
            }
            model.setRows(data);
            showInfo(list.isEmpty() ? "No medical records match these filters."
                    : list.size() + " medical records. Double-click a row for its full details.");
        });
    }

    private MedicalRow selected() {
        int row = selectedModelRow(table);
        if (row < 0) {
            showError("Select a medical from the table first.");
            return null;
        }
        return rows.get(row);
    }

    private void viewDocument() {
        MedicalRow m = selected();
        if (m == null) {
            return;
        }
        if (!m.hasEvidence()) {
            showError("This medical has no supporting document.");
            return;
        }
        openDocument(m.medicalId());
    }

    private void showDetails() {
        MedicalRow m = selected();
        if (m == null) {
            return;
        }
        String html = "<html><body style='width:380px'><b>" + esc(m.studentName()) + "</b> (" + esc(m.registrationNo())
                + ", batch " + esc(m.batch()) + ")<br>"
                + esc(m.courseCode() + " " + m.courseName() + " - " + Display.capitalize(m.type())) + ", session "
                + m.sessionNo() + " on " + Display.date(m.sessionDate()) + "<br><br>"
                + "<b>Submitted:</b> " + Display.date(m.submittedDate()) + "<br>"
                + "<b>Reason:</b> " + esc(m.reason()) + "<br>"
                + "<b>Document:</b> " + esc(m.hasEvidence() ? Display.orDash(m.evidenceName()) : "none") + "<br>"
                + "<b>Status:</b> " + Display.capitalize(m.status()) + "<br>"
                + (m.reviewerName() == null ? "" : "<b>Reviewed by:</b> " + esc(m.reviewerName()) + " on "
                        + Display.dateTime(m.decidedAt()) + "<br>")
                + (m.rejectionReason() == null ? "" : "<b>Rejection reason:</b> " + esc(m.rejectionReason()) + "<br>")
                + "</body></html>";
        JLabel label = new JLabel(html);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JOptionPane.showMessageDialog(this, label, "Medical " + m.medicalId(), JOptionPane.PLAIN_MESSAGE);
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}