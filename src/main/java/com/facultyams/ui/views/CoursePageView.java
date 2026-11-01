package com.facultyams.ui.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    private final JPanel contentPanel = new JPanel(new GridBagLayout());
    private final List<CollapsibleSection> activeSections = new ArrayList<>();
    private final JLabel titleLbl = new JLabel();
    private final JLabel codeLbl = new JLabel();

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
        leftColumn.add(createMaterialsCard(course));

        rightColumn.add(createEnrollmentCard());
        rightColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        rightColumn.add(createLecturerCard(course));
        rightColumn.add(Box.createVerticalStrut(Theme.CARD_GAP));
        rightColumn.add(createScheduleCard(course));

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
            sectionsContainer.add(Box.createVerticalStrut(Theme.CARD_GAP));
        }
        addAssignmentContent(courseAssignments);
    }

    private void addMaterialContent(List<StudentMaterial> materials) {
        List<StudentMaterial> visible = materials.stream()
                .filter(material -> matchesMaterialCategory(material, materialCategory)).toList();
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_2, 0));
        chips.setOpaque(false);
        for (String category : List.of("All", "Slides", "Tutorials", "Lab sheets", "Past papers", "Links")) {
            JButton chip = new JButton(category);
            chip.setFont(Theme.FONT_SMALL);
            chip.setForeground(category.equals(materialCategory) ? Theme.PRIMARY : Theme.TEXT_MUTED);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(category.equals(materialCategory)
                            ? Theme.PRIMARY : Theme.CARD_BORDER),
                    Theme.createPadding(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2)));
            chip.setContentAreaFilled(false);
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(e -> {
                materialCategory = category;
                rebuildCourseContent();
            });
            chips.add(chip);
        }
        sectionsContainer.add(chips);
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
            sectionsContainer.add(Box.createVerticalStrut(Theme.CARD_GAP));
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
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(Theme.createPadding(Theme.SPACE_3, Theme.CARD_PADDING,
                Theme.SPACE_3, Theme.CARD_PADDING));
        JButton toggle = new JButton((expanded ? "\u2304  " : "\u203a  ") + title
                + "  \u00b7  " + materials.size() + " items");
        toggle.setFont(Theme.FONT_BODY_BOLD);
        toggle.setForeground(Theme.TEXT_MAIN);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        materials.forEach(material -> rows.add(createMaterialRow(material)));
        rows.setVisible(expanded);
        toggle.addActionListener(e -> {
            rows.setVisible(!rows.isVisible());
            toggle.setText((rows.isVisible() ? "\u2304  " : "\u203a  ") + title
                    + "  \u00b7  " + materials.size() + " items");
            weekCard.revalidate();
            weekCard.repaint();
        });
        header.add(toggle, BorderLayout.CENTER);
        weekCard.add(header, BorderLayout.NORTH);
        weekCard.add(rows, BorderLayout.CENTER);
        weekCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        return weekCard;
    }

    private JPanel createMaterialRow(StudentMaterial material) {
        JPanel row = new JPanel(new BorderLayout(Theme.CARD_GAP, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                Theme.createPadding(Theme.SPACE_3, Theme.CARD_PADDING, Theme.SPACE_3, Theme.CARD_PADDING)));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("\u25a1  " + material.getTitle());
        title.setFont(Theme.FONT_BODY_BOLD);
        title.setForeground(Theme.TEXT_MAIN);
        JLabel notes = new JLabel(material.getNotes().isBlank() ? material.getType() : material.getNotes());
        notes.setFont(Theme.FONT_SMALL);
        notes.setForeground(Theme.TEXT_MUTED);
        JLabel date = new JLabel(material.getUploadedAt() == null ? "Date not provided"
                : material.getUploadedAt().toLocalDate().toString());
        date.setFont(Theme.FONT_SMALL);
        date.setForeground(Theme.TEXT_MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(Theme.SPACE_1));
        text.add(notes);
        text.add(date);
        JButton open = new JButton(material.isLink() ? "Open link" : "Open / Download");
        open.setFont(Theme.FONT_BODY);
        open.setForeground(Theme.PRIMARY);
        open.setBorder(BorderFactory.createEmptyBorder());
        open.setContentAreaFilled(false);
        open.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        open.addActionListener(e -> {
            if (onOpenMaterial != null) onOpenMaterial.accept(material.getCourseCode(), material.getId());
            else if (onViewAllMaterials != null) onViewAllMaterials.accept(material.getCourseCode());
            else if (onOpenMaterials != null) onOpenMaterials.run();
        });
        row.add(text, BorderLayout.CENTER);
        row.add(open, BorderLayout.EAST);
        return row;
    }

    private void addAssignmentContent(List<StudentAssignment> assignments) {
        Ui.Card assignmentCard = createCard("Assignments");
        if (assignments.isEmpty()) {
            assignmentCard.add(createMessage("No assignments available.",
                    "Assignments for this course will appear here."));
        } else {
            for (StudentAssignment assignment : assignments) {
                JPanel row = new JPanel(new BorderLayout(Theme.CARD_GAP, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER));
                JLabel detail = bodyLabel(assignment.title() + "  \u00b7  due "
                        + (assignment.dueAt() == null ? "Not provided" : assignment.dueAt().toString()));
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
                action.addActionListener(e -> {
                    if (onOpenAssignment != null) onOpenAssignment.accept(assignment.courseCode(), assignment.id());
                    else if (onViewAllMaterials != null) onViewAllMaterials.accept(assignment.courseCode());
                });
                row.add(detail, BorderLayout.CENTER);
                row.add(action, BorderLayout.EAST);
                assignmentCard.add(row);
            }
        }
        sectionsContainer.add(Box.createVerticalStrut(Theme.CARD_GAP));
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

    private record LoadedSection(Section section, List<Activity> activities) {}
    private record CourseContent(List<LoadedSection> sections, List<StudentMaterial> materials,
                                 List<StudentAssignment> assignments) {}
}
