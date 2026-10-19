package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.*;
import com.srms.service.AttendanceService;
import com.srms.service.MedicalService;
import com.srms.ui.components.DateRangeCalendar;
import com.srms.ui.components.DropZone;
import com.srms.ui.components.StepIndicator;
import com.srms.ui.components.Toast;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * 3-Step Medical Submission Wizard:
 * Step 1: Date Range Selection
 * Step 2: Course & Session Selection
 * Step 3: Medical Details & Certificate Upload
 */
public class MedicalWizardView extends JPanel {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    private final AttendanceService attendanceService;
    private final MedicalService medicalService;
    private final String studentId;
    private final Runnable onComplete;
    private final Runnable onCancel;

    private int currentStep = 1;

    // Wizard Components
    private final StepIndicator stepIndicator = new StepIndicator();
    private final CardLayout wizardLayout = new CardLayout();
    private final JPanel stepsContainer = new JPanel(wizardLayout);

    private final JButton backBtn = new JButton("Back");
    private final JButton nextBtn = new JButton("Next");
    private final JButton cancelBtn = new JButton("Cancel");

    // Step 1 Data & Components
    private final DateRangeCalendar calendar = new DateRangeCalendar();
    private final JLabel rangeSummaryLbl = new JLabel("Please select a date range on the calendar.");
    private final JLabel daysCountLbl = new JLabel("-");
    private final JLabel absentCountLbl = new JLabel("-");
    private SwingWorker<List<SessionAttendance>, Void> currentLoaderWorker;

    // Step 2 Data & Components
    private final List<SessionAttendance> loadedSessions = new ArrayList<>();
    private final Set<String> selectedSessionIds = new HashSet<>();
    private final JPanel courseChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final JPanel sessionsListPanel = new JPanel();
    private final JLabel selectionSummaryLbl = new JLabel("0 sessions selected");

    // Step 3 Data & Components
    private final JComboBox<String> reasonCombo = new JComboBox<>(new String[]{ "Illness", "Injury", "Hospitalisation", "Family emergency", "Other" });
    private final JTextField doctorField = new JTextField();
    private final JTextField certNoField = new JTextField();
    private final JTextArea notesArea = new JTextArea(3, 20);
    private final JCheckBox confirmBox = new JCheckBox("I confirm that the provided medical information and documents are authentic and accurate.");
    private final JLabel step3ErrorLbl = new JLabel();
    private DropZone dropZone;
    private final JPanel reviewPanel = new JPanel();

    public MedicalWizardView(AttendanceService attendanceService, MedicalService medicalService,
                             String studentId, Runnable onComplete, Runnable onCancel) {
        this.attendanceService = attendanceService;
        this.medicalService = medicalService;
        this.studentId = studentId;
        this.onComplete = onComplete;
        this.onCancel = onCancel;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 4, 16, 4));

        initUI();
        initAbsentDates();
    }

    private void initUI() {
        // North: Stepper Indicator
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);

        JLabel titleLbl = new JLabel("Submit Medical Certificate");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);
        north.add(titleLbl, BorderLayout.WEST);

        stepIndicator.setPreferredSize(new Dimension(420, 50));
        north.add(stepIndicator, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        // Center: Steps container
        stepsContainer.setOpaque(false);
        stepsContainer.add(createStep1Panel(), "Step1");
        stepsContainer.add(createStep2Panel(), "Step2");
        stepsContainer.add(createStep3Panel(), "Step3");
        add(stepsContainer, BorderLayout.CENTER);

        // South: Navigation controls
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        styleButton(cancelBtn, false, false);
        cancelBtn.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
        });
        south.add(cancelBtn, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBtns.setOpaque(false);

        styleButton(backBtn, false, false);
        backBtn.setEnabled(false);
        backBtn.addActionListener(e -> prevStep());

        styleButton(nextBtn, true, false);
        nextBtn.addActionListener(e -> nextStep());

        rightBtns.add(backBtn);
        rightBtns.add(nextBtn);
        south.add(rightBtns, BorderLayout.EAST);

        add(south, BorderLayout.SOUTH);
        validateCurrentStep();
    }

    private void initAbsentDates() {
        new SwingWorker<List<SessionAttendance>, Void>() {
            @Override
            protected List<SessionAttendance> doInBackground() {
                LocalDate start = LocalDate.now().minusDays(MedicalPolicy.WINDOW_DAYS);
                return attendanceService.sessionsBetween(studentId, start, LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    List<SessionAttendance> list = get();
                    Set<LocalDate> abs = new HashSet<>();
                    for (SessionAttendance sa : list) {
                        if (sa.status() == SessionStatus.ABSENT) {
                            abs.add(sa.session().date());
                        }
                    }
                    calendar.setAbsentDates(abs);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // --- STEP 1: DATES ---
    private JPanel createStep1Panel() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);

        // Left: Calendar
        Ui.Card calCard = new Ui.Card();
        calCard.setLayout(new BorderLayout());
        calCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        calCard.add(calendar, BorderLayout.CENTER);
        p.add(calCard, BorderLayout.CENTER);

        // Right: Summary Card
        Ui.Card summaryCard = new Ui.Card();
        summaryCard.setPreferredSize(new Dimension(300, 360));
        summaryCard.setLayout(new BorderLayout());
        summaryCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);

        JLabel sumTitle = new JLabel("Date Selection Summary");
        sumTitle.setFont(Theme.FONT_SUBTITLE);
        sumTitle.setForeground(Theme.TEXT_MAIN);
        sumTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        rangeSummaryLbl.setFont(Theme.FONT_BODY_BOLD);
        rangeSummaryLbl.setForeground(Theme.PRIMARY);
        rangeSummaryLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        daysCountLbl.setFont(Theme.FONT_SECTION);
        daysCountLbl.setForeground(Theme.TEXT_MAIN);
        daysCountLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        absentCountLbl.setFont(Theme.FONT_SECTION);
        absentCountLbl.setForeground(Theme.DANGER);
        absentCountLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        stack.add(sumTitle);
        stack.add(Box.createVerticalStrut(18));

        JLabel l1 = new JLabel("Selected Period");
        l1.setFont(Theme.FONT_TINY);
        l1.setForeground(Theme.TEXT_MUTED);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(l1);
        stack.add(Box.createVerticalStrut(3));
        stack.add(rangeSummaryLbl);
        stack.add(Box.createVerticalStrut(16));

        JLabel l2 = new JLabel("Duration");
        l2.setFont(Theme.FONT_TINY);
        l2.setForeground(Theme.TEXT_MUTED);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(l2);
        stack.add(Box.createVerticalStrut(3));
        stack.add(daysCountLbl);
        stack.add(Box.createVerticalStrut(16));

        JLabel l3 = new JLabel("Absent Sessions in Period");
        l3.setFont(Theme.FONT_TINY);
        l3.setForeground(Theme.TEXT_MUTED);
        l3.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(l3);
        stack.add(Box.createVerticalStrut(3));
        stack.add(absentCountLbl);
        stack.add(Box.createVerticalGlue());

        summaryCard.add(stack, BorderLayout.CENTER);
        p.add(summaryCard, BorderLayout.EAST);

        // Listener for calendar range changes
        calendar.addPropertyChangeListener("range", evt -> onRangeChanged());
        return p;
    }

    private void onRangeChanged() {
        LocalDate start = calendar.getRangeStart();
        LocalDate end = calendar.getRangeEnd();

        if (start == null || end == null) {
            rangeSummaryLbl.setText("Please select a start and end date.");
            daysCountLbl.setText("-");
            absentCountLbl.setText("-");
            validateCurrentStep();
            return;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        rangeSummaryLbl.setText(start.format(DATE_FMT) + " to " + end.format(DATE_FMT));
        daysCountLbl.setText(days + (days == 1 ? " day" : " days"));

        // Asynchronously load absent sessions
        if (currentLoaderWorker != null && !currentLoaderWorker.isDone()) {
            currentLoaderWorker.cancel(true);
        }

        absentCountLbl.setText("Searching sessions...");
        currentLoaderWorker = new SwingWorker<List<SessionAttendance>, Void>() {
            @Override
            protected List<SessionAttendance> doInBackground() {
                return attendanceService.sessionsBetween(studentId, start, end);
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<SessionAttendance> list = get();
                    loadedSessions.clear();
                    loadedSessions.addAll(list);

                    long absentCount = list.stream().filter(s -> s.status() == SessionStatus.ABSENT).count();
                    absentCountLbl.setText(absentCount + " absent sessions");

                    validateCurrentStep();
                } catch (Exception ex) {
                    absentCountLbl.setText("Error loading sessions");
                }
            }
        };
        currentLoaderWorker.execute();
    }

    // --- STEP 2: COURSES & SESSIONS ---
    private JPanel createStep2Panel() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        Ui.Card card = new Ui.Card();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header controls (Chips + Select All)
        JPanel topHeader = new JPanel(new BorderLayout(10, 8));
        topHeader.setOpaque(false);

        JPanel chipsBox = new JPanel();
        chipsBox.setLayout(new BoxLayout(chipsBox, BoxLayout.Y_AXIS));
        chipsBox.setOpaque(false);

        JLabel chipHeader = new JLabel("Filter & toggle whole courses:");
        chipHeader.setFont(Theme.FONT_TINY);
        chipHeader.setForeground(Theme.TEXT_MUTED);

        courseChipsPanel.setOpaque(false);
        chipsBox.add(chipHeader);
        chipsBox.add(Box.createVerticalStrut(4));
        chipsBox.add(courseChipsPanel);
        topHeader.add(chipsBox, BorderLayout.WEST);

        // Select all link & live counter
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        actionRow.setOpaque(false);

        JLabel selectAllLink = new JLabel("Select all absent");
        selectAllLink.setFont(Theme.FONT_BODY_BOLD);
        selectAllLink.setForeground(Theme.PRIMARY);
        selectAllLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectAllLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectAllAbsent(true);
            }
        });

        selectionSummaryLbl.setFont(Theme.FONT_BODY_BOLD);
        selectionSummaryLbl.setForeground(Theme.TEXT_MAIN);

        actionRow.add(selectAllLink);
        actionRow.add(selectionSummaryLbl);
        topHeader.add(actionRow, BorderLayout.EAST);
        card.add(topHeader, BorderLayout.NORTH);

        // Sessions list grouped by day
        sessionsListPanel.setLayout(new BoxLayout(sessionsListPanel, BoxLayout.Y_AXIS));
        sessionsListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(sessionsListPanel);
        Ui.applyThinScrollBar(scroll);
        card.add(scroll, BorderLayout.CENTER);

        p.add(card, BorderLayout.CENTER);
        return p;
    }

    private void buildStep2UI() {
        courseChipsPanel.removeAll();
        sessionsListPanel.removeAll();
        selectedSessionIds.clear();

        // 1. Course chips with absent counts
        Map<String, Long> courseAbsentCounts = new LinkedHashMap<>();
        for (SessionAttendance sa : loadedSessions) {
            if (sa.status() == SessionStatus.ABSENT) {
                courseAbsentCounts.merge(sa.session().courseCode(), 1L, Long::sum);
            }
        }

        for (Map.Entry<String, Long> entry : courseAbsentCounts.entrySet()) {
            String code = entry.getKey();
            long cnt = entry.getValue();

            JButton chip = new JButton(code + " (" + cnt + ")");
            chip.setFont(Theme.FONT_TINY);
            chip.setForeground(Theme.TEXT_MAIN);
            chip.setBackground(new Color(241, 245, 249));
            chip.setFocusPainted(false);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            chip.addActionListener(e -> toggleCourseSessions(code));
            courseChipsPanel.add(chip);
        }

        // 2. Group sessions by date
        Map<LocalDate, List<SessionAttendance>> byDate = new TreeMap<>();
        for (SessionAttendance sa : loadedSessions) {
            byDate.computeIfAbsent(sa.session().date(), d -> new ArrayList<>()).add(sa);
        }

        for (Map.Entry<LocalDate, List<SessionAttendance>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<SessionAttendance> daySessions = entry.getValue();

            // Day Header
            JPanel dayHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            dayHeader.setOpaque(true);
            dayHeader.setBackground(new Color(243, 244, 246));
            dayHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            JCheckBox dayCheck = new JCheckBox(date.format(DATE_FMT));
            dayCheck.setFont(Theme.FONT_BODY_BOLD);
            dayCheck.setForeground(Theme.TEXT_MAIN);
            dayCheck.setOpaque(false);
            dayCheck.addActionListener(e -> toggleDaySessions(daySessions, dayCheck.isSelected()));

            dayHeader.add(dayCheck);
            sessionsListPanel.add(dayHeader);

            // Sessions in that day
            for (SessionAttendance sa : daySessions) {
                sessionsListPanel.add(createSessionRow(sa, dayCheck));
            }
            sessionsListPanel.add(Box.createVerticalStrut(8));
        }

        // Select absent sessions by default
        selectAllAbsent(true);
        updateSelectionSummary();

        courseChipsPanel.revalidate();
        sessionsListPanel.revalidate();
        courseChipsPanel.repaint();
        sessionsListPanel.repaint();
    }

    private JPanel createSessionRow(SessionAttendance sa, JCheckBox dayCheckParent) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setPreferredSize(new Dimension(700, 48));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        ClassSession cs = sa.session();
        boolean isAbsent = (sa.status() == SessionStatus.ABSENT);

        JCheckBox check = new JCheckBox();
        check.setOpaque(false);
        check.setEnabled(isAbsent); // Disabled for PRESENT, MEDICAL_PENDING, etc.

        if (isAbsent && selectedSessionIds.contains(cs.id())) {
            check.setSelected(true);
        }

        check.addActionListener(e -> {
            if (check.isSelected()) {
                selectedSessionIds.add(cs.id());
            } else {
                selectedSessionIds.remove(cs.id());
            }
            updateSelectionSummary();
            validateCurrentStep();
        });

        // Left details
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(check);

        JLabel codeLbl = new JLabel(cs.courseCode());
        codeLbl.setFont(Theme.FONT_BODY_BOLD);
        codeLbl.setForeground(Theme.PRIMARY);

        JLabel titleLbl = new JLabel(cs.courseTitle() + " (" + cs.type() + ")");
        titleLbl.setFont(Theme.FONT_BODY);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        left.add(codeLbl);
        left.add(titleLbl);
        row.add(left, BorderLayout.WEST);

        // Right details: Time + Lecturer + Status Pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        right.setOpaque(false);

        JLabel timeLbl = new JLabel(cs.start().toString() + " - " + cs.end().toString() + " • " + cs.lecturer());
        timeLbl.setFont(Theme.FONT_SMALL);
        timeLbl.setForeground(Theme.TEXT_MUTED);

        Ui.Pill statusPill = switch (sa.status()) {
            case ABSENT -> new Ui.Pill("Absent", Theme.DANGER_BG, Theme.DANGER);
            case PRESENT -> new Ui.Pill("Present", Theme.SUCCESS_BG, Theme.SUCCESS);
            case MEDICAL_APPROVED -> new Ui.Pill("Medical Approved", Theme.INFO_BG, Theme.INFO);
            case MEDICAL_PENDING -> new Ui.Pill("Medical Pending", Theme.WARNING_BG, Theme.WARNING);
            case NOT_HELD -> new Ui.Pill("Not Held", new Color(241, 245, 249), Theme.TEXT_MUTED);
        };

        right.add(timeLbl);
        right.add(statusPill);
        row.add(right, BorderLayout.EAST);

        return row;
    }

    private void toggleCourseSessions(String code) {
        boolean anySelected = loadedSessions.stream()
                .filter(s -> s.session().courseCode().equalsIgnoreCase(code) && s.status() == SessionStatus.ABSENT)
                .anyMatch(s -> selectedSessionIds.contains(s.session().id()));

        boolean target = !anySelected;
        for (SessionAttendance sa : loadedSessions) {
            if (sa.session().courseCode().equalsIgnoreCase(code) && sa.status() == SessionStatus.ABSENT) {
                if (target) selectedSessionIds.add(sa.session().id());
                else selectedSessionIds.remove(sa.session().id());
            }
        }
        buildStep2UI();
        validateCurrentStep();
    }

    private void toggleDaySessions(List<SessionAttendance> daySessions, boolean select) {
        for (SessionAttendance sa : daySessions) {
            if (sa.status() == SessionStatus.ABSENT) {
                if (select) selectedSessionIds.add(sa.session().id());
                else selectedSessionIds.remove(sa.session().id());
            }
        }
        buildStep2UI();
        validateCurrentStep();
    }

    private void selectAllAbsent(boolean select) {
        for (SessionAttendance sa : loadedSessions) {
            if (sa.status() == SessionStatus.ABSENT) {
                if (select) selectedSessionIds.add(sa.session().id());
                else selectedSessionIds.remove(sa.session().id());
            }
        }
        updateSelectionSummary();
        validateCurrentStep();
    }

    private void updateSelectionSummary() {
        Set<String> distinctCourses = new HashSet<>();
        for (SessionAttendance sa : loadedSessions) {
            if (selectedSessionIds.contains(sa.session().id())) {
                distinctCourses.add(sa.session().courseCode());
            }
        }
        selectionSummaryLbl.setText(selectedSessionIds.size() + " sessions across " + distinctCourses.size() + " courses selected");
    }

    // --- STEP 3: MEDICAL DETAILS & ATTACHMENTS ---
    private JPanel createStep3Panel() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);

        // Left Form & DropZone
        Ui.Card formCard = new Ui.Card();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel("Medical Certificate Details");
        titleLbl.setFont(Theme.FONT_SUBTITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(titleLbl);
        formCard.add(Box.createVerticalStrut(16));

        JPanel f1 = createFormField("Reason Category:", reasonCombo);
        f1.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(f1);
        formCard.add(Box.createVerticalStrut(12));

        JPanel f2 = createFormField("Doctor / Hospital Name:", doctorField);
        f2.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(f2);
        formCard.add(Box.createVerticalStrut(12));

        JPanel f3 = createFormField("Certificate Number:", certNoField);
        f3.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(f3);
        formCard.add(Box.createVerticalStrut(12));

        notesArea.setFont(Theme.FONT_BODY);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setPreferredSize(new Dimension(300, 60));
        notesScroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1));
        JPanel f4 = createFormField("Optional Notes:", notesScroll);
        f4.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(f4);
        formCard.add(Box.createVerticalStrut(16));

        // Certificate Upload Zone
        dropZone = new DropZone(List.of("pdf", "jpg", "png"), MedicalPolicy.MAX_FILES, MedicalPolicy.MAX_FILE_MB);
        dropZone.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropZone.setOnError(err -> {
            if (err != null) {
                step3ErrorLbl.setText(err);
                step3ErrorLbl.setVisible(true);
            } else {
                step3ErrorLbl.setVisible(false);
            }
            validateCurrentStep();
        });
        formCard.add(dropZone);
        formCard.add(Box.createVerticalStrut(14));

        // Declaration check
        confirmBox.setFont(Theme.FONT_SMALL);
        confirmBox.setForeground(Theme.TEXT_MAIN);
        confirmBox.setOpaque(false);
        confirmBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBox.addActionListener(e -> validateCurrentStep());
        formCard.add(confirmBox);

        step3ErrorLbl.setFont(Theme.FONT_TINY);
        step3ErrorLbl.setForeground(Theme.DANGER);
        step3ErrorLbl.setVisible(false);
        step3ErrorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(step3ErrorLbl);

        JScrollPane leftScroll = new JScrollPane(formCard);
        Ui.applyThinScrollBar(leftScroll);
        p.add(leftScroll, BorderLayout.CENTER);

        // Right Review Panel
        Ui.Card reviewCard = new Ui.Card();
        reviewCard.setPreferredSize(new Dimension(320, 440));
        reviewCard.setLayout(new BorderLayout());
        reviewCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
        reviewPanel.setOpaque(false);

        JScrollPane reviewScroll = new JScrollPane(reviewPanel);
        Ui.applyThinScrollBar(reviewScroll);
        reviewCard.add(reviewScroll, BorderLayout.CENTER);
        p.add(reviewCard, BorderLayout.EAST);

        return p;
    }

    private JPanel createFormField(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        int h = comp instanceof JScrollPane ? 64 : 36;
        p.setMaximumSize(new Dimension(Short.MAX_VALUE, h));
        p.setPreferredSize(new Dimension(500, h));

        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_BODY_BOLD);
        l.setForeground(Theme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(170, h));

        comp.setFont(Theme.FONT_BODY);
        if (comp instanceof JTextField tf) {
            tf.setPreferredSize(new Dimension(200, 36));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        } else if (comp instanceof JComboBox<?> cb) {
            cb.setPreferredSize(new Dimension(200, 36));
            cb.setBackground(Color.WHITE);
        }
        p.add(l, BorderLayout.WEST);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void buildReviewPanel() {
        reviewPanel.removeAll();

        JLabel title = new JLabel("Submission Review");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        reviewPanel.add(title);
        reviewPanel.add(Box.createVerticalStrut(18));

        String rangeStr = calendar.getRangeStart().format(DATE_FMT) + " to " + calendar.getRangeEnd().format(DATE_FMT);
        reviewPanel.add(createReviewItem("Date Range", rangeStr));
        reviewPanel.add(Box.createVerticalStrut(14));

        reviewPanel.add(createReviewItem("Covered Sessions", selectedSessionIds.size() + " absent sessions"));
        reviewPanel.add(Box.createVerticalStrut(14));

        Set<String> courses = new LinkedHashSet<>();
        for (SessionAttendance sa : loadedSessions) {
            if (selectedSessionIds.contains(sa.session().id())) {
                courses.add(sa.session().courseCode());
            }
        }
        String coursesStr = courses.isEmpty() ? "None" : String.join(", ", courses);
        reviewPanel.add(createReviewItem("Courses", coursesStr));
        reviewPanel.add(Box.createVerticalStrut(14));

        String doc = doctorField.getText().trim().isEmpty() ? "Not specified" : doctorField.getText().trim();
        reviewPanel.add(createReviewItem("Doctor / Hospital", doc));
        reviewPanel.add(Box.createVerticalStrut(14));

        String cert = certNoField.getText().trim().isEmpty() ? "Not specified" : certNoField.getText().trim();
        reviewPanel.add(createReviewItem("Certificate Number", cert));
        reviewPanel.add(Box.createVerticalGlue());

        reviewPanel.revalidate();
        reviewPanel.repaint();
    }

    private JPanel createReviewItem(String key, String val) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel k = new JLabel(key);
        k.setFont(Theme.FONT_TINY);
        k.setForeground(Theme.TEXT_MUTED);
        k.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel v = new JLabel("<html><body style='width: 230px;'>" + val + "</body></html>");
        v.setFont(Theme.FONT_BODY_BOLD);
        v.setForeground(Theme.TEXT_MAIN);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(k);
        p.add(Box.createVerticalStrut(3));
        p.add(v);
        return p;
    }

    // --- STEP NAVIGATION & VALIDATION ---
    private void validateCurrentStep() {
        boolean valid = false;
        if (currentStep == 1) {
            valid = (calendar.getRangeStart() != null && calendar.getRangeEnd() != null && !loadedSessions.isEmpty());
        } else if (currentStep == 2) {
            valid = (!selectedSessionIds.isEmpty());
        } else if (currentStep == 3) {
            boolean hasFiles = dropZone != null && !dropZone.getSelectedFiles().isEmpty();
            boolean hasDoctor = !doctorField.getText().trim().isEmpty();
            boolean hasCert = !certNoField.getText().trim().isEmpty();
            valid = (hasFiles && hasDoctor && hasCert && confirmBox.isSelected());
        }

        nextBtn.setEnabled(valid);
        backBtn.setEnabled(currentStep > 1);
        nextBtn.setText(currentStep == 3 ? "Submit request" : "Next");
    }

    private void nextStep() {
        if (currentStep == 1) {
            buildStep2UI();
            currentStep = 2;
            wizardLayout.show(stepsContainer, "Step2");
        } else if (currentStep == 2) {
            buildReviewPanel();
            currentStep = 3;
            wizardLayout.show(stepsContainer, "Step3");
        } else if (currentStep == 3) {
            handleSubmit();
            return;
        }
        stepIndicator.setStep(currentStep);
        validateCurrentStep();
    }

    private void prevStep() {
        if (currentStep == 2) {
            currentStep = 1;
            wizardLayout.show(stepsContainer, "Step1");
        } else if (currentStep == 3) {
            currentStep = 2;
            wizardLayout.show(stepsContainer, "Step2");
        }
        stepIndicator.setStep(currentStep);
        validateCurrentStep();
    }

    private void handleSubmit() {
        step3ErrorLbl.setVisible(false);

        List<File> files = dropZone.getSelectedFiles();
        if (files.isEmpty()) {
            step3ErrorLbl.setText("Please attach at least 1 medical certificate document.");
            step3ErrorLbl.setVisible(true);
            return;
        }
        if (doctorField.getText().trim().isEmpty() || certNoField.getText().trim().isEmpty()) {
            step3ErrorLbl.setText("Doctor/Hospital name and Certificate Number are required.");
            step3ErrorLbl.setVisible(true);
            return;
        }
        if (!confirmBox.isSelected()) {
            step3ErrorLbl.setText("You must confirm the truthfulness of the declaration.");
            step3ErrorLbl.setVisible(true);
            return;
        }

        int res = JOptionPane.showConfirmDialog(this,
                "Submit medical excuse request covering " + selectedSessionIds.size() + " absent sessions?",
                "Confirm Medical Submission", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (res != JOptionPane.YES_OPTION) return;

        nextBtn.setEnabled(false);
        dropZone.setUploading(true, 40);

        MedicalRequest draft = new MedicalRequest(
                null,
                null,
                studentId,
                calendar.getRangeStart(),
                calendar.getRangeEnd(),
                new ArrayList<>(selectedSessionIds),
                (String) reasonCombo.getSelectedItem(),
                doctorField.getText().trim(),
                certNoField.getText().trim(),
                notesArea.getText().trim(),
                null,
                null,
                MedicalStatus.PENDING,
                null
        );

        new SwingWorker<MedicalRequest, Void>() {
            @Override
            protected MedicalRequest doInBackground() throws Exception {
                return medicalService.submit(draft, files);
            }

            @Override
            protected void done() {
                nextBtn.setEnabled(true);
                dropZone.setUploading(false, 0);
                try {
                    get();
                    Toast.show(MedicalWizardView.this, "Medical request submitted");
                    if (onComplete != null) onComplete.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MedicalWizardView.this,
                            "Failed to submit medical request: " + ex.getMessage(),
                            "Submission Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void styleButton(JButton btn, boolean isPrimary, boolean isDanger) {
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (isPrimary) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(Theme.PRIMARY);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        } else if (isDanger) {
            btn.setForeground(Theme.DANGER);
            btn.setBackground(Theme.DANGER_BG);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
        } else {
            btn.setForeground(Theme.TEXT_MAIN);
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
        }
    }
}
