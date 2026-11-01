package com.facultyams.ui.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.Activity;
import com.facultyams.model.Assignment;
import com.facultyams.model.CourseSummary;
import com.facultyams.model.Section;
import com.facultyams.service.StudentCourseService;
import com.facultyams.service.SubmissionService;
import com.facultyams.ui.components.ActivityRow;
import com.facultyams.ui.components.CollapsibleSection;
import com.facultyams.ui.components.PrimaryButton;

/** Student course details and weekly course content. */
public class CoursePageView extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CoursePageView.class.getName());
    private static final Map<String, Set<String>> COLLAPSED_STATE_CACHE = new HashMap<>();

    private final StudentCourseService courseService;
    private final SubmissionService submissionService;
    private final String studentId;
    private final Runnable onBack;
    private final Consumer<Assignment> onAssignmentSelected;
    private final Runnable onManageEnrollment;
    private Runnable onOpenMaterials;

    private final JPanel contentPanel = new JPanel(new GridBagLayout());
    private final List<CollapsibleSection> activeSections = new ArrayList<>();
    private final JLabel titleLbl = new JLabel();
    private final JLabel codeLbl = new JLabel();

    private CourseSummary currentCourse;
    private JPanel sectionsContainer;
    private long loadGeneration;

    public CoursePageView(StudentCourseService courseService, SubmissionService submissionService,
                          String studentId, Runnable onBack, Consumer<Assignment> onAssignmentSelected) {
        this(courseService, submissionService, studentId, onBack, onAssignmentSelected, null);
    }

    public CoursePageView(StudentCourseService courseService, SubmissionService submissionService,
                          String studentId, Runnable onBack, Consumer<Assignment> onAssignmentSelected,
                          Runnable onManageEnrollment) {
        this.courseService = courseService;
        this.submissionService = submissionService;
        this.studentId = studentId;
        this.onBack = onBack;
        this.onAssignmentSelected = onAssignmentSelected;
        this.onManageEnrollment = onManageEnrollment;

        setLayout(new BorderLayout(0, Theme.CARD_GAP));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 4, Theme.PAGE_PADDING / 2, 4));

        add(createHeader(), BorderLayout.NORTH);
        contentPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        Ui.applyThinScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel backLink = new JLabel("< Back to courses");
        backLink.setFont(Theme.FONT_BODY_BOLD);
        backLink.setForeground(Theme.PRIMARY);
        backLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onBack != null) onBack.run();
            }
        });

        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);
        codeLbl.setFont(Theme.FONT_BODY);
        codeLbl.setForeground(Theme.TEXT_MUTED);

        headerPanel.add(backLink);
        headerPanel.add(Box.createVerticalStrut(Theme.SPACE_3));
        headerPanel.add(titleLbl);
        headerPanel.add(Box.createVerticalStrut(Theme.SPACE_1));
        headerPanel.add(codeLbl);
        return headerPanel;
    }

    public void loadCourse(CourseSummary course) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> loadCourse(course));
            return;
        }
        if (course == null || course.id() == null || course.id().isBlank()
                || course.code() == null || course.code().isBlank()) {
            showUnavailable();
            return;
        }

        currentCourse = course;
        activeSections.clear();
        contentPanel.removeAll();
        long generation = ++loadGeneration;
        titleLbl.setText(valueOr(course.title(), "Course details are not available"));
        codeLbl.setText(course.code() + " \u2022 " + valueOr(course.semester(), "Not provided"));

        JPanel leftColumn = createColumn();
        JPanel rightColumn = createColumn();
        addColumn(leftColumn, 0, 0.65, new Insets(0, 0, 0, Theme.CARD_GAP));
        addColumn(rightColumn, 1, 0.35, new Insets(0, 0, 0, 0));

        leftColumn.add(createOverviewCard(course));
        leftColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));

        Ui.Card contentCard = createCard("Course content");
        sectionsContainer = new JPanel();
        sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.Y_AXIS));
        sectionsContainer.setOpaque(false);
        contentCard.add(sectionsContainer);
        leftColumn.add(contentCard);
        leftColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        Ui.Card assessmentCard = createCard("Assessment");
        assessmentCard.add(createMessage("Assessment details are not provided.",
                "Components, weights, and assessment dates are not available for this course."));
        leftColumn.add(assessmentCard);
        leftColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        leftColumn.add(createMaterialsCard());

        rightColumn.add(createEnrollmentCard());
        rightColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        rightColumn.add(createLecturerCard(course));
        rightColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        rightColumn.add(createScheduleCard(course));

        contentPanel.revalidate();
        contentPanel.repaint();
        showContentMessage("Loading course content...", "Please wait while course sections are loaded.");

        new SwingWorker<List<LoadedSection>, Void>() {
            @Override
            protected List<LoadedSection> doInBackground() {
                List<LoadedSection> loaded = new ArrayList<>();
                for (Section section : courseService.sections(course.id())) {
                    loaded.add(new LoadedSection(section, courseService.activities(section.id())));
                }
                return loaded;
            }

            @Override
            protected void done() {
                if (generation != loadGeneration) return;
                try {
                    buildSectionsUI(course, get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showContentMessage("Course content could not be loaded",
                            "The request was interrupted. Please reopen this course.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    LOGGER.log(Level.SEVERE, "Failed to load course content for " + course.code(), cause);
                    showContentMessage("Course content could not be loaded",
                            "Please try reopening this course.");
                }
            }
        }.execute();
    }

    public void showUnavailable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showUnavailable);
            return;
        }
        showEmptyPage("Course details are not available",
                "This course could not be found or its details are unavailable.");
    }

    public void showLoadFailure(Throwable cause) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showLoadFailure(cause));
            return;
        }
        LOGGER.log(Level.SEVERE, "Failed to load course details", cause);
        showEmptyPage("Course details could not be loaded",
                "Please check your connection and try reopening this course.");
    }

    public void setOnOpenMaterials(Runnable action) {
        onOpenMaterials = action;
    }

    private void showEmptyPage(String title, String detail) {
        loadGeneration++;
        currentCourse = null;
        activeSections.clear();
        sectionsContainer = null;
        titleLbl.setText(title);
        codeLbl.setText("");
        contentPanel.removeAll();

        Ui.Card emptyCard = createCard(title);
        emptyCard.add(createMessage(detail, "Use the button below to return to your course list."));
        JButton backButton = new JButton("Back to courses");
        backButton.setFont(Theme.FONT_BODY_BOLD);
        backButton.setForeground(Theme.PRIMARY);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyCard.add(Box.createVerticalStrut(Theme.SPACE_3));
        emptyCard.add(backButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(emptyCard, c);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private Ui.Card createMaterialsCard() {
        Ui.Card card = createCard("Course materials");
        card.add(createMessage("View available files and references on the Course Materials page.",
                "That page lists files and references for your enrolled courses."));
        if (onOpenMaterials != null) {
            JButton openButton = new JButton("Open course materials");
            openButton.setFont(Theme.FONT_BODY_BOLD);
            openButton.setForeground(Theme.PRIMARY);
            openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            openButton.setHorizontalAlignment(SwingConstants.LEFT);
            openButton.setBorder(Theme.createPadding(Theme.SPACE_2, 0, 0, 0));
            openButton.setContentAreaFilled(false);
            openButton.setFocusPainted(true);
            openButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            openButton.addActionListener(e -> onOpenMaterials.run());
            card.add(Box.createVerticalStrut(Theme.SPACE_2));
            card.add(openButton);
        }
        return card;
    }

    private JPanel createOverviewCard(CourseSummary course) {
        Ui.Card card = createCard("Overview");
        addInfoRow(card, "Course code", valueOr(course.code(), "Not provided"));
        addInfoRow(card, "Credits", course.credits() > 0 ? Integer.toString(course.credits()) : "Not provided");
        addInfoRow(card, "Course type", valueOr(course.courseType(), "Not provided"));
        addInfoRow(card, "Level and semester", valueOr(course.semester(), "Not provided"));
        addInfoRow(card, "Description", "Not provided");
        addInfoRow(card, "Prerequisites", "Not provided");
        return card;
    }

    private JPanel createEnrollmentCard() {
        Ui.Card card = createCard("Enrollment");
        JLabel status = new Ui.Pill("Enrolled", Theme.SUCCESS_BG, Theme.SUCCESS);
        status.setFont(Theme.FONT_SMALL);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setBorder(Theme.createPadding(4, Theme.SPACE_3, 4, Theme.SPACE_3));
        card.add(status);
        card.add(Box.createVerticalStrut(Theme.SPACE_3));

        JLabel hint = bodyLabel("Enrollment actions are available from the enrollment page.");
        card.add(hint);
        if (onManageEnrollment != null) {
            PrimaryButton manageButton = new PrimaryButton("Manage enrollment");
            manageButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            manageButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.CONTROL_HEIGHT));
            manageButton.addActionListener(e -> onManageEnrollment.run());
            card.add(Box.createVerticalStrut(Theme.SPACE_3));
            card.add(manageButton);
        }
        return card;
    }

    private JPanel createLecturerCard(CourseSummary course) {
        Ui.Card card = createCard("Lecturer");
        addInfoRow(card, "Name", valueOr(course.lecturerName(), "Not provided"));
        addInfoRow(card, "Email", valueOr(course.lecturerEmail(), "Not provided"));
        addInfoRow(card, "Office", "Not provided");
        return card;
    }

    private JPanel createScheduleCard(CourseSummary course) {
        Ui.Card card = createCard("Schedule");
        addInfoRow(card, "Sessions", valueOr(course.scheduleTime(), "Not scheduled"));
        return card;
    }

    private JPanel createColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        return column;
    }

    private Ui.Card createCard(String title) {
        Ui.Card card = new Ui.Card(Theme.RADIUS_CARD, Theme.CARD_BG, Theme.CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.createPadding(Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.FONT_SECTION);
        heading.setForeground(Theme.TEXT_MAIN);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(Theme.SPACE_3));
        return card;
    }

    private void addInfoRow(JPanel card, String labelText, String valueText) {
        JPanel row = new JPanel(new BorderLayout(Theme.CARD_GAP, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        JLabel value = new JLabel(valueOr(valueText, "Not provided"), SwingConstants.RIGHT);
        value.setFont(Theme.FONT_BODY);
        value.setForeground(Theme.TEXT_MAIN);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        row.setBorder(Theme.createPadding(Theme.SPACE_2, 0, Theme.SPACE_2, 0));
        card.add(row);
    }

    private JLabel bodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BODY);
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea createMessage(String title, String detail) {
        JTextArea message = new JTextArea(title + "\n" + detail);
        message.setEditable(false);
        message.setFocusable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setOpaque(false);
        message.setFont(Theme.FONT_BODY);
        message.setForeground(Theme.TEXT_MUTED);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setBorder(BorderFactory.createEmptyBorder());
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        return message;
    }

    private void showContentMessage(String title, String detail) {
        if (sectionsContainer == null) return;
        sectionsContainer.removeAll();
        sectionsContainer.add(createMessage(title, detail));
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }

    private void addColumn(JPanel column, int gridX, double weight, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridX;
        c.gridy = 0;
        c.weightx = weight;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        contentPanel.add(column, c);
    }

    private void buildSectionsUI(CourseSummary course, List<LoadedSection> sections) {
        if (sectionsContainer == null || currentCourse == null
                || !currentCourse.id().equals(course.id())) {
            return;
        }

        sectionsContainer.removeAll();
        activeSections.clear();
        if (sections.isEmpty()) {
            sectionsContainer.add(createMessage("Course content is not available yet.",
                    "Weekly topics and activities have not been added for this course."));
        } else {
            Set<String> collapsedIds = COLLAPSED_STATE_CACHE.computeIfAbsent(course.id(), key -> new HashSet<>());
            for (int i = 0; i < sections.size(); i++) {
                LoadedSection loaded = sections.get(i);
                boolean isFirst = i == 0;
                CollapsibleSection sectionComp = new CollapsibleSection(
                        loaded.section().id(), loaded.section().title(), isFirst, this::toggleAllSections);
                for (Activity activity : loaded.activities()) {
                    sectionComp.addActivityRow(new ActivityRow(
                            activity, studentId, submissionService, onAssignmentSelected));
                }
                if (collapsedIds.contains(loaded.section().id())) {
                    sectionComp.setExpanded(false, false);
                }
                sectionComp.setAlignmentX(Component.LEFT_ALIGNMENT);
                activeSections.add(sectionComp);
                sectionsContainer.add(sectionComp);
                sectionsContainer.add(Box.createVerticalStrut(Theme.CARD_GAP));
            }
        }
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }

    private void toggleAllSections() {
        if (currentCourse == null || activeSections.isEmpty()) return;
        boolean target = activeSections.stream().noneMatch(CollapsibleSection::isExpanded);
        Set<String> collapsedIds = COLLAPSED_STATE_CACHE.computeIfAbsent(currentCourse.id(),
                key -> new HashSet<>());
        for (CollapsibleSection section : activeSections) {
            section.setExpanded(target, true);
            if (target) {
                collapsedIds.remove(section.getSectionId());
            } else {
                collapsedIds.add(section.getSectionId());
            }
        }
        updateToggleAllText();
    }

    private void updateToggleAllText() {
        if (!activeSections.isEmpty()) {
            boolean anyExpanded = activeSections.stream().anyMatch(CollapsibleSection::isExpanded);
            activeSections.get(0).setToggleAllText(anyExpanded ? "Collapse all" : "Expand all");
        }
    }

    public void refreshActivityStatuses() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshActivityStatuses);
            return;
        }
        for (CollapsibleSection section : activeSections) {
            section.updateAllStatus();
        }
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record LoadedSection(Section section, List<Activity> activities) {}
}
