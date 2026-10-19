package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.*;
import com.srms.service.SubmissionService;
import com.srms.ui.components.DropZone;
import com.srms.ui.components.StatusPill;
import com.srms.ui.components.Toast;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.*;

/**
 * Screen C: Assignment view with details, live countdown timer, key/value status table,
 * drag-and-drop submission zone, validation, and async file persistence.
 */
public class AssignmentView extends JPanel {
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH);

    private final SubmissionService submissionService;
    private final String studentId;
    private final Runnable onNavigateToCourses;
    private final Runnable onNavigateToCoursePage;

    private Course currentCourse;
    private Assignment currentAssignment;

    // UI elements
    private final JPanel breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel assignmentTitleLbl = new JLabel();
    private final JTextArea descriptionArea = new JTextArea();
    private final JLabel datesAndConstraintsLbl = new JLabel();

    // Table rows
    private final StatusPill statusPill = new StatusPill(SubmissionStatus.NOT_SUBMITTED);
    private final JLabel gradingStatusLbl = new JLabel("Not graded");
    private final JLabel timeRemainingLbl = new JLabel();
    private final JLabel lastModifiedLbl = new JLabel("-");
    private final JPanel filesCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel feedbackLbl = new JLabel("-");

    // Submission controls
    private DropZone dropZone;
    private final JPanel dropZoneHolder = new JPanel(new BorderLayout());
    private final JLabel inlineErrorLbl = new JLabel();
    private final JButton submitBtn = new JButton("Add submission");
    private final JButton removeBtn = new JButton("Remove submission");
    private final JButton cancelBtn = new JButton("Cancel");

    private Timer countdownTimer;

    public AssignmentView(SubmissionService submissionService, String studentId,
                          Runnable onNavigateToCourses, Runnable onNavigateToCoursePage) {
        this.submissionService = submissionService;
        this.studentId = studentId;
        this.onNavigateToCourses = onNavigateToCourses;
        this.onNavigateToCoursePage = onNavigateToCoursePage;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 4, 16, 4));

        initUI();
    }

    private void initUI() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);

        // 1. Breadcrumb
        breadcrumbPanel.setOpaque(false);
        scrollContent.add(breadcrumbPanel);
        scrollContent.add(Box.createVerticalStrut(12));

        // 2. Details Card
        Ui.Card detailsCard = new Ui.Card();
        detailsCard.setLayout(new BorderLayout(0, 10));
        detailsCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        assignmentTitleLbl.setFont(Theme.FONT_TITLE);
        assignmentTitleLbl.setForeground(Theme.TEXT_MAIN);
        detailsCard.add(assignmentTitleLbl, BorderLayout.NORTH);

        descriptionArea.setFont(Theme.FONT_BODY);
        descriptionArea.setForeground(Theme.TEXT_MUTED);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        detailsCard.add(descriptionArea, BorderLayout.CENTER);

        datesAndConstraintsLbl.setFont(Theme.FONT_SMALL);
        datesAndConstraintsLbl.setForeground(Theme.TEXT_SUBTLE);
        detailsCard.add(datesAndConstraintsLbl, BorderLayout.SOUTH);

        scrollContent.add(detailsCard);
        scrollContent.add(Box.createVerticalStrut(16));

        // 3. Submission Status Card
        Ui.Card statusCard = new Ui.Card();
        statusCard.setLayout(new BorderLayout());
        statusCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel statusHeader = new JLabel("Submission status");
        statusHeader.setFont(Theme.FONT_SECTION);
        statusHeader.setForeground(Theme.TEXT_MAIN);
        statusCard.add(statusHeader, BorderLayout.NORTH);

        JPanel tableGrid = new JPanel(new GridLayout(6, 1, 0, 1));
        tableGrid.setOpaque(false);
        tableGrid.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        tableGrid.add(createTableRow("Submission status", statusPill, false));
        tableGrid.add(createTableRow("Grading status", gradingStatusLbl, true));
        tableGrid.add(createTableRow("Time remaining", timeRemainingLbl, false));
        tableGrid.add(createTableRow("Last modified", lastModifiedLbl, true));

        filesCell.setOpaque(false);
        tableGrid.add(createTableRow("File submissions", filesCell, false));
        tableGrid.add(createTableRow("Feedback comments", feedbackLbl, true));

        statusCard.add(tableGrid, BorderLayout.CENTER);
        scrollContent.add(statusCard);
        scrollContent.add(Box.createVerticalStrut(16));

        // 4. Submission Upload Zone Card
        Ui.Card uploadCard = new Ui.Card();
        uploadCard.setLayout(new BorderLayout(0, 14));
        uploadCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel uploadTitle = new JLabel("File submission");
        uploadTitle.setFont(Theme.FONT_SUBTITLE);
        uploadTitle.setForeground(Theme.TEXT_MAIN);
        uploadCard.add(uploadTitle, BorderLayout.NORTH);

        dropZoneHolder.setOpaque(false);
        uploadCard.add(dropZoneHolder, BorderLayout.CENTER);

        // South action buttons & error
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setOpaque(false);

        inlineErrorLbl.setFont(Theme.FONT_BODY_BOLD);
        inlineErrorLbl.setForeground(Theme.DANGER);
        inlineErrorLbl.setVisible(false);
        southPanel.add(inlineErrorLbl);
        southPanel.add(Box.createVerticalStrut(10));

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsRow.setOpaque(false);

        stylePrimaryButton(submitBtn);
        styleDangerButton(removeBtn);
        styleSecondaryButton(cancelBtn);

        submitBtn.addActionListener(e -> handleSubmit());
        removeBtn.addActionListener(e -> handleRemove());
        cancelBtn.addActionListener(e -> {
            if (onNavigateToCoursePage != null) onNavigateToCoursePage.run();
        });

        buttonsRow.add(removeBtn);
        buttonsRow.add(cancelBtn);
        buttonsRow.add(submitBtn);

        southPanel.add(buttonsRow);
        uploadCard.add(southPanel, BorderLayout.SOUTH);

        scrollContent.add(uploadCard);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        Ui.applyThinScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createTableRow(String label, JComponent valueComp, boolean altBg) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(true);
        row.setBackground(altBg ? new Color(249, 250, 251) : Color.WHITE);
        row.setPreferredSize(new Dimension(500, 44));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel keyLbl = new JLabel(label);
        keyLbl.setFont(Theme.FONT_BODY_BOLD);
        keyLbl.setForeground(Theme.TEXT_MUTED);
        keyLbl.setPreferredSize(new Dimension(170, 24));

        row.add(keyLbl, BorderLayout.WEST);

        if (valueComp instanceof JLabel lbl) {
            lbl.setFont(Theme.FONT_BODY);
            lbl.setForeground(Theme.TEXT_MAIN);
        }
        row.add(valueComp, BorderLayout.CENTER);
        return row;
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(Theme.PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
    }

    private void styleDangerButton(JButton btn) {
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setForeground(Theme.DANGER);
        btn.setBackground(Theme.DANGER_BG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setForeground(Theme.TEXT_MUTED);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void loadAssignment(Course course, Assignment assignment) {
        this.currentCourse = course;
        this.currentAssignment = assignment;
        inlineErrorLbl.setVisible(false);

        // Update breadcrumb
        breadcrumbPanel.removeAll();
        addBreadcrumbItem("Courses", onNavigateToCourses);
        addBreadcrumbSeparator();
        addBreadcrumbItem(course.title(), onNavigateToCoursePage);
        addBreadcrumbSeparator();
        JLabel currentLbl = new JLabel(assignment.title());
        currentLbl.setFont(Theme.FONT_BODY_BOLD);
        currentLbl.setForeground(Theme.TEXT_MAIN);
        breadcrumbPanel.add(currentLbl);

        // Update details card
        assignmentTitleLbl.setText(assignment.title());
        descriptionArea.setText(assignment.description());

        String openedStr = assignment.opensAt() != null ? assignment.opensAt().format(DATE_FMT) : "Now";
        String dueStr = assignment.dueAt() != null ? assignment.dueAt().format(DATE_FMT) : "No due date";
        String typesStr = String.join(", ", assignment.allowedTypes()).toUpperCase();
        datesAndConstraintsLbl.setText(String.format("Opened: %s   |   Due: %s   |   Allowed: %s   |   Max %d MB",
                openedStr, dueStr, typesStr, assignment.maxFileMb()));

        // Setup DropZone
        dropZoneHolder.removeAll();
        dropZone = new DropZone(assignment.allowedTypes(), assignment.maxFiles(), assignment.maxFileMb());
        dropZone.setOnError(err -> {
            if (err != null) {
                inlineErrorLbl.setText(err);
                inlineErrorLbl.setVisible(true);
            } else {
                inlineErrorLbl.setVisible(false);
            }
        });
        dropZoneHolder.add(dropZone, BorderLayout.CENTER);
        dropZoneHolder.revalidate();

        // Refresh submission state
        refreshStatus();
        startCountdownTimer();
    }

    public void refreshStatus() {
        if (currentAssignment == null) return;

        Optional<Submission> subOpt = submissionService.find(studentId, currentAssignment.id());

        filesCell.removeAll();
        if (subOpt.isPresent()) {
            Submission sub = subOpt.get();
            statusPill.setStatus(sub.status());
            gradingStatusLbl.setText(sub.grade() != null ? sub.grade() : "Not graded");
            lastModifiedLbl.setText(sub.submittedAt().format(DATE_FMT));
            feedbackLbl.setText(sub.feedback() != null ? sub.feedback() : "-");

            for (String fn : sub.files()) {
                JLabel fileLbl = new JLabel(fn);
                fileLbl.setFont(Theme.FONT_BODY_BOLD);
                fileLbl.setForeground(Theme.PRIMARY);
                fileLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                filesCell.add(fileLbl);
            }

            submitBtn.setText("Edit submission");
            removeBtn.setVisible(true);
        } else {
            statusPill.setStatus(SubmissionStatus.NOT_SUBMITTED);
            gradingStatusLbl.setText("Not graded");
            lastModifiedLbl.setText("-");
            feedbackLbl.setText("-");
            filesCell.add(new JLabel("No files submitted"));

            submitBtn.setText("Add submission");
            removeBtn.setVisible(false);
        }

        filesCell.revalidate();
        filesCell.repaint();
        updateTimeRemaining();
    }

    private void startCountdownTimer() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        countdownTimer = new Timer(1000, e -> updateTimeRemaining());
        countdownTimer.start();
        updateTimeRemaining();
    }

    private void updateTimeRemaining() {
        if (currentAssignment == null || currentAssignment.dueAt() == null) {
            timeRemainingLbl.setText("No due date");
            timeRemainingLbl.setForeground(Theme.TEXT_MAIN);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = currentAssignment.dueAt();

        if (now.isBefore(due)) {
            Duration dur = Duration.between(now, due);
            long days = dur.toDays();
            long hours = dur.toHoursPart();
            long mins = dur.toMinutesPart();
            long secs = dur.toSecondsPart();

            String timeStr = String.format("%d days %d hours %d mins %d secs", days, hours, mins, secs);
            timeRemainingLbl.setText(timeStr);

            if (dur.toHours() < 24) {
                timeRemainingLbl.setForeground(Theme.DANGER);
            } else {
                timeRemainingLbl.setForeground(Theme.TEXT_MAIN);
            }
        } else {
            Duration overdue = Duration.between(due, now);
            long days = overdue.toDays();
            long hours = overdue.toHoursPart();
            long mins = overdue.toMinutesPart();

            String lateStr = "Overdue by " + (days > 0 ? days + " days " : "") + hours + " hours " + mins + " mins";
            timeRemainingLbl.setText(lateStr);
            timeRemainingLbl.setForeground(Theme.DANGER);
        }
    }

    private void handleSubmit() {
        inlineErrorLbl.setVisible(false);

        List<File> files = dropZone.getSelectedFiles();
        if (files.isEmpty()) {
            inlineErrorLbl.setText("Please choose or drag at least one file to submit.");
            inlineErrorLbl.setVisible(true);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (currentAssignment.dueAt() != null && now.isAfter(currentAssignment.dueAt()) && !currentAssignment.allowLate()) {
            inlineErrorLbl.setText("Submission deadline has passed. Late submissions are not permitted.");
            inlineErrorLbl.setVisible(true);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to save this submission?",
                "Confirm Submission",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        submitBtn.setEnabled(false);
        dropZone.setUploading(true, 30);

        new SwingWorker<Submission, Integer>() {
            @Override
            protected Submission doInBackground() throws Exception {
                publish(60);
                Submission sub = submissionService.submit(studentId, currentAssignment.id(), files);
                publish(100);
                return sub;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    dropZone.setUploading(true, chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                submitBtn.setEnabled(true);
                dropZone.setUploading(false, 0);
                try {
                    get();
                    dropZone.clearFiles();
                    refreshStatus();
                    Toast.show(AssignmentView.this, "Submission saved");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignmentView.this,
                            "Failed to save submission: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleRemove() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove your submission?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                submissionService.remove(studentId, currentAssignment.id());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshStatus();
                    Toast.show(AssignmentView.this, "Submission removed");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignmentView.this,
                            "Failed to remove submission: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addBreadcrumbItem(String text, Runnable onClick) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BODY);
        lbl.setForeground(Theme.PRIMARY);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
        breadcrumbPanel.add(lbl);
    }

    private void addBreadcrumbSeparator() {
        JLabel sep = new JLabel(" > ");
        sep.setFont(Theme.FONT_BODY);
        sep.setForeground(Theme.TEXT_SUBTLE);
        breadcrumbPanel.add(sep);
    }
}
