package com.facultyams.ui.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Icon;
import javax.swing.JComponent;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.Activity;
import com.facultyams.model.Assignment;
import com.facultyams.model.CourseSummary;
import com.facultyams.model.Section;
import com.facultyams.model.student.StudentAssignment;
import com.facultyams.model.student.StudentMaterial;
import com.facultyams.service.DataService;
import com.facultyams.service.StudentAssignmentService;
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
    private Consumer<String> onViewAllMaterials;
    private BiConsumer<String, Long> onOpenAssignment;
    private BiConsumer<String, Integer> onOpenMaterial;
    private final DataService studentDataService = DataService.getInstance();

    private final JPanel contentPanel = new ResponsiveContentPanel();
    private final List<CollapsibleSection> activeSections = new ArrayList<>();
    private final JLabel titleLbl = new JLabel();
    private final JLabel codeLbl = new JLabel();
    private JPanel currentLeftColumn;
    private JPanel currentRightColumn;

    private CourseSummary currentCourse;
    private List<LoadedSection> courseSections = List.of();
    private List<StudentMaterial> courseMaterials = List.of();
    private List<StudentAssignment> courseAssignments = List.of();
    private String materialCategory = "All";
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

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());

        add(createHeader(), BorderLayout.NORTH);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(Theme.COURSE_CARD_GAP, 0,
                Theme.PAGE_PADDING, 0));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                updateColumnLayout();
            }
        });
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
        headerPanel.add(Box.createVerticalStrut(Theme.SPACE_2));
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
        currentLeftColumn = leftColumn;
        currentRightColumn = rightColumn;

        leftColumn.add(createOverviewCard(course));
        addColumnGap(leftColumn);

        Ui.Card contentCard = createCard("Course content");
        sectionsContainer = new JPanel();
        sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.Y_AXIS));
        sectionsContainer.setOpaque(false);
        sectionsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionsContainer.setMinimumSize(new Dimension(0, 0));
        sectionsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        contentCard.add(sectionsContainer);
        leftColumn.add(contentCard);
        addColumnGap(leftColumn);
        Ui.Card assessmentCard = createCard("Assessment");
        assessmentCard.add(createMessage("Assessment details are not provided.",
                "Components, weights, and assessment dates are not available for this course."));
        leftColumn.add(assessmentCard);
        addColumnGap(leftColumn);
        leftColumn.add(createMaterialsCard(course));

        rightColumn.add(createEnrollmentCard());
        addColumnGap(rightColumn);
        rightColumn.add(createLecturerCard(course));
        addColumnGap(rightColumn);
        rightColumn.add(createScheduleCard(course));

        updateColumnLayout();
        contentPanel.revalidate();
        contentPanel.repaint();
        showContentMessage("Loading course content...", "Please wait while course sections are loaded.");

        new SwingWorker<CourseContent, Void>() {
            @Override
            protected CourseContent doInBackground() {
                List<LoadedSection> loaded = new ArrayList<>();
                for (Section section : courseService.sections(course.id())) {
                    loaded.add(new LoadedSection(section, courseService.activities(section.id())));
                }
                List<StudentMaterial> materials = studentDataService.getMaterialsForCourse(course.code());
                List<StudentAssignment> assignments =
                        new StudentAssignmentService().getAssignmentsForStudent(course.code());
                return new CourseContent(loaded, materials, assignments);
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

    public void setOnViewAllMaterials(Consumer<String> action) {
        onViewAllMaterials = action;
    }

    public void setOnOpenAssignment(BiConsumer<String, Long> action) {
        onOpenAssignment = action;
    }

    public void setOnOpenMaterial(BiConsumer<String, Integer> action) {
        onOpenMaterial = action;
    }

    private void showEmptyPage(String title, String detail) {
        loadGeneration++;
        currentCourse = null;
        currentLeftColumn = null;
        currentRightColumn = null;
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

        setCardMaximumWidth(emptyCard);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(emptyCard, c);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private Ui.Card createMaterialsCard(CourseSummary course) {
        Ui.Card card = createCard("Course materials");
        card.add(createMessage("Course resources are grouped by week above.",
                "Open the materials page to see every file and reference."));
        JButton openButton = new JButton("View all materials");
        openButton.setFont(Theme.FONT_BODY_BOLD);
        openButton.setForeground(Theme.PRIMARY);
        openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openButton.setHorizontalAlignment(SwingConstants.LEFT);
        openButton.setBorder(Theme.createPadding(Theme.SPACE_2, 0, 0, 0));
        openButton.setContentAreaFilled(false);
        openButton.setFocusPainted(true);
        openButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openButton.addActionListener(e -> {
            if (onViewAllMaterials != null) onViewAllMaterials.accept(course.code());
            else if (onOpenMaterials != null) onOpenMaterials.run();
        });
        card.add(Box.createVerticalStrut(Theme.SPACE_2));
        card.add(openButton);
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
        card.add(Box.createVerticalStrut(Theme.COURSE_ROW_GAP));

        JTextArea hint = wrappedText("Enrollment actions are available from the enrollment page.",
                Theme.TEXT_MUTED);
        hint.setRows(2);
        card.add(hint);
        if (onManageEnrollment != null) {
            PrimaryButton manageButton = new PrimaryButton("Manage enrollment");
            manageButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            manageButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.CONTROL_HEIGHT));
            manageButton.setMinimumSize(new Dimension(0, Theme.CONTROL_HEIGHT));
            manageButton.addActionListener(e -> onManageEnrollment.run());
            card.add(Box.createVerticalStrut(Theme.COURSE_ROW_GAP));
            card.add(manageButton);
        }
        return card;
    }

    private JPanel createLecturerCard(CourseSummary course) {
        Ui.Card card = createCard("Lecturer");
        addLecturerField(card, "Name", valueOr(course.lecturerName(), "Not provided"), null);
        addLecturerField(card, "Email", valueOr(course.lecturerEmail(), "Not provided"),
                course.lecturerEmail());
        addLecturerField(card, "Office", "Not provided", null);
        return card;
    }

    private JPanel createScheduleCard(CourseSummary course) {
        Ui.Card card = createCard("Schedule");
        String schedule = valueOr(course.scheduleTime(), "No sessions scheduled");
        card.add(wrappedText(schedule, Theme.TEXT_MAIN));
        return card;
    }

    private JPanel createColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setMinimumSize(new Dimension(0, 0));
        column.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        card.add(Box.createVerticalStrut(Theme.COURSE_CARD_GAP));
        setCardMaximumWidth(card);
        return card;
    }

    private void addInfoRow(JPanel card, String labelText, String valueText) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMinimumSize(new Dimension(0, 40));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        label.setPreferredSize(new Dimension(160, 24));
        JTextArea value = new JTextArea(valueOr(valueText, "Not provided"));
        value.setEditable(false);
        value.setFocusable(false);
        value.setLineWrap(true);
        value.setWrapStyleWord(true);
        value.setOpaque(false);
        value.setFont(Theme.FONT_BODY);
        value.setForeground(Theme.TEXT_MAIN);
        value.setBorder(BorderFactory.createEmptyBorder());
        value.setColumns(1);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.weightx = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.insets = new Insets(Theme.SPACE_2, 0, Theme.SPACE_2, Theme.SPACE_2);
        row.add(label, labelConstraints);
        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = 0;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.insets = new Insets(Theme.SPACE_2, 0, Theme.SPACE_2, 0);
        row.add(value, valueConstraints);
        card.add(row);
        javax.swing.JSeparator separator = new javax.swing.JSeparator();
        separator.setForeground(Theme.CARD_BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(separator);
    }

    private void addLecturerField(JPanel card, String labelText, String valueText, String email) {
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(label);
        card.add(Box.createVerticalStrut(Theme.SPACE_1));
        JLabel value = new JLabel(valueText);
        value.setFont(Theme.FONT_BODY);
        value.setForeground(email == null || "Not provided".equals(valueText) ? Theme.TEXT_MAIN : Theme.PRIMARY);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setToolTipText(valueText);
        if (email != null && !email.isBlank()) {
            value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            value.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    try {
                        if (!java.awt.Desktop.isDesktopSupported()
                                || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                            throw new UnsupportedOperationException("No email application is available.");
                        }
                        java.awt.Desktop.getDesktop().mail(java.net.URI.create("mailto:" + email));
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Could not open lecturer email client", ex);
                    }
                }
            });
        }
        card.add(value);
        card.add(Box.createVerticalStrut(Theme.COURSE_ROW_GAP));
    }

    private JTextArea wrappedText(String text, java.awt.Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(Theme.FONT_BODY);
        area.setForeground(color);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setColumns(1);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMinimumSize(new Dimension(0, 0));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return area;
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
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return message;
    }

    private void showContentMessage(String title, String detail) {
        if (sectionsContainer == null) return;
        sectionsContainer.removeAll();
        sectionsContainer.add(createMessage(title, detail));
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }

    private void addColumnGap(JPanel column) {
        Component gap = Box.createVerticalStrut(Theme.COURSE_CARD_GAP);
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.COURSE_CARD_GAP));
        column.add(gap);
    }

    private void setCardMaximumWidth(JComponent card) {
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMinimumSize(new Dimension(0, card.getMinimumSize().height));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    private void updateColumnLayout() {
        if (currentLeftColumn == null || currentRightColumn == null) return;
        contentPanel.removeAll();
        int width = contentPanel.getWidth();
        boolean stacked = width > 0 && width < 960;
        currentLeftColumn.setMinimumSize(new Dimension(0, 0));
        currentRightColumn.setMinimumSize(new Dimension(0, 0));
        currentRightColumn.setPreferredSize(stacked ? null : new Dimension(360, 1));
        currentRightColumn.setMaximumSize(stacked
                ? new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
                : new Dimension(360, Integer.MAX_VALUE));
        if (stacked) {
            addColumn(currentLeftColumn, 0, 1, 0, 0, Theme.COURSE_CARD_GAP);
            addColumn(currentRightColumn, 0, 1, 1, 0, 0);
            addFiller(2);
        } else {
            addColumn(currentLeftColumn, 0, 1, 0, Theme.COURSE_CARD_GAP, 0);
            addColumn(currentRightColumn, 1, 0, 0, 0, 0);
            addFiller(1);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addColumn(JPanel column, int gridX, double weight, int gridY,
                           int rightGap, int bottomGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridX;
        c.gridy = gridY;
        c.weightx = weight;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, bottomGap, rightGap);
        contentPanel.add(column, c);
    }

    private void addFiller(int gridY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridY;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPanel.add(Box.createGlue(), c);
    }

    private void buildSectionsUI(CourseSummary course, CourseContent data) {
        if (sectionsContainer == null || currentCourse == null
                || !currentCourse.id().equals(course.id())) {
            return;
        }

        sectionsContainer.removeAll();
        activeSections.clear();
        courseMaterials = data.materials();
        courseAssignments = data.assignments();
        courseSections = data.sections();
        courseMaterials = data.materials();
        courseAssignments = data.assignments();
        populateCourseContent();
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }

    private void populateCourseContent() {
        addMaterialContent(courseMaterials);
        if (courseSections.isEmpty() && courseMaterials.isEmpty()) {
            sectionsContainer.add(createMessage("No materials uploaded yet",
                    "Course topics and resources will appear here when available."));
        }
        Set<String> collapsedIds = currentCourse == null ? Set.of()
                : COLLAPSED_STATE_CACHE.computeIfAbsent(currentCourse.id(), key -> new HashSet<>());
        for (int i = 0; i < courseSections.size(); i++) {
            LoadedSection loaded = courseSections.get(i);
            CollapsibleSection sectionComp = new CollapsibleSection(
                    loaded.section().id(), loaded.section().title(), i == 0, this::toggleAllSections);
            for (Activity activity : loaded.activities()) {
                sectionComp.addActivityRow(new ActivityRow(
                        activity, studentId, submissionService, onAssignmentSelected));
            }
            if (collapsedIds.contains(loaded.section().id())) sectionComp.setExpanded(false, false);
            sectionComp.setAlignmentX(Component.LEFT_ALIGNMENT);
            activeSections.add(sectionComp);
            sectionsContainer.add(sectionComp);
            sectionsContainer.add(Box.createVerticalStrut(Theme.COURSE_CARD_GAP));
        }
        addAssignmentContent(courseAssignments);
    }

    private void addMaterialContent(List<StudentMaterial> materials) {
        List<StudentMaterial> visible = materials.stream()
                .filter(material -> matchesMaterialCategory(material, materialCategory)).toList();
        JPanel chips = new JPanel(new WrapLayout(FlowLayout.LEFT, Theme.SPACE_2, Theme.SPACE_2));
        chips.setOpaque(false);
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        for (String category : List.of("All", "Slides", "Tutorials", "Lab sheets", "Past papers", "Links")) {
            JButton chip = new JButton(category);
            chip.setFont(Theme.FONT_SMALL);
            chip.setForeground(category.equals(materialCategory) ? Theme.PRIMARY : Theme.TEXT_MUTED);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(category.equals(materialCategory)
                            ? Theme.PRIMARY : Theme.CARD_BORDER),
                    Theme.createPadding(Theme.SPACE_1, Theme.SPACE_2,
                            Theme.SPACE_1, Theme.SPACE_2)));
            chip.setBackground(category.equals(materialCategory) ? Theme.PRIMARY_LIGHT : Theme.CARD_BG);
            chip.setContentAreaFilled(true);
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.setPreferredSize(new Dimension(chip.getPreferredSize().width, 28));
            chip.addActionListener(e -> {
                materialCategory = category;
                rebuildCourseContent();
            });
            chips.add(chip);
        }
        sectionsContainer.add(chips);
        sectionsContainer.add(Box.createVerticalStrut(Theme.COURSE_CARD_GAP));
        if (visible.isEmpty()) {
            sectionsContainer.add(createMessage(materials.isEmpty() ? "No materials uploaded yet" : "No materials found",
                    materials.isEmpty() ? "Course materials will appear here when they are published."
                            : "There are no materials in this category."));
            return;
        }

        Map<Integer, List<StudentMaterial>> weeks = new java.util.TreeMap<>();
        List<StudentMaterial> other = new ArrayList<>();
        for (StudentMaterial material : visible) {
            if (material.getWeekNo() == null || material.getWeekNo() < 1) other.add(material);
            else weeks.computeIfAbsent(material.getWeekNo(), ignored -> new ArrayList<>()).add(material);
        }
        Integer latestWeek = weeks.isEmpty() ? null : weeks.keySet().stream().max(Integer::compareTo).orElse(null);
        for (var entry : weeks.entrySet()) {
            sectionsContainer.add(createMaterialWeek("Week " + entry.getKey(), entry.getValue(),
                    entry.getKey().equals(latestWeek)));
            sectionsContainer.add(Box.createVerticalStrut(Theme.COURSE_CARD_GAP));
        }
        if (!other.isEmpty()) sectionsContainer.add(createMaterialWeek("Other", other, weeks.isEmpty()));
    }

    private boolean matchesMaterialCategory(StudentMaterial material, String category) {
        if ("All".equals(category)) return true;
        String text = (material.getTitle() + " " + material.getNotes() + " " + material.getType())
                .toLowerCase(java.util.Locale.ROOT);
        return switch (category) {
            case "Slides" -> text.contains("slide") || text.contains(".ppt");
            case "Tutorials" -> text.contains("tutorial");
            case "Lab sheets" -> text.contains("lab");
            case "Past papers" -> text.contains("past paper") || text.contains("exam paper");
            case "Links" -> material.isLink();
            default -> true;
        };
    }

    private JPanel createMaterialWeek(String title, List<StudentMaterial> materials, boolean expanded) {
        Ui.Card weekCard = new Ui.Card(Theme.RADIUS_CARD, Theme.CARD_BG, Theme.CARD_BORDER);
        weekCard.setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(Theme.COURSE_ICON_GAP, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                Theme.createPadding(Theme.SPACE_2, Theme.COURSE_ROW_PADDING,
                        Theme.SPACE_2, Theme.COURSE_ROW_PADDING)));
        header.setPreferredSize(new Dimension(0, 44));
        JButton toggle = new JButton(new ChevronIcon(expanded));
        toggle.setPreferredSize(new Dimension(28, 28));
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel weekTitle = new JLabel(title);
        weekTitle.setFont(Theme.FONT_BODY_BOLD);
        weekTitle.setForeground(Theme.TEXT_MAIN);
        JPanel weekLabel = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        weekLabel.setOpaque(false);
        weekLabel.add(toggle, BorderLayout.WEST);
        weekLabel.add(weekTitle, BorderLayout.CENTER);
        JLabel count = new JLabel(materials.size() + (materials.size() == 1 ? " item" : " items"));
        count.setFont(Theme.FONT_SMALL);
        count.setForeground(Theme.TEXT_MUTED);
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        for (int i = 0; i < materials.size(); i++) {
            rows.add(createMaterialRow(materials.get(i), i > 0));
        }
        rows.setVisible(expanded);
        toggle.addActionListener(e -> {
            rows.setVisible(!rows.isVisible());
            toggle.setIcon(new ChevronIcon(rows.isVisible()));
            weekCard.revalidate();
            weekCard.repaint();
        });
        header.add(weekLabel, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        weekCard.add(header, BorderLayout.NORTH);
        weekCard.add(rows, BorderLayout.CENTER);
        weekCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        weekCard.setMinimumSize(new Dimension(0, 0));
        weekCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return weekCard;
    }

    private JPanel createMaterialRow(StudentMaterial material, boolean drawDivider) {
        JPanel row = new JPanel(new BorderLayout(Theme.COURSE_ICON_GAP, 0));
        row.setOpaque(false);
        row.setBorder(drawDivider
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                        Theme.createPadding(Theme.COURSE_ROW_PADDING, Theme.COURSE_ROW_PADDING,
                                Theme.COURSE_ROW_PADDING, Theme.COURSE_ROW_PADDING))
                : Theme.createPadding(Theme.COURSE_ROW_PADDING, Theme.COURSE_ROW_PADDING,
                        Theme.COURSE_ROW_PADDING, Theme.COURSE_ROW_PADDING));
        String extension = material.getReference().replace('\\', '/');
        int dot = extension.lastIndexOf('.');
        String fileType = material.isLink() ? "link"
                : dot < 0 ? "document" : extension.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        JLabel icon = new JLabel(new MaterialIcon(fileType));
        icon.setPreferredSize(new Dimension(24, 24));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(ellipsize(material.getTitle(), 100));
        title.setFont(Theme.FONT_BODY_BOLD);
        title.setForeground(Theme.PRIMARY);
        title.setToolTipText(material.getTitle());
        JLabel notes = new JLabel(ellipsize(material.getNotes().isBlank()
                ? material.getType() : material.getNotes(), 120));
        notes.setFont(Theme.FONT_SMALL);
        notes.setForeground(Theme.TEXT_MUTED);
        notes.setToolTipText(material.getNotes());
        JLabel date = new JLabel(material.getUploadedAt() == null ? "Date not provided"
                : material.getUploadedAt().toLocalDate().toString());
        date.setFont(Theme.FONT_SMALL);
        date.setForeground(Theme.TEXT_SUBTLE);
        text.add(title);
        text.add(Box.createVerticalStrut(Theme.SPACE_1));
        text.add(notes);
        text.add(Box.createVerticalStrut(Theme.SPACE_1));
        text.add(date);
        JButton open = new JButton(material.isLink() ? "Open link" : "Open / Download");
        open.setFont(Theme.FONT_BODY);
        open.setForeground(Theme.PRIMARY);
        open.setBorder(BorderFactory.createEmptyBorder());
        open.setContentAreaFilled(false);
        open.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        open.setMaximumSize(new Dimension(112, Theme.CONTROL_HEIGHT));
        open.addActionListener(e -> {
            if (onOpenMaterial != null) onOpenMaterial.accept(material.getCourseCode(), material.getId());
            else if (onViewAllMaterials != null) onViewAllMaterials.accept(material.getCourseCode());
            else if (onOpenMaterials != null) onOpenMaterials.run();
        });
        row.add(icon, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(open, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMinimumSize(new Dimension(0, 80));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return row;
    }

    private void addAssignmentContent(List<StudentAssignment> assignments) {
        Ui.Card assignmentCard = createCard("Assignments");
        if (assignments.isEmpty()) {
            assignmentCard.add(createMessage("No assignments available",
                    "Assignments for this course will appear here."));
        } else {
            for (StudentAssignment assignment : assignments) {
                JPanel row = new JPanel(new BorderLayout(Theme.COURSE_ICON_GAP, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                        Theme.createPadding(Theme.SPACE_3, Theme.SPACE_2,
                                Theme.SPACE_3, Theme.SPACE_2)));
                JLabel icon = new JLabel(new MaterialIcon("upload"));
                icon.setPreferredSize(new Dimension(24, 24));
                JPanel detail = new JPanel();
                detail.setOpaque(false);
                detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
                JLabel assignmentTitle = new JLabel(ellipsize(assignment.title(), 100));
                assignmentTitle.setFont(Theme.FONT_BODY_BOLD);
                assignmentTitle.setForeground(Theme.PRIMARY);
                assignmentTitle.setToolTipText(assignment.title());
                String opened = assignment.openedAt() == null ? "Not provided"
                        : assignment.openedAt().format(java.time.format.DateTimeFormatter
                        .ofPattern("EEEE, d MMMM yyyy, h:mm a"));
                String due = assignment.dueAt() == null ? "Not provided"
                        : assignment.dueAt().format(java.time.format.DateTimeFormatter
                        .ofPattern("EEEE, d MMMM yyyy, h:mm a"));
                JLabel dates = new JLabel("<html><b>Opened:</b> " + opened
                        + " &nbsp;&nbsp; <b>Due:</b> " + due + "</html>");
                dates.setFont(Theme.FONT_SMALL);
                dates.setForeground(Theme.TEXT_MUTED);
                detail.add(assignmentTitle);
                detail.add(Box.createVerticalStrut(Theme.SPACE_1));
                detail.add(dates);
                boolean closed = assignment.dueAt() != null
                        && LocalDateTime.now().isAfter(assignment.allowLate() && assignment.cutoffAt() != null
                        ? assignment.cutoffAt() : assignment.dueAt());
                String actionText = closed || "GRADED".equalsIgnoreCase(assignment.status()) ? "View"
                        : assignment.hasSubmission() ? "Resubmit" : "Submit";
                JButton action = new JButton(actionText);
                action.setFont(Theme.FONT_BODY_BOLD);
                action.setForeground(Theme.PRIMARY);
                action.setBorder(BorderFactory.createEmptyBorder());
                action.setContentAreaFilled(false);
                action.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                action.setToolTipText(closed ? "The submission deadline has passed." : null);
                action.setPreferredSize(new Dimension(96, Theme.CONTROL_HEIGHT));
                action.addActionListener(e -> {
                    if (onOpenAssignment != null) onOpenAssignment.accept(assignment.courseCode(), assignment.id());
                    else if (onViewAllMaterials != null) onViewAllMaterials.accept(assignment.courseCode());
                });
                row.add(icon, BorderLayout.WEST);
                row.add(detail, BorderLayout.CENTER);
                row.add(action, BorderLayout.EAST);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setMinimumSize(new Dimension(0, 64));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                assignmentCard.add(row);
            }
        }
        sectionsContainer.add(Box.createVerticalStrut(Theme.COURSE_CARD_GAP));
        setCardMaximumWidth(assignmentCard);
        sectionsContainer.add(assignmentCard);
    }

    private void rebuildCourseContent() {
        if (currentCourse == null || sectionsContainer == null) return;
        sectionsContainer.removeAll();
        activeSections.clear();
        populateCourseContent();
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

    private String ellipsize(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, Math.max(0, maxLength - 1)) + "\u2026";
    }

    private final class ResponsiveContentPanel extends JPanel implements Scrollable {
        private ResponsiveContentPanel() {
            super(new GridBagLayout());
        }

        @Override public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class WrapLayout extends FlowLayout {
        private WrapLayout(int alignment, int horizontalGap, int verticalGap) {
            super(alignment, horizontalGap, verticalGap);
        }

        @Override public Dimension preferredLayoutSize(java.awt.Container target) {
            return layoutSize(target, true);
        }

        @Override public Dimension minimumLayoutSize(java.awt.Container target) {
            Dimension preferred = layoutSize(target, false);
            preferred.width -= getHgap() + 1;
            return preferred;
        }

        private Dimension layoutSize(java.awt.Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                Insets insets = target.getInsets();
                int horizontalInsets = insets.left + insets.right + getHgap() * 2;
                int maxWidth = targetWidth - horizontalInsets;
                Dimension dimensions = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                for (Component component : target.getComponents()) {
                    if (!component.isVisible()) continue;
                    Dimension size = preferred ? component.getPreferredSize() : component.getMinimumSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + size.width > maxWidth) {
                        dimensions.width = Math.max(dimensions.width, rowWidth);
                        dimensions.height += rowHeight + getVgap();
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth > 0) rowWidth += getHgap();
                    rowWidth += size.width;
                    rowHeight = Math.max(rowHeight, size.height);
                }
                dimensions.width = Math.max(dimensions.width, rowWidth);
                dimensions.height += rowHeight;
                dimensions.width += horizontalInsets;
                dimensions.height += insets.top + insets.bottom + getVgap() * 2;
                return dimensions;
            }
        }
    }

    private static final class ChevronIcon implements Icon {
        private final boolean expanded;

        private ChevronIcon(boolean expanded) { this.expanded = expanded; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.translate(x, y);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            if (expanded) {
                g2.drawLine(4, 6, 8, 10);
                g2.drawLine(8, 10, 12, 6);
            } else {
                g2.drawLine(6, 4, 10, 8);
                g2.drawLine(10, 8, 6, 12);
            }
            g2.dispose();
        }
    }

    private static final class MaterialIcon implements Icon {
        private final String kind;

        private MaterialIcon(String kind) { this.kind = kind; }
        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 24; }
        @Override public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.translate(x, y);
            g2.setColor("pdf".equals(kind) ? new java.awt.Color(13, 148, 136) : Theme.PRIMARY);
            g2.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            if ("link".equals(kind)) {
                g2.drawRoundRect(2, 7, 11, 9, 5, 5);
                g2.drawRoundRect(11, 7, 11, 9, 5, 5);
                g2.drawLine(8, 12, 16, 12);
            } else if ("upload".equals(kind)) {
                g2.drawRoundRect(4, 3, 16, 18, 2, 2);
                g2.drawLine(12, 17, 12, 8);
                g2.drawLine(8, 12, 12, 8);
                g2.drawLine(12, 8, 16, 12);
            } else {
                g2.drawRoundRect(4, 2, 15, 20, 2, 2);
                g2.drawLine(8, 9, 15, 9);
                g2.drawLine(8, 13, 15, 13);
                g2.drawLine(8, 17, 13, 17);
            }
            g2.dispose();
        }
    }

    private record LoadedSection(Section section, List<Activity> activities) {}
    private record CourseContent(List<LoadedSection> sections, List<StudentMaterial> materials,
                                 List<StudentAssignment> assignments) {}
}
