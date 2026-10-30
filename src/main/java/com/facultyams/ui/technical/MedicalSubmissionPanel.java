package com.facultyams.ui.technical;

import com.facultyams.model.MedicalRow;
import com.facultyams.model.StudentInfo;
import com.facultyams.model.SubmittableSession;
import com.facultyams.security.EvidenceStorage;
import com.facultyams.util.Display;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Medical Submission: pick a student, pick one of their absent sessions that has no medical yet, give the reason
 * and attach the document (PDF / JPG / PNG, up to 5 MB). The new medical is Pending. The student's earlier
 * submissions and their status are listed underneath. The same rules are enforced by {@code TechnicalMedicalService},
 * which a student screen can call for a student's own absences.
 */
public class MedicalSubmissionPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private record StudentData(List<SubmittableSession> absences, List<MedicalRow> history) {
    }

    private final HintField searchField = new HintField("Student name or reg no", 16);
    private final JComboBox<Object> studentBox = new JComboBox<>();
    private final ReadOnlyTableModel absenceModel = ReadOnlyTableModel.ofStrings("Session date", "Course", "Subject", "Session");
    private final JTable absenceTable = table(absenceModel);
    private final HintField rangeFrom = new HintField("From (yyyy-MM-dd)", 9);
    private final HintField rangeTo = new HintField("To (yyyy-MM-dd)", 9);
    private final JTextArea reasonArea = new JTextArea(3, 30);
    private final JLabel fileLabel = new JLabel("No document selected");
    private final ReadOnlyTableModel historyModel = ReadOnlyTableModel.ofStrings(
            "Submitted", "Course", "Session date", "Reason", "Document", "Status", "Decision", "Rejection reason");
    private final JTable historyTable = table(historyModel);
    private transient List<SubmittableSession> absences = List.of();
    private transient Path chosenFile;
    private boolean fillingStudents;

    public MedicalSubmissionPanel() {
        JButton find = button("Find Student", "Search students by name or registration number", KeyEvent.VK_F, this::findStudents);
        searchField.addActionListener(e -> findStudents());
        studentBox.addActionListener(e -> {
            if (!fillingStudents) {
                loadStudentData();
            }
        });
        studentBox.setPrototypeDisplayValue("TG/2023/1001 - Chamari Dissanayake Wijesinghe");

        JPanel pick = filterBar();
        pick.add(searchField);
        pick.add(find);
        pick.add(labeled("Student", studentBox));

        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.getAccessibleContext().setAccessibleName("Reason for the medical");
        JButton browse = button("Choose Document...", "Optionally attach the medical certificate (PDF, JPG or PNG, up to "
                + EvidenceStorage.MAX_MB + " MB)", KeyEvent.VK_C, this::chooseFile);
        JButton submit = button("Submit Medical", "Submit the medical for approval", KeyEvent.VK_M, this::submit);
        JButton clear = button("Clear Form", "Clear the reason and document", 0, this::clearForm);

        JPanel docRow = filterBar();
        docRow.add(browse);
        docRow.add(fileLabel);
        JPanel buttons = filterBar();
        buttons.add(submit);
        buttons.add(clear);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(heading("1. Student"));
        form.add(pick);
        form.add(heading("2. Absent sessions (select one or more, or pick a date range)"));
        JPanel range = filterBar();
        range.add(rangeFrom);
        range.add(rangeTo);
        range.add(button("Select Range", "Select every listed absence between the two dates", KeyEvent.VK_R, this::selectRange));
        form.add(range);
        absenceTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane absenceScroll = new JScrollPane(absenceTable);
        absenceScroll.setPreferredSize(new Dimension(100, 130));
        absenceScroll.setAlignmentX(LEFT_ALIGNMENT);
        form.add(absenceScroll);
        form.add(heading("3. Reason and optional document"));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        reasonScroll.setAlignmentX(LEFT_ALIGNMENT);
        reasonScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        form.add(reasonScroll);
        form.add(docRow);
        form.add(buttons);
        form.add(statusArea());
        form.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        for (java.awt.Component c : form.getComponents()) {
            ((javax.swing.JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        }

        JPanel history = new JPanel(new BorderLayout(0, 4));
        history.add(heading("Submission status of this student"), BorderLayout.NORTH);
        history.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        historyTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        add(form, BorderLayout.NORTH);
        add(history, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        if (studentBox.getItemCount() > 0 && studentBox.getSelectedItem() instanceof StudentInfo) {
            loadStudentData();
        }
    }

    private void findStudents() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            showError("Type a name or registration number to search for.");
            return;
        }
        load("Searching students...", () -> medicalService.searchStudents(text), students -> {
            fillingStudents = true;
            fillCombo(studentBox, "Select a student", students);
            fillingStudents = false;
            if (students.isEmpty()) {
                showInfo("No student matches \"" + text + "\".");
                absenceModel.setRowCount(0);
                historyModel.setRowCount(0);
            } else if (students.size() == 1) {
                studentBox.setSelectedIndex(1); // fires loadStudentData
            } else {
                showInfo(students.size() + " students found. Choose one from the Student list.");
            }
        });
    }

    private void loadStudentData() {
        StudentInfo student = selectedOf(studentBox, StudentInfo.class);
        if (student == null) {
            absenceModel.setRowCount(0);
            historyModel.setRowCount(0);
            absences = List.of();
            return;
        }
        load("Loading " + student.fullName() + "...",
                () -> new StudentData(medicalService.getSubmittableSessions(student.userId()),
                        medicalService.listForStudent(student.userId())),
                data -> {
                    absences = data.absences();
                    List<Object[]> a = new ArrayList<>();
                    for (SubmittableSession s : absences) {
                        a.add(new Object[] {Display.date(s.date()), s.courseCode() + " - " + s.courseName(),
                                Display.capitalize(s.type()), String.valueOf(s.sessionNo())});
                    }
                    absenceModel.setRows(a);
                    List<Object[]> h = new ArrayList<>();
                    for (MedicalRow m : data.history()) {
                        h.add(new Object[] {Display.date(m.submittedDate()), m.courseCode() + " " + Display.capitalize(m.type()),
                                Display.date(m.sessionDate()), m.reason(), m.hasEvidence() ? Display.orDash(m.evidenceName()) : "None",
                                Display.capitalize(m.status()),
                                m.reviewerName() == null ? "-" : m.reviewerName() + " " + Display.dateTime(m.decidedAt()),
                                Display.orDash(m.rejectionReason())});
                    }
                    historyModel.setRows(h);
                    showInfo(absences.isEmpty()
                            ? student.fullName() + " has no absent sessions without a medical."
                            : student.fullName() + " has " + absences.size() + " absent session(s) without a medical.");
                });
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the medical document");
        chooser.setFileFilter(new FileNameExtensionFilter("Medical documents (PDF, JPG, PNG)", "pdf", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            chosenFile = f.toPath();
            fileLabel.setText(f.getName() + "  (" + Math.max(1, f.length() / 1024) + " KB)");
        }
    }

    private void clearForm() {
        reasonArea.setText("");
        chosenFile = null;
        fileLabel.setText("No document selected");
        clearMessages();
    }

    private void selectRange() {
        try {
            java.time.LocalDate from = parseDate(rangeFrom.getText(), "The 'from' date");
            java.time.LocalDate to = parseDate(rangeTo.getText(), "The 'to' date");
            if (from == null || to == null) {
                showError("Enter both a from and a to date, for example 2026-10-01.");
                return;
            }
            if (from.isAfter(to)) {
                showError("The 'from' date must not be after the 'to' date.");
                return;
            }
            absenceTable.clearSelection();
            int count = 0;
            for (int view = 0; view < absenceTable.getRowCount(); view++) {
                java.time.LocalDate d = absences.get(absenceTable.convertRowIndexToModel(view)).date();
                if (!d.isBefore(from) && !d.isAfter(to)) {
                    absenceTable.addRowSelectionInterval(view, view);
                    count++;
                }
            }
            showInfo(count == 0 ? "This student has no listed absences between those dates."
                    : count + " absence(s) selected in that date range.");
        } catch (com.facultyams.exception.ValidationException e) {
            showError(e.getMessage());
        }
    }

    private void submit() {
        if (selectedOf(studentBox, StudentInfo.class) == null) {
            showError("Find and select a student first.");
            return;
        }
        int[] viewRows = absenceTable.getSelectedRows();
        if (viewRows.length == 0) {
            showError("Select the absent session(s) the medical is for.");
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (int v : viewRows) {
            ids.add(absences.get(absenceTable.convertRowIndexToModel(v)).attendanceId());
        }
        String reason = reasonArea.getText();
        Path file = chosenFile;
        act("Submitting the medical...", () -> medicalService.submit(ids, reason, file), count -> {
            clearForm();
            load("Reloading...", () -> {
                StudentInfo s = selectedOf(studentBox, StudentInfo.class);
                return new StudentData(medicalService.getSubmittableSessions(s.userId()), medicalService.listForStudent(s.userId()));
            }, data -> {
                loadStudentDataFrom(data);
                showSuccess(count + " medical(s) submitted. Status: Pending until a technical officer decides.");
            });
        });
    }

    private void loadStudentDataFrom(StudentData data) {
        absences = data.absences();
        List<Object[]> a = new ArrayList<>();
        for (SubmittableSession s : absences) {
            a.add(new Object[] {Display.date(s.date()), s.courseCode() + " - " + s.courseName(),
                    Display.capitalize(s.type()), String.valueOf(s.sessionNo())});
        }
        absenceModel.setRows(a);
        List<Object[]> h = new ArrayList<>();
        for (MedicalRow m : data.history()) {
            h.add(new Object[] {Display.date(m.submittedDate()), m.courseCode() + " " + Display.capitalize(m.type()),
                    Display.date(m.sessionDate()), m.reason(), m.hasEvidence() ? Display.orDash(m.evidenceName()) : "None",
                    Display.capitalize(m.status()),
                    m.reviewerName() == null ? "-" : m.reviewerName() + " " + Display.dateTime(m.decidedAt()),
                    Display.orDash(m.rejectionReason())});
        }
        historyModel.setRows(h);
    }
}