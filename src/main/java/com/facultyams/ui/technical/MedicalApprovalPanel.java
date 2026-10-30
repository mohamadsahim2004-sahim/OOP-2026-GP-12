package com.facultyams.ui.technical;

import com.facultyams.model.MedicalRow;
import com.facultyams.service.TechnicalMedicalService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.Display;
import com.facultyams.util.UIUtil;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Medical Approval: the queue of pending medicals. The officer can read the document, approve, or reject
 * with a required reason. The service records the reviewer and time, and refuses a second decision.
 */
public class MedicalApprovalPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private final ReadOnlyTableModel model = new ReadOnlyTableModel(
            new String[] {"ID", "Reg No", "Student", "Course", "Type", "Session", "Session date", "Submitted", "Reason", "Document"},
            new Class<?>[] {Integer.class, String.class, String.class, String.class, String.class, Integer.class,
                    String.class, String.class, String.class, String.class});
    private final JTable table = table(model);
    private transient List<MedicalRow> rows = List.of();

    public MedicalApprovalPanel() {
        JButton approve = button("Approve", "Approve the selected medical", KeyEvent.VK_A, this::approve);
        JButton reject = button("Reject", "Reject the selected medical (a reason is required)", KeyEvent.VK_J, this::reject);
        reject.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        AdminTheme.danger(reject);
        JButton document = button("View Document", "Open the supporting document", KeyEvent.VK_D, this::viewDocument);
        JButton reload = button("Refresh", "Reload the pending list", KeyEvent.VK_R, this::refresh);

        JPanel actions = filterBar();
        actions.add(approve);
        actions.add(reject);
        actions.add(document);
        actions.add(reload);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(actions);
        top.add(statusArea());
        alignLeft(top);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        load("Loading pending medicals...", TechnicalMedicalService::listPending, this::show);
    }

    private void show(List<MedicalRow> pending) {
        rows = pending;
        List<Object[]> data = new ArrayList<>();
        for (MedicalRow m : pending) {
            data.add(new Object[] {m.medicalId(), m.registrationNo(), m.studentName(), m.courseCode(),
                    Display.capitalize(m.type()), m.sessionNo(), Display.date(m.sessionDate()),
                    Display.date(m.submittedDate()), m.reason(), m.hasEvidence() ? Display.orDash(m.evidenceName()) : "None"});
        }
        model.setRows(data);
        showInfo(pending.isEmpty() ? "No medicals are waiting for approval." : pending.size() + " medical(s) waiting for a decision.");
    }

    private MedicalRow selected() {
        int row = selectedModelRow(table);
        if (row < 0) {
            showError("Select a pending medical from the table first.");
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

    private void approve() {
        MedicalRow m = selected();
        if (m == null || !UIUtil.confirm(this, "Approve the medical of " + m.studentName() + " for "
                + m.courseCode() + " " + Display.capitalize(m.type()) + ", session " + m.sessionNo()
                + " (" + Display.date(m.sessionDate()) + ")?\n\nThe session will count towards the student's attendance.")) {
            return;
        }
        act("Saving the decision...", () -> {
            TechnicalMedicalService.approve(m.medicalId());
            return m.medicalId();
        }, id -> {
            refreshAfter("Medical " + id + " approved.");
        });
    }

    private void reject() {
        MedicalRow m = selected();
        if (m == null) {
            return;
        }
        String reason = askRejectionReason(m);
        if (reason == null) {
            return;
        }
        act("Saving the decision...", () -> {
            TechnicalMedicalService.reject(m.medicalId(), reason);
            return m.medicalId();
        }, id -> {
            refreshAfter("Medical " + id + " rejected.");
        });
    }

    private void refreshAfter(String successMessage) {
        load("Reloading...", TechnicalMedicalService::listPending, list -> {
            show(list);
            showSuccess(successMessage);
        });
    }

    /** Keeps asking until a valid reason is typed; null means the officer cancelled. */
    private String askRejectionReason(MedicalRow m) {
        JTextArea area = new JTextArea(4, 34);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.getAccessibleContext().setAccessibleName("Reason for rejecting");
        JLabel hint = new JLabel("Reason for rejecting (required, " + TechnicalMedicalService.MIN_TEXT + "-"
                + TechnicalMedicalService.MAX_TEXT + " characters):");
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(hint, BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, p, "Reject medical of " + m.studentName(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            String text = area.getText().trim();
            if (text.length() >= TechnicalMedicalService.MIN_TEXT && text.length() <= TechnicalMedicalService.MAX_TEXT) {
                return text;
            }
            hint.setText("Please enter between " + TechnicalMedicalService.MIN_TEXT + " and " + TechnicalMedicalService.MAX_TEXT
                    + " characters:");
            hint.setForeground(AdminTheme.DANGER);
        }
    }
}