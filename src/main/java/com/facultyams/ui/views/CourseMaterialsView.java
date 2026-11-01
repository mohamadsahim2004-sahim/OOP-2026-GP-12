package com.facultyams.ui.views;

import com.facultyams.model.student.StudentAssignment;
import com.facultyams.model.student.StudentMaterial;
import com.facultyams.model.student.StudentSubmissionDetails;
import com.facultyams.service.DataService;
import com.facultyams.service.StudentAssignmentService;
import com.facultyams.ui.components.BadgeLabel;
import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.components.ModernButton;
import com.facultyams.ui.components.PrimaryButton;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Duration;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Student course materials and assignment submissions. */
public class CourseMaterialsView extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CourseMaterialsView.class.getName());
    private static final int PAGE_SIZE = 10;
    private static final long FILE_LIMIT_BYTES = 20L * 1024 * 1024;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm");
    private static final DateTimeFormatter FULL_DATE_TIME =
            DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu, h:mm a");
    private static final Set<String> UPLOAD_TYPES = Set.of("pdf", "doc", "docx", "zip", "java");

    private final DataService dataService = DataService.getInstance();
    private final StudentAssignmentService assignmentService = new StudentAssignmentService();
    private final List<StudentMaterial> materials = new ArrayList<>();
    private final List<StudentAssignment> assignments = new ArrayList<>();
    private final MaterialModel materialModel = new MaterialModel();
    private final AssignmentModel assignmentModel = new AssignmentModel();
    private final JTable materialTable = new JTable(materialModel);
    private final JTable assignmentTable = new JTable(assignmentModel);
    private final TableRowSorter<MaterialModel> materialSorter = new TableRowSorter<>(materialModel);
    private final TableRowSorter<AssignmentModel> assignmentSorter = new TableRowSorter<>(assignmentModel);
    private final JPanel tableContainer = new JPanel(new BorderLayout());
    private final JPanel materialPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel assignmentPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JLabel materialPageLabel = new JLabel();
    private final JLabel assignmentPageLabel = new JLabel();
    private final JLabel materialStats = new JLabel("0 materials  \u00b7  0 courses");
    private final JComboBox<String> courseFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>(
            new String[]{"All", "Slides", "PDF", "Lab sheets", "Past papers", "Links", "Submissions"});
    private final JTextField searchField = new JTextField(22);
    private final JPanel tabContent = new JPanel(new BorderLayout());
    private final JPanel tabsBar = new JPanel(new BorderLayout());
    private final JPanel emptyContainer = new JPanel(new GridBagLayout());
    private final JButton viewAllButton = new JButton("View all materials");
    private final Map<String, Boolean> expandedWeeks = new LinkedHashMap<>();
    private RuntimeException assignmentLoadError;

    private int materialPage;
    private int assignmentPage;
    private int materialHoverRow = -1;
    private int assignmentHoverRow = -1;
    private boolean populatingFilter;
    private String pendingCourseCode;
    private Long pendingAssignmentId;
    private Integer pendingMaterialId;
    private String activeTab = "Materials";
    private Consumer<String> onViewCourseDetails;

    public CourseMaterialsView() {
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, Theme.PAGE_PADDING, 0));

        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(Theme.CARD_GAP));
        content.add(createMaterialsCard());

        JScrollPane pageScroll = new JScrollPane(content);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setOpaque(false);
        pageScroll.getViewport().setOpaque(false);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScroll, BorderLayout.CENTER);

        configureTables();
        configureFilters();
        reload();
    }

    public void setOnViewCourseDetails(Consumer<String> action) {
        onViewCourseDetails = action;
        updateCourseShortcut();
    }

    /** Select a course and show its course-content page. */
    public void openCourse(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) return;
        pendingCourseCode = courseCode;
        if (onViewCourseDetails != null) onViewCourseDetails.accept(courseCode);
    }

    public void selectCourse(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) return;
        pendingCourseCode = courseCode;
        selectCourseItem(courseCode);
    }

    public void showSubmissions() {
        activeTab = "Submissions";
        updateTabs();
    }

    public void openAssignment(String courseCode, long assignmentId) {
        if (courseCode == null || courseCode.isBlank()) return;
        pendingCourseCode = courseCode;
        pendingAssignmentId = assignmentId;
        selectCourseItem(courseCode);
        activeTab = "Submissions";
        updateTabs();
    }

    public void openMaterial(String courseCode, int materialId) {
        if (courseCode == null || courseCode.isBlank()) return;
        pendingCourseCode = courseCode;
        pendingMaterialId = materialId;
        activeTab = "Materials";
        selectCourseItem(courseCode);
        updateTabs();
    }

    /** Re-reads enrolled courses, materials, and assignments. */
    public void reload() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::reload);
            return;
        }
        loadRows();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(Theme.CARD_RADIUS);
        card.setLayout(new BorderLayout(Theme.CARD_GAP, 0));
        card.setBorder(Theme.createEmptyPadding(Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Course materials");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Lecture slides, tutorials, lab sheets, and course resources.");
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.TEXT_MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(Theme.SPACE_2));
        text.add(subtitle);
        card.add(text, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        materialStats.setFont(Theme.FONT_BODY);
        materialStats.setForeground(Theme.TEXT_MUTED);
        materialStats.setAlignmentX(Component.RIGHT_ALIGNMENT);
        viewAllButton.setFont(Theme.FONT_BODY_BOLD);
        viewAllButton.setForeground(Theme.PRIMARY);
        viewAllButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        viewAllButton.setContentAreaFilled(false);
        viewAllButton.setFocusPainted(false);
        viewAllButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAllButton.addActionListener(e -> openCourse(selectedCourseCode()));
        actions.add(materialStats);
        actions.add(Box.createVerticalStrut(Theme.SPACE_2));
        actions.add(viewAllButton);
        card.add(actions, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JPanel createMaterialsCard() {
        CardPanel card = new CardPanel(Theme.CARD_RADIUS);
        card.setLayout(new BorderLayout(0, Theme.CARD_GAP));
        card.setBorder(Theme.createEmptyPadding(Theme.SPACE_3, Theme.CARD_PADDING,
                Theme.SPACE_3, Theme.CARD_PADDING));

        tabsBar.setOpaque(false);
        tabsBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tabsBar, BorderLayout.NORTH);
        updateTabs();

        tabContent.setOpaque(false);
        tableContainer.setOpaque(false);
        tabContent.add(createToolbar(), BorderLayout.NORTH);
        tabContent.add(tableContainer, BorderLayout.CENTER);
        card.add(tabContent, BorderLayout.CENTER);
        return card;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(Theme.CARD_GAP, 0));
        toolbar.setOpaque(false);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filters.setOpaque(false);
        filters.add(controlLabel("Filter course"));
        filters.add(Box.createHorizontalStrut(Theme.SPACE_2));
        styleCombo(courseFilter, 220);
        filters.add(courseFilter);
        filters.add(Box.createHorizontalStrut(Theme.SPACE_3));
        filters.add(controlLabel("Type"));
        filters.add(Box.createHorizontalStrut(Theme.SPACE_2));
        styleCombo(typeFilter, 132);
        filters.add(typeFilter);

        JPanel search = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        search.setOpaque(false);
        searchField.setFont(Theme.FONT_BODY);
        searchField.setPreferredSize(new Dimension(260, Theme.CONTROL_HEIGHT));
        searchField.setMinimumSize(new Dimension(140, Theme.CONTROL_HEIGHT));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(0, Theme.SPACE_3, 0, Theme.SPACE_3)));
        searchField.putClientProperty("JTextField.placeholderText", "Search materials...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyMaterialFilters(); applyAssignmentFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyMaterialFilters(); applyAssignmentFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyMaterialFilters(); applyAssignmentFilters(); }
        });

        ModernButton searchButton = new ModernButton("Search", ModernButton.StyleType.PRIMARY);
        searchButton.setFont(Theme.FONT_BODY_BOLD);
        searchButton.setPreferredSize(new Dimension(96, Theme.CONTROL_HEIGHT));
        searchButton.addActionListener(e -> {
            applyMaterialFilters();
            applyAssignmentFilters();
        });
        search.add(searchField, BorderLayout.CENTER);
        search.add(searchButton, BorderLayout.EAST);
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(search, BorderLayout.EAST);
        return toolbar;
    }

    private JLabel controlLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BODY_BOLD);
        label.setForeground(Theme.TEXT_MAIN);
        return label;
    }

    private void styleCombo(JComboBox<String> combo, int width) {
        combo.setFont(Theme.FONT_BODY);
        combo.setPreferredSize(new Dimension(width, Theme.CONTROL_HEIGHT));
        combo.setMinimumSize(new Dimension(96, Theme.CONTROL_HEIGHT));
    }

    private void configureFilters() {
        courseFilter.addActionListener(e -> {
            if (!populatingFilter) {
                materialPage = 0;
                assignmentPage = 0;
                updateCourseShortcut();
                applyMaterialFilters();
                applyAssignmentFilters();
            }
        });
        typeFilter.addActionListener(e -> {
            materialPage = 0;
            applyMaterialFilters();
            applyAssignmentFilters();
        });
    }

    private void configureTables() {
        configureTable(materialTable, materialSorter, true);
        configureTable(assignmentTable, assignmentSorter, false);
        materialTable.setModel(materialModel);
        materialTable.setRowSorter(materialSorter);
        assignmentTable.setModel(assignmentModel);
        assignmentTable.setRowSorter(assignmentSorter);

        materialSorter.addRowSorterListener(sortListener(materialSorter, true));
        assignmentSorter.addRowSorterListener(sortListener(assignmentSorter, false));

        materialTable.getColumnModel().getColumn(2).setCellRenderer(new BadgeRenderer(materialTable, true));
        materialTable.getColumnModel().getColumn(5).setCellRenderer(new ActionRenderer(materialTable, true));
        materialTable.getColumnModel().getColumn(5).setCellEditor(new ActionEditor(true));
        assignmentTable.getColumnModel().getColumn(3).setCellRenderer(new BadgeRenderer(assignmentTable, false));
        assignmentTable.getColumnModel().getColumn(6).setCellRenderer(new ActionRenderer(assignmentTable, false));
        assignmentTable.getColumnModel().getColumn(6).setCellEditor(new ActionEditor(false));

        installHoverTracking(materialTable, true);
        installHoverTracking(assignmentTable, false);
        installWidths(materialTable, new int[]{30, 10, 12, 28, 12, 8});
        installWidths(assignmentTable, new int[]{20, 10, 15, 13, 14, 18, 10});
        updateHeaderSortIndicator(materialTable, materialSorter);
        updateHeaderSortIndicator(assignmentTable, assignmentSorter);
    }

    private <M extends AbstractTableModel> void configureTable(
            JTable table, TableRowSorter<M> sorter, boolean materialsTable) {
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setFont(Theme.FONT_BODY);
        table.setForeground(Theme.TEXT_MAIN);
        table.setBackground(Theme.CARD_BG);
        table.setSelectionBackground(Theme.PRIMARY_LIGHT);
        table.setSelectionForeground(Theme.TEXT_MAIN);
        table.setRowSorter(sorter);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new HeaderRenderer(table, materialsTable));
        for (int col = 0; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(new TextRenderer(table));
        }
        JScrollPane pane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getViewport().setBackground(Theme.CARD_BG);
        pane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel pagination = materialsTable ? materialPagination : assignmentPagination;
        JLabel pageLabel = materialsTable ? materialPageLabel : assignmentPageLabel;
        pagination.setOpaque(false);
        JButton previous = linkButton("Previous");
        JButton next = linkButton("Next");
        previous.addActionListener(e -> changePage(materialsTable, -1));
        next.addActionListener(e -> changePage(materialsTable, 1));
        pageLabel.setFont(Theme.FONT_SMALL);
        pageLabel.setForeground(Theme.TEXT_MUTED);
        pagination.add(previous);
        pagination.add(pageLabel);
        pagination.add(next);

        if (materialsTable) {
            tableContainer.add(pane, BorderLayout.CENTER);
            tableContainer.add(pagination, BorderLayout.SOUTH);
        }
    }

    private JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BODY);
        button.setForeground(Theme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_2, Theme.SPACE_2,
                Theme.SPACE_2, Theme.SPACE_2));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private RowSorterListener sortListener(TableRowSorter<?> sorter, boolean material) {
        return event -> {
            if (event.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                SwingUtilities.invokeLater(() -> refreshPage(material));
            }
            if (event.getType() == RowSorterEvent.Type.SORTED) {
                updateHeaderSortIndicator(material ? materialTable : assignmentTable, sorter);
            }
        };
    }

    private void updateHeaderSortIndicator(JTable table, TableRowSorter<?> sorter) {
        if (table.getTableHeader().getDefaultRenderer() instanceof HeaderRenderer renderer) {
            renderer.setSorter(sorter);
        }
        table.getTableHeader().repaint();
    }

    private void installHoverTracking(JTable table, boolean materialsTable) {
        table.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (materialsTable) materialHoverRow = row;
                else assignmentHoverRow = row;
                table.repaint();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (materialsTable) materialHoverRow = -1;
                else assignmentHoverRow = -1;
                table.repaint();
            }
        });
    }

    private void installWidths(JTable table, int[] percentages) {
        table.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int width = table.getParent() == null ? table.getWidth()
                        : table.getParent().getWidth();
                if (width <= 0) return;
                for (int col = 0; col < percentages.length; col++) {
                    table.getColumnModel().getColumn(col).setPreferredWidth(width * percentages[col] / 100);
                }
            }
        });
    }

    private void loadRows() {
        loadRows(true);
    }

    private void loadRows(boolean showLoadingState) {
        if (showLoadingState) showLoading();
        String selectedCode = pendingCourseCode != null ? pendingCourseCode : selectedCourseCode();
        new SwingWorker<RowData, Void>() {
            @Override protected RowData doInBackground() {
                List<String> courseOptions = dataService.getEnrolledCourses().stream()
                        .map(course -> course.getCode() + " - " + course.getTitle()).toList();
                List<StudentMaterial> loadedMaterials = dataService.getMaterialsForCourse("ALL");
                List<StudentAssignment> loadedAssignments = List.of();
                RuntimeException assignmentError = null;
                try {
                    loadedAssignments = assignmentService.getAssignmentsForStudent(null);
                } catch (RuntimeException ex) {
                    assignmentError = ex;
                    LOGGER.log(Level.SEVERE, "Could not load student assignments", ex);
                }
                return new RowData(loadedMaterials, loadedAssignments, courseOptions, assignmentError);
            }

            @Override protected void done() {
                try {
                    RowData data = get();
                    populatingFilter = true;
                    courseFilter.removeAllItems();
                    courseFilter.addItem("All courses");
                    data.courseOptions().forEach(courseFilter::addItem);
                    selectCourseItem(pendingCourseCode != null ? pendingCourseCode : selectedCode);
                    pendingCourseCode = null;
                    populatingFilter = false;
                    materials.clear();
                    materials.addAll(data.materials());
                    assignments.clear();
                    assignments.addAll(data.assignments());
                    assignmentLoadError = data.assignmentError();
                    materialModel.fireTableDataChanged();
                    assignmentModel.fireTableDataChanged();
                    updateCourseShortcut();
                    if ("Materials".equals(activeTab)) {
                        applyMaterialFilters();
                        if (pendingMaterialId != null) {
                            int requestedId = pendingMaterialId;
                            pendingMaterialId = null;
                            SwingUtilities.invokeLater(() -> openMaterial(requestedId));
                        }
                    } else {
                        applyAssignmentFilters();
                        if (pendingAssignmentId != null) {
                            long requestedId = pendingAssignmentId;
                            pendingAssignmentId = null;
                            assignments.stream().filter(item -> item.id() == requestedId).findFirst()
                                    .ifPresent(item -> SwingUtilities.invokeLater(() -> showAssignmentAction(item)));
                        }
                    }
                } catch (Exception ex) {
                    populatingFilter = false;
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    LOGGER.log(Level.SEVERE, "Failed to load student course materials", cause);
                    showErrorState("Course content could not be loaded",
                            loadingFailureMessage(cause), CourseMaterialsView.this::loadRows);
                }
            }
        }.execute();
    }

    private void updateAssignmentAfterSubmit(StudentAssignment previous) {
        LocalDateTime submittedAt = LocalDateTime.now();
        LocalDateTime due = previous.dueAt();
        String status = due != null && submittedAt.isAfter(due) ? "LATE" : "SUBMITTED";
        StudentAssignment updated = new StudentAssignment(previous.id(), previous.courseId(),
                previous.courseCode(), previous.courseName(), previous.title(), previous.description(),
                previous.dueAt(), previous.cutoffAt(), previous.allowLate(), previous.maxMarks(),
                previous.allowedTypes(), previous.maxFileMb(), previous.maxFiles(), status, submittedAt,
                null, "", "", previous.weekNo(), previous.openedAt());
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).id() == updated.id()) {
                assignments.set(i, updated);
                break;
            }
        }
        assignmentModel.fireTableDataChanged();
        renderWeekContent("Submissions".equals(activeTab));
    }

    private void showLoading() {
        tableContainer.removeAll();
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setOpaque(false);
        JPanel message = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SPACE_2, 0));
        message.setOpaque(false);
        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setPreferredSize(new Dimension(28, 4));
        spinner.setBorderPainted(false);
        spinner.setForeground(Theme.PRIMARY);
        JLabel label = new JLabel("Loading course content...");
        label.setFont(Theme.FONT_BODY);
        label.setForeground(Theme.TEXT_MUTED);
        message.add(spinner);
        message.add(label);
        loading.add(message);
        tableContainer.add(loading, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private void applyMaterialFilters() {
        materialPage = 0;
        renderWeekContent(false);
    }

    private void applyAssignmentFilters() {
        assignmentPage = 0;
        if (assignmentLoadError != null) {
            showErrorState("Submissions could not be loaded",
                    loadingFailureMessage(assignmentLoadError), this::loadRows);
            return;
        }
        renderWeekContent(true);
    }

    private RowFilter<Object, Object> materialFilter(boolean assignmentRows) {
        String selectedCourse = selectedCourseCode();
        String selectedType = (String) typeFilter.getSelectedItem();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        return new RowFilter<>() {
            @Override public boolean include(Entry<?, ?> entry) {
                int row = ((Number) entry.getIdentifier()).intValue();
                if (assignmentRows) {
                            if (row < 0 || row >= assignments.size()) return false;
                    StudentAssignment item = assignments.get(row);
                    return (selectedCourse == null || item.courseCode().equalsIgnoreCase(selectedCourse))
                            && (query.isEmpty() || contains(query, item.title(), item.courseCode(),
                            item.courseName(), item.description()));
                }
                if (row < 0 || row >= materials.size()) return false;
                StudentMaterial item = materials.get(row);
                boolean courseMatches = selectedCourse == null
                        || item.getCourseCode().equalsIgnoreCase(selectedCourse);
                boolean typeMatches = selectedType == null || selectedType.equals("All")
                        || item.getType().equalsIgnoreCase(selectedType);
                return courseMatches && typeMatches
                        && (query.isEmpty() || contains(query, item.getTitle(), item.getCourseCode(),
                        item.getCourseName(), item.getNotes()));
            }
        };
    }

    private boolean contains(String query, String... values) {
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private void renderWeekContent(boolean submissionsTab) {
        tableContainer.removeAll();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedType = (String) typeFilter.getSelectedItem();
        String courseCode = selectedCourseCode();
        Map<Integer, List<ContentItem>> groups = new TreeMap<>(
                Comparator.nullsLast(Comparator.naturalOrder()));

        if (!submissionsTab && !"Submissions".equals(selectedType)) {
            for (StudentMaterial material : materials) {
                if (courseCode != null && !courseCode.equalsIgnoreCase(material.getCourseCode())) continue;
                if (!matchesMaterialType(material, selectedType)) continue;
                if (!query.isEmpty() && !contains(query, material.getTitle(), material.getNotes())) continue;
                Integer weekNo = materialWeek(material);
                groups.computeIfAbsent(weekNo, ignored -> new ArrayList<>())
                        .add(ContentItem.forMaterial(material, weekNo));
            }
        }
        if (submissionsTab || selectedType == null
                || "All".equals(selectedType) || "Submissions".equals(selectedType)) {
            for (StudentAssignment assignment : assignments) {
                if (courseCode != null && !courseCode.equalsIgnoreCase(assignment.courseCode())) continue;
                if (!query.isEmpty() && !contains(query, assignment.title(), assignment.description())) continue;
                Integer weekNo = validWeek(assignment.weekNo()) ? assignment.weekNo() : null;
                groups.computeIfAbsent(weekNo, ignored -> new ArrayList<>())
                        .add(ContentItem.forAssignment(assignment, weekNo));
            }
        }

        int filteredMaterials = groups.values().stream().flatMap(List::stream)
                .filter(item -> item.material() != null).toList().size();
        int filteredAssignments = groups.values().stream().flatMap(List::stream)
                .filter(item -> item.assignment() != null).toList().size();
        long courseCount = groups.values().stream().flatMap(List::stream)
                .map(ContentItem::courseCode).distinct().count();
        materialStats.setText(submissionsTab
                ? filteredAssignments + " submissions  \u00b7  " + courseCount + " courses"
                : filteredMaterials + " materials  \u00b7  " + courseCount + " courses");

        if (groups.isEmpty()) {
            boolean noCourseMaterials = courseCode == null ? materials.isEmpty()
                    : materials.stream().noneMatch(m -> courseCode.equalsIgnoreCase(m.getCourseCode()));
            boolean typeOrSearchActive = !query.isEmpty()
                    || (selectedType != null && !"All".equals(selectedType));
            boolean filtersActive = courseCode != null || typeOrSearchActive;
            if (!submissionsTab && !"Submissions".equals(selectedType)
                    && noCourseMaterials && !typeOrSearchActive) {
                showEmpty("No materials uploaded yet for this course", "",
                        courseCode == null ? null : this::clearFilters);
            } else if (!submissionsTab && !typeOrSearchActive && filteredMaterials == 0
                    && filteredAssignments == 0 && materials.isEmpty()) {
                showEmpty("No materials uploaded yet for this course", "", null);
            } else if (filtersActive) {
                showEmpty(submissionsTab || "Submissions".equals(selectedType)
                                ? "No submissions found" : "No materials found",
                        "No course content matches the selected filters.", this::clearFilters);
            } else {
                showEmpty(submissionsTab || "Submissions".equals(selectedType)
                                ? "No submissions yet" : "No materials uploaded yet for this course",
                        "", null);
            }
            return;
        }

        WeekContentPanel content = new WeekContentPanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        int contentRow = 0;
        if (submissionsTab) {
            List<StudentAssignment> visibleAssignments = groups.values().stream().flatMap(List::stream)
                    .map(ContentItem::assignment).filter(java.util.Objects::nonNull).toList();
            addWeekListRow(content, createSubmissionSummary(visibleAssignments), contentRow++, 0);
        }
        addWeekListRow(content, createExpandControls(groups, courseCode), contentRow++, Theme.SPACE_2);

        Integer latestWeek = groups.keySet().stream().filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(null);
        for (Map.Entry<Integer, List<ContentItem>> entry : groups.entrySet()) {
            List<ContentItem> items = entry.getValue();
            items.sort(Comparator.comparingInt(ContentItem::sortOrder)
                    .thenComparing(ContentItem::sortDate, Comparator.nullsLast(Comparator.naturalOrder())));
            String weekKey = weekKey(courseCode, entry.getKey());
            boolean matchesSearch = !query.isEmpty();
            boolean expanded = expandedWeeks.computeIfAbsent(weekKey,
                    ignored -> latestWeek == null
                            ? entry.getKey() == null : entry.getKey() != null && entry.getKey().equals(latestWeek));
            if (matchesSearch) expanded = true;
            addWeekListRow(content, createWeekSection(entry.getKey(), items, weekKey, expanded,
                    matchesSearch), contentRow++, Theme.CARD_GAP);
        }
        content.setBorder(Theme.createEmptyPadding(Theme.SPACE_2, 0, Theme.SPACE_2, 0));
        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        tableContainer.add(scroll, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private void addWeekListRow(JPanel content, JComponent component, int row, int bottomGap) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, bottomGap, 0);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(component, constraints);
    }

    private boolean matchesMaterialType(StudentMaterial material, String selectedType) {
        if (selectedType == null || "All".equals(selectedType)) return true;
        if ("Submissions".equals(selectedType)) return false;
        String titleNotes = (material.getTitle() + " " + material.getNotes()).toLowerCase(Locale.ROOT);
        String extension = extension(material.getReference().replace('\\', '/'));
        return switch (selectedType) {
            case "Slides" -> Set.of("ppt", "pptx").contains(extension) || titleNotes.contains("slide");
            case "PDF" -> "pdf".equals(extension);
            case "Lab sheets" -> titleNotes.contains("lab") || titleNotes.contains("tutorial");
            case "Past papers" -> titleNotes.contains("past paper");
            case "Links" -> material.isLink();
            default -> true;
        };
    }

    private boolean validWeek(Integer weekNo) {
        return weekNo != null && weekNo > 0;
    }

    private Integer materialWeek(StudentMaterial material) {
        if (validWeek(material.getWeekNo())) return material.getWeekNo();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)\\bweek\\s*0*(\\d+)\\b")
                .matcher(material.getTitle() + " " + material.getNotes());
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "Invalid week number in material title: " + material.getTitle(), ex);
            return null;
        }
    }

    private String weekKey(String courseCode, Integer weekNo) {
        return (courseCode == null ? "ALL" : courseCode) + ":" + (weekNo == null ? "OTHER" : weekNo);
    }

    private JPanel createSubmissionSummary(List<StudentAssignment> visibleAssignments) {
        long pending = visibleAssignments.stream().filter(a -> !a.hasSubmission())
                .filter(a -> !"GRADED".equalsIgnoreCase(a.status())).count();
        long graded = visibleAssignments.stream().filter(a -> "GRADED".equalsIgnoreCase(a.status())).count();
        long submitted = visibleAssignments.stream().filter(StudentAssignment::hasSubmission)
                .filter(a -> !"GRADED".equalsIgnoreCase(a.status())).count();
        JPanel summary = new JPanel(new BorderLayout());
        summary.setOpaque(false);
        JLabel label = new JLabel(pending + " pending  \u00b7  " + submitted
                + " submitted  \u00b7  " + graded + " graded");
        label.setFont(Theme.FONT_BODY_BOLD);
        label.setForeground(Theme.TEXT_MUTED);
        summary.add(label, BorderLayout.WEST);
        summary.setBorder(Theme.createEmptyPadding(Theme.SPACE_2, 0, Theme.SPACE_3, 0));
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return summary;
    }

    private JPanel createExpandControls(Map<Integer, List<ContentItem>> groups, String courseCode) {
        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        JPanel links = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_3, 0));
        links.setOpaque(false);
        JButton expand = linkButton("Expand all");
        expand.addActionListener(e -> {
            groups.keySet().forEach(week -> expandedWeeks.put(weekKey(courseCode, week), true));
            renderWeekContent("Submissions".equals(activeTab));
        });
        JButton collapse = linkButton("Collapse all");
        collapse.addActionListener(e -> {
            groups.keySet().forEach(week -> expandedWeeks.put(weekKey(courseCode, week), false));
            renderWeekContent("Submissions".equals(activeTab));
        });
        links.add(expand);
        links.add(collapse);
        controls.add(links, BorderLayout.EAST);
        controls.setBorder(Theme.createEmptyPadding(0, 0, Theme.SPACE_2, 0));
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return controls;
    }

    private JPanel createWeekSection(Integer weekNo, List<ContentItem> items, String key,
                                     boolean expanded, boolean forceExpanded) {
        CardPanel section = new CardPanel(12);
        section.setLayout(new BorderLayout());
        section.setBackground(Theme.CARD_BG);
        section.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1));
        JPanel header = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        header.setBackground(Theme.CARD_BG);
        header.setBorder(Theme.createEmptyPadding(Theme.SPACE_3, Theme.SPACE_4,
                Theme.SPACE_3, Theme.SPACE_4));
        JButton chevron = new JButton(expanded ? "v" : ">") {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(Theme.PRIMARY_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        chevron.setFont(Theme.FONT_BODY_BOLD);
        chevron.setForeground(Theme.PRIMARY);
        chevron.setBorder(BorderFactory.createEmptyBorder());
        chevron.setFocusPainted(false);
        chevron.setContentAreaFilled(false);
        chevron.setOpaque(false);
        chevron.setPreferredSize(new Dimension(36, 36));
        chevron.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel title = new JLabel(weekNo == null ? "Other" : "Week " + weekNo);
        title.setFont(Theme.FONT_WEEK_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        JPanel heading = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.WEST);
        JLabel count = new JLabel(items.size() + (items.size() == 1 ? " item" : " items"));
        count.setFont(Theme.FONT_BODY);
        count.setForeground(Theme.TEXT_MUTED);
        heading.add(count, BorderLayout.EAST);
        header.add(chevron, BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        section.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (int i = 0; i < items.size(); i++) {
            JPanel row = items.get(i).material() != null
                    ? createMaterialContentRow(items.get(i).material())
                    : createSubmissionContentRow(items.get(i).assignment());
            if (i == 0) row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                    row.getBorder()));
            body.add(row);
        }
        body.setVisible(expanded);
        section.add(body, BorderLayout.CENTER);
        Runnable toggle = () -> {
            if (forceExpanded) return;
            boolean next = !body.isVisible();
            expandedWeeks.put(key, next);
            body.setVisible(next);
            chevron.setText(next ? "v" : ">");
            section.revalidate();
            section.repaint();
        };
        chevron.addActionListener(e -> toggle.run());
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle.run(); }
        });
        heading.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle.run(); }
        });
        title.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle.run(); }
        });
        count.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle.run(); }
        });
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return section;
    }

    private JPanel createMaterialContentRow(StudentMaterial material) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        row.setOpaque(true);
        row.setBackground(Theme.CARD_BG);
        row.setBorder(contentRowBorder());
        JPanel icon = fileTypeIcon(material);
        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_2, 0));
        titleArea.setOpaque(false);
        JLabel title = new JLabel(material.getTitle());
        title.setFont(Theme.FONT_BODY);
        title.setForeground(Theme.PRIMARY);
        JLabel tag = new JLabel(fileType(material));
        tag.setFont(Theme.FONT_BODY_BOLD);
        tag.setForeground(Theme.TEXT_MUTED);
        titleArea.add(title);
        titleArea.add(tag);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(titleArea, BorderLayout.NORTH);
        if (!material.getNotes().isBlank()) {
            JLabel notes = new JLabel(ellipsize(material.getNotes(), 100));
            notes.setFont(Theme.FONT_SMALL);
            notes.setForeground(Theme.TEXT_MUTED);
            center.add(notes, BorderLayout.SOUTH);
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_3, 0));
        actions.setOpaque(false);
        JLabel date = new JLabel(material.getUploadedAt() == null ? ""
                : material.getUploadedAt().format(DateTimeFormatter.ofPattern("d MMM uuuu")));
        date.setFont(Theme.FONT_SMALL);
        date.setForeground(Theme.TEXT_MUTED);
        String actionText = "File".equals(material.getType()) ? "Download"
                : material.isLink() ? "Open link" : "Open";
        JButton action = ghostButton(actionText, false);
        action.addActionListener(e -> openMaterial(material.getId()));
        actions.add(date);
        actions.add(action);
        row.add(icon, BorderLayout.WEST);
        row.add(center, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openMaterial(material.getId()); }
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(Theme.TABLE_HOVER); }
            @Override public void mouseExited(MouseEvent e) { row.setBackground(Theme.CARD_BG); }
        });
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        return row;
    }

    private JPanel fileTypeIcon(StudentMaterial material) {
        String type = fileType(material);
        Color color = switch (type) {
            case "PDF" -> Theme.FILE_PDF;
            case "DOC", "DOCX" -> Theme.FILE_DOC;
            case "PPT", "PPTX" -> Theme.FILE_PRESENTATION;
            case "ZIP" -> Theme.FILE_ARCHIVE;
            case "LINK" -> Theme.PRIMARY;
            default -> Theme.FILE_GENERIC;
        };
        return contentIcon(type, color);
    }

    private JPanel contentIcon(String text, Color color) {
        JPanel icon = new JPanel(new GridBagLayout());
        icon.setOpaque(true);
        icon.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 24));
        icon.setPreferredSize(new Dimension(36, 36));
        JLabel label = new JLabel(text);
        label.setFont(new Font(Theme.FONT_BODY.getName(), Font.BOLD, text.length() > 3 ? 9 : 11));
        label.setForeground(color);
        icon.add(label);
        return icon;
    }

    private String fileType(StudentMaterial material) {
        if (material.isLink()) return "LINK";
        String ext = extension(material.getReference().replace('\\', '/')).toUpperCase(Locale.ROOT);
        return ext.isBlank() ? "FILE" : ext;
    }

    private JPanel createSubmissionContentRow(StudentAssignment assignment) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        row.setOpaque(true);
        row.setBackground(Theme.CARD_BG);
        row.setBorder(contentRowBorder());
        row.add(contentIcon("UP", Theme.FILE_SUBMISSION), BorderLayout.WEST);
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(assignment.title());
        title.setFont(Theme.FONT_BODY);
        title.setForeground(Theme.PRIMARY);
        details.add(title);
        String opened = assignment.openedAt() == null ? "Not provided"
                : assignment.openedAt().format(FULL_DATE_TIME);
        String due = assignment.dueAt() == null ? "Not provided"
                : assignment.dueAt().format(FULL_DATE_TIME);
        JLabel dates = new JLabel("<html><b>Opened:</b> " + opened + " &nbsp;&nbsp; <b>Due:</b> " + due
                + (assignment.marks() == null ? "" : " &nbsp;&nbsp; <b>Grade:</b> " + assignment.marks())
                + "</html>");
        dates.setFont(Theme.FONT_SMALL);
        dates.setForeground(Theme.TEXT_MUTED);
        details.add(Box.createVerticalStrut(Theme.SPACE_1));
        details.add(dates);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        right.setOpaque(false);
        if (isDueSoon(assignment)) right.add(statusBadge("Due soon"));
        right.add(statusBadge(statusFor(assignment)));
        String actionName = actionFor(assignment);
        JButton action = assignmentActionButton(actionName);
        boolean closedAndUnsubmitted = !assignment.hasSubmission() && isPastCutoff(assignment);
        if (closedAndUnsubmitted) {
            action.setEnabled(false);
            action.setToolTipText("The submission cutoff has passed.");
        }
        action.addActionListener(e -> {
            if (!closedAndUnsubmitted) showAssignmentAction(assignment);
        });
        right.add(action);
        row.add(details, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!closedAndUnsubmitted) showAssignmentAction(assignment);
            }
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(Theme.TABLE_HOVER); }
            @Override public void mouseExited(MouseEvent e) { row.setBackground(Theme.CARD_BG); }
        });
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        return row;
    }

    private JLabel statusBadge(String text) {
        Color[] colors = badgeColors(text, false);
        JLabel badge = new JLabel(text) {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(colors[0]);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        badge.setFont(Theme.FONT_BODY_BOLD);
        badge.setForeground(colors[1]);
        badge.setOpaque(false);
        badge.setBorder(Theme.createEmptyPadding(Theme.SPACE_1, Theme.SPACE_2,
                Theme.SPACE_1, Theme.SPACE_2));
        return badge;
    }

    private javax.swing.border.Border contentRowBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                Theme.createEmptyPadding(Theme.SPACE_3, Theme.SPACE_4,
                        Theme.SPACE_3, Theme.SPACE_4));
    }

    private boolean isDueSoon(StudentAssignment assignment) {
        if (assignment.hasSubmission() || assignment.dueAt() == null) return false;
        Duration remaining = Duration.between(LocalDateTime.now(), assignment.dueAt());
        return !remaining.isNegative() && remaining.compareTo(Duration.ofHours(24)) <= 0;
    }

    private boolean isPastCutoff(StudentAssignment assignment) {
        LocalDateTime cutoff = assignment.cutoffAt() == null ? assignment.dueAt() : assignment.cutoffAt();
        return cutoff != null && LocalDateTime.now().isAfter(cutoff);
    }

    private void refreshPage(boolean materialsPage) {
        if (materialsPage) renderWeekContent(false);
        else renderWeekContent(true);
    }

    private void changePage(boolean materialsPage, int delta) {
        // Week-wise content is scrollable and does not paginate.
    }

    private void clearFilters() {
        populatingFilter = true;
        courseFilter.setSelectedIndex(0);
        typeFilter.setSelectedIndex(0);
        searchField.setText("");
        populatingFilter = false;
        applyMaterialFilters();
        applyAssignmentFilters();
    }

    private void showEmpty(String title, String detail, Runnable clearAction) {
        tableContainer.removeAll();
        emptyContainer.removeAll();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(Theme.FONT_SECTION);
        heading.setForeground(Theme.TEXT_MUTED);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel(detail == null ? "" : detail, SwingConstants.CENTER);
        hint.setFont(Theme.FONT_BODY);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(heading);
        if (detail != null && !detail.isBlank()) {
            content.add(Box.createVerticalStrut(Theme.SPACE_2));
            content.add(hint);
        }
        if (clearAction != null) {
            JButton clear = linkButton("Clear filters");
            clear.setAlignmentX(Component.CENTER_ALIGNMENT);
            clear.addActionListener(e -> clearAction.run());
            content.add(Box.createVerticalStrut(Theme.SPACE_2));
            content.add(clear);
        }
        emptyContainer.add(content);
        emptyContainer.setPreferredSize(new Dimension(0, 180));
        tableContainer.add(emptyContainer, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private void showErrorState(String title, String detail, Runnable retryAction) {
        tableContainer.removeAll();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(Theme.FONT_SECTION);
        heading.setForeground(Theme.TEXT_MAIN);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel(ellipsize(detail, 180), SwingConstants.CENTER);
        hint.setFont(Theme.FONT_BODY);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton retry = new JButton("Retry");
        retry.setFont(Theme.FONT_BODY_BOLD);
        retry.setForeground(Theme.PRIMARY);
        retry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                Theme.createEmptyPadding(Theme.SPACE_2, Theme.SPACE_3,
                        Theme.SPACE_2, Theme.SPACE_3)));
        retry.setBackground(Theme.CARD_BG);
        retry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(e -> retryAction.run());
        content.add(heading);
        content.add(Box.createVerticalStrut(Theme.SPACE_2));
        content.add(hint);
        content.add(Box.createVerticalStrut(Theme.SPACE_3));
        content.add(retry);
        JPanel centered = new JPanel(new GridBagLayout());
        centered.setOpaque(false);
        centered.add(content);
        tableContainer.add(centered, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private String loadingFailureMessage(Throwable error) {
        String deepestMessage = error.getMessage();
        for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) deepestMessage = cause.getMessage();
        }
        String lower = deepestMessage == null ? "" : deepestMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("assignments") && (lower.contains("doesn't exist")
                || lower.contains("does not exist") || lower.contains("unknown table"))) {
            return "The assignments table is missing. Apply migration 011_student_assignments.sql.";
        }
        if (lower.contains("unknown column") && (lower.contains("week_no") || lower.contains("uploaded_at"))) {
            return "Course material metadata is missing. Apply migration 011_student_assignments.sql.";
        }
        if (lower.contains("doesn't exist") || lower.contains("does not exist") || lower.contains("unknown table")) {
            return "A required course-content table is missing. Check the installed database migrations.";
        }
        return deepestMessage == null || deepestMessage.isBlank()
                ? "Please retry. See the application log for details." : deepestMessage;
    }

    private String selectedCourseCode() {
        Object selected = courseFilter.getSelectedItem();
        if (!(selected instanceof String value) || "All courses".equals(value)) return null;
        int separator = value.indexOf(" - ");
        return separator < 0 ? value.trim() : value.substring(0, separator).trim();
    }

    private void selectCourseItem(String code) {
        if (code == null) {
            courseFilter.setSelectedIndex(0);
            return;
        }
        for (int i = 1; i < courseFilter.getItemCount(); i++) {
            if (courseFilter.getItemAt(i).startsWith(code + " - ")) {
                courseFilter.setSelectedIndex(i);
                return;
            }
        }
        courseFilter.setSelectedIndex(0);
    }

    private void updateCourseShortcut() {
        viewAllButton.setEnabled(onViewCourseDetails != null && selectedCourseCode() != null);
    }

    private void updateTabs() {
        tabsBar.removeAll();
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_3, 0));
        tabs.setOpaque(false);
        tabs.add(tabButton("Materials"));
        tabs.add(tabButton("Submissions"));
        tabsBar.add(tabs, BorderLayout.WEST);
        JButton submissionLink = linkButton("Submit an assignment");
        submissionLink.setToolTipText("Open the submissions list to attach files to an assignment.");
        submissionLink.addActionListener(e -> {
            activeTab = "Submissions";
            updateTabs();
            applyAssignmentFilters();
        });
        tabsBar.add(submissionLink, BorderLayout.EAST);
        tabsBar.revalidate();
        tabsBar.repaint();
    }

    private JButton tabButton(String tab) {
        JButton button = new JButton(tab);
        button.setFont(Theme.FONT_BODY_BOLD);
        button.setForeground(activeTab.equals(tab) ? Theme.PRIMARY : Theme.TEXT_MUTED);
        button.setBorder(BorderFactory.createMatteBorder(0, 0, activeTab.equals(tab) ? 2 : 0, 0,
                activeTab.equals(tab) ? Theme.PRIMARY : Theme.CARD_BG));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            activeTab = tab;
            updateTabs();
            if ("Materials".equals(tab)) applyMaterialFilters();
            else applyAssignmentFilters();
        });
        return button;
    }

    private String usefulMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage() == null ? "Please try again." : cause.getMessage();
    }

    private void openMaterial(int materialId) {
        new SwingWorker<StudentMaterial, Void>() {
            @Override protected StudentMaterial doInBackground() {
                return dataService.getMaterial(materialId);
            }
            @Override protected void done() {
                try {
                    StudentMaterial material = get();
                    if (material == null) {
                        showWarning("This material is not available for your courses.");
                        return;
                    }
                    openMaterialReference(material);
                } catch (Exception ex) {
                    showError("Material unavailable", usefulMessage(ex));
                }
            }
        }.execute();
    }

    private void openMaterialReference(StudentMaterial material) {
        String reference = material.getReference();
        if (material.isLink()) {
            try {
                URI uri = URI.create(reference);
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                    throw new IllegalArgumentException("Only HTTP and HTTPS links can be opened.");
                }
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new UnsupportedOperationException("No browser is available.");
                }
                Desktop.getDesktop().browse(uri);
            } catch (Exception ex) {
                showError("Could not open link", ex.getMessage());
            }
        } else if ("File".equals(material.getType())) {
            downloadMaterial(material);
        } else {
            JTextArea text = new JTextArea(reference.isBlank() ? material.getNotes() : reference, 8, 44);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(text), material.getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void downloadMaterial(StudentMaterial material) {
        String name = Path.of(material.getReference().replace('\\', '/')).getFileName().toString();
        String safeCode = material.getCourseCode().replaceAll("[^A-Za-z0-9_-]", "_");
        Path source = Path.of("data", "materials", safeCode, name);
        JFileChooser chooser = new JFileChooser(Path.of(System.getProperty("user.home"), "Downloads").toFile());
        chooser.setSelectedFile(new java.io.File(name));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destination = chooser.getSelectedFile().toPath();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws IOException {
                if (!Files.isRegularFile(source)) {
                    throw new IOException("The material file was not found in course storage.");
                }
                Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(CourseMaterialsView.this, "Saved to " + destination,
                            "Download complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Download failed", usefulMessage(ex));
                }
            }
        }.execute();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Please try again." : message,
                title, JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Course materials", JOptionPane.WARNING_MESSAGE);
    }

    private void showAssignmentAction(StudentAssignment assignment) {
        String action = actionFor(assignment);
        if ("View".equals(action)) showSubmissionDetails(assignment);
        else showSubmitDialog(assignment);
    }

    private String actionFor(StudentAssignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        boolean closed = isPastCutoff(assignment)
                || (assignment.dueAt() != null && now.isAfter(assignment.dueAt()) && !assignment.allowLate());
        if ("GRADED".equalsIgnoreCase(assignment.status()) || assignment.hasSubmission() && closed) return "View";
        if (assignment.hasSubmission()) return "Resubmit";
        return "Submit";
    }

    private void showSubmitDialog(StudentAssignment assignment) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Submit assignment",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(false);
        JPanel panel = new JPanel(new BorderLayout(0, Theme.CARD_GAP));
        panel.setBackground(Theme.CARD_BG);
        panel.setBorder(Theme.createEmptyPadding(Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        addDetail(details, 0, "Course", assignment.courseCode() + " - " + assignment.courseName());
        addDetail(details, 1, "Assignment", assignment.title());
        addDetail(details, 2, "Opened", formatTime(assignment.openedAt()));
        addDetail(details, 3, "Due date", formatTime(assignment.dueAt()));
        addDetail(details, 4, "Time remaining", timeRemaining(assignment.dueAt()));

        List<Path> selectedFiles = new ArrayList<>();
        Map<Path, JLabel> selectedFileLabels = new HashMap<>();
        AtomicInteger validationGeneration = new AtomicInteger();
        JPanel fileList = new JPanel();
        fileList.setOpaque(false);
        fileList.setLayout(new BoxLayout(fileList, BoxLayout.Y_AXIS));
        JButton submit = new PrimaryButton("Submit");
        submit.setEnabled(false);
        JLabel error = new JLabel(" ");
        error.setFont(Theme.FONT_SMALL);
        error.setForeground(Theme.DANGER);
        JPanel dropZone = new JPanel(new BorderLayout(0, Theme.SPACE_2));
        dropZone.setBackground(Theme.BG_MAIN);
        dropZone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                Theme.createEmptyPadding(Theme.CARD_PADDING, Theme.CARD_PADDING,
                        Theme.CARD_PADDING, Theme.CARD_PADDING)));
        JLabel prompt = new JLabel("Drop assignment files here", SwingConstants.CENTER);
        prompt.setFont(Theme.FONT_BODY);
        prompt.setForeground(Theme.TEXT_MUTED);
        JButton browse = new JButton("Browse files");
        browse.setFont(Theme.FONT_BODY_BOLD);
        browse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        browse.addActionListener(e -> chooseUploadFiles(dialog, assignment, selectedFiles,
                selectedFileLabels, validationGeneration, fileList, error, submit));
        dropZone.add(prompt, BorderLayout.CENTER);
        dropZone.add(browse, BorderLayout.SOUTH);
        dropZone.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override public boolean importData(TransferSupport support) {
                try {
                    @SuppressWarnings("unchecked")
                    List<java.io.File> dropped =
                            (List<java.io.File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    selectedFiles.clear();
                    dropped.stream().map(java.io.File::toPath).forEach(selectedFiles::add);
                    refreshSelectedFiles(selectedFiles, selectedFileLabels, validationGeneration,
                            fileList, error, assignment, submit);
                    return true;
                } catch (Exception ex) {
                    error.setText("Could not read the dropped files.");
                    return false;
                }
            }
        });

        JTextArea comment = new JTextArea(3, 40);
        comment.setFont(Theme.FONT_BODY);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        JScrollPane commentScroll = new JScrollPane(comment);
        commentScroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        JButton cancel = new JButton("Cancel");
        submit.addActionListener(e -> submitFiles(dialog, assignment, selectedFiles, comment.getText(),
                error, progress, submit, cancel));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(details);
        body.add(Box.createVerticalStrut(Theme.CARD_GAP));
        body.add(dropZone);
        body.add(error);
        body.add(fileList);
        body.add(Box.createVerticalStrut(Theme.CARD_GAP));
        body.add(new JLabel("Comment (optional)"));
        body.add(commentScroll);
        body.add(progress);
        panel.add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        buttons.setOpaque(false);
        cancel.addActionListener(e -> dialog.dispose());
        buttons.add(cancel);
        buttons.add(submit);
        panel.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.setMinimumSize(new Dimension(560, 560));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        refreshSelectedFiles(selectedFiles, selectedFileLabels, validationGeneration,
                fileList, error, assignment, submit);
        dialog.setVisible(true);
    }

    private void chooseUploadFiles(JDialog dialog, StudentAssignment assignment, List<Path> selected,
                                  Map<Path, JLabel> labels, AtomicInteger validationGeneration,
                                  JPanel fileList, JLabel error, JButton submit) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Assignment files (" + String.join(", ", UPLOAD_TYPES) + ")",
                UPLOAD_TYPES.toArray(String[]::new)));
        if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            selected.clear();
            for (java.io.File file : chooser.getSelectedFiles()) selected.add(file.toPath());
            refreshSelectedFiles(selected, labels, validationGeneration, fileList, error, assignment, submit);
        }
    }

    private void refreshSelectedFiles(List<Path> selected, Map<Path, JLabel> labels,
                                       AtomicInteger validationGeneration, JPanel fileList, JLabel error,
                                       StudentAssignment assignment, JButton submit) {
        fileList.removeAll();
        labels.clear();
        for (Path path : selected) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel label = new JLabel(path.getFileName() + "  (checking size...)");
            labels.put(path, label);
            label.setFont(Theme.FONT_BODY);
            JButton remove = linkButton("Remove");
            remove.addActionListener(e -> {
                selected.remove(path);
                refreshSelectedFiles(selected, labels, validationGeneration, fileList,
                        error, assignment, submit);
            });
            row.add(label, BorderLayout.CENTER);
            row.add(remove, BorderLayout.EAST);
            fileList.add(row);
        }
        fileList.revalidate();
        fileList.repaint();
        int generation = validationGeneration.incrementAndGet();
        submit.setEnabled(false);
        error.setText(selected.isEmpty() ? "Select at least one file." : "Checking selected files...");
        List<Path> snapshot = List.copyOf(selected);
        new SwingWorker<UploadValidation, Void>() {
            @Override protected UploadValidation doInBackground() {
                Map<Path, Long> sizes = new HashMap<>();
                if (snapshot.isEmpty()) return new UploadValidation(sizes, "Select at least one file.");
                if (snapshot.size() > assignment.maxFiles()) {
                    return new UploadValidation(sizes,
                            "This assignment accepts at most " + assignment.maxFiles() + " files.");
                }
                for (Path path : snapshot) {
                    if (path == null || !Files.isRegularFile(path)) {
                        return new UploadValidation(sizes, "A selected file is no longer available.");
                    }
                    String ext = extension(path.getFileName().toString());
                    if (!UPLOAD_TYPES.contains(ext) || !assignment.allowedTypes().contains(ext)) {
                        return new UploadValidation(sizes, "File type ." + ext + " is not allowed.");
                    }
                    try {
                        long size = Files.size(path);
                        if (size > Math.min(FILE_LIMIT_BYTES,
                                (long) assignment.maxFileMb() * 1024 * 1024)) {
                            return new UploadValidation(sizes, "Each file must be no larger than 20 MB.");
                        }
                        sizes.put(path, size);
                    } catch (IOException ex) {
                        return new UploadValidation(sizes, "Could not read " + path.getFileName() + ".");
                    }
                }
                return new UploadValidation(sizes, " ");
            }

            @Override protected void done() {
                if (generation != validationGeneration.get() || !snapshot.equals(selected)) return;
                try {
                    UploadValidation result = get();
                    result.sizes().forEach((path, size) -> {
                        JLabel label = labels.get(path);
                        if (label != null) label.setText(path.getFileName() + "  (" + size / 1024 + " KB)");
                    });
                    error.setText(result.message());
                    submit.setEnabled(" ".equals(result.message()));
                } catch (Exception ex) {
                    error.setText(usefulMessage(ex));
                    submit.setEnabled(false);
                }
            }
        }.execute();
    }

    private void submitFiles(JDialog dialog, StudentAssignment assignment, List<Path> files, String comment,
                            JLabel error, JProgressBar progress, JButton submit, JButton cancel) {
        boolean wasValid = submit.isEnabled();
        if (!wasValid) return;
        submit.setEnabled(false);
        cancel.setEnabled(false);
        progress.setVisible(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                assignmentService.submit(assignment.id(), List.copyOf(files), comment);
                return null;
            }
            @Override protected void done() {
                progress.setVisible(false);
                try {
                    get();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(CourseMaterialsView.this,
                            "Your work was submitted successfully.", "Submission complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    updateAssignmentAfterSubmit(assignment);
                    loadRows(false);
                } catch (Exception ex) {
                    cancel.setEnabled(true);
                    submit.setEnabled(wasValid);
                    error.setText(usefulMessage(ex));
                }
            }
        }.execute();
    }

    private void showSubmissionDetails(StudentAssignment assignment) {
        new SwingWorker<StudentSubmissionDetails, Void>() {
            @Override protected StudentSubmissionDetails doInBackground() {
                return assignmentService.getSubmission(assignment.id());
            }
            @Override protected void done() {
                try {
                    StudentSubmissionDetails details = get();
                    if (details == null) {
                        showWarning("There is no submission to view.");
                        return;
                    }
                    String files = details.files().stream().map(f -> f.name() + " (" + f.size() / 1024 + " KB)")
                            .collect(Collectors.joining("\n"));
                    String detailText = "Assignment: " + assignment.title()
                            + "\nSubmitted: " + formatTime(details.submittedAt())
                            + "\nFiles:\n" + files
                            + "\nComment: " + details.comment()
                            + "\nStatus: " + details.status()
                            + (details.marks() == null ? "" : "\nMarks: " + details.marks())
                            + (details.feedback().isBlank() ? "" : "\nFeedback: " + details.feedback());
                    JTextArea text = new JTextArea(detailText, 14, 52);
                    text.setEditable(false);
                    text.setLineWrap(true);
                    text.setWrapStyleWord(true);
                    JOptionPane.showMessageDialog(CourseMaterialsView.this, new JScrollPane(text),
                            "Submission details", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Submission unavailable", usefulMessage(ex));
                }
            }
        }.execute();
    }

    private void addDetail(JPanel panel, int row, String name, String value) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.gridy = row;
        label.anchor = GridBagConstraints.WEST;
        label.insets = new Insets(Theme.SPACE_1, 0, Theme.SPACE_1, Theme.CARD_GAP);
        JLabel labelComponent = new JLabel(name);
        labelComponent.setFont(Theme.FONT_SMALL);
        labelComponent.setForeground(Theme.TEXT_MUTED);
        panel.add(labelComponent, label);
        GridBagConstraints val = new GridBagConstraints();
        val.gridx = 1;
        val.gridy = row;
        val.weightx = 1;
        val.fill = GridBagConstraints.HORIZONTAL;
        val.anchor = GridBagConstraints.WEST;
        val.insets = new Insets(Theme.SPACE_1, 0, Theme.SPACE_1, 0);
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(Theme.FONT_BODY);
        valueComponent.setForeground(Theme.TEXT_MAIN);
        panel.add(valueComponent, val);
    }

    private String timeRemaining(LocalDateTime due) {
        if (due == null) return "Not provided";
        long minutes = java.time.Duration.between(LocalDateTime.now(), due).toMinutes();
        if (minutes < 0) return "Deadline passed";
        long days = minutes / (60 * 24);
        long hours = (minutes % (60 * 24)) / 60;
        return days > 0 ? days + " days, " + hours + " hours" : hours + " hours remaining";
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "Not provided" : value.format(DATE_TIME);
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void updateAssignmentPage() {
        if (assignmentTable.getModel().getRowCount() == 0) assignmentModel.fireTableDataChanged();
        refreshPage(false);
    }

    private final class MaterialModel extends AbstractTableModel {
        private final String[] columns = {"Title", "Course", "Type", "Notes", "Lecturer", "Action"};
        @Override public int getRowCount() { return materials.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public boolean isCellEditable(int row, int column) { return column == 5; }
        @Override public Object getValueAt(int row, int column) {
            StudentMaterial material = materials.get(row);
            return switch (column) {
                case 0 -> material.getTitle();
                case 1 -> material.getCourseCode();
                case 2 -> material.getType();
                case 3 -> material.getNotes();
                case 4 -> material.getLecturer();
                case 5 -> material.getType().equalsIgnoreCase("File") ? "Download"
                        : material.isLink() ? "Open link" : "Open";
                default -> "";
            };
        }
    }

    private final class AssignmentModel extends AbstractTableModel {
        private final String[] columns = {"Title", "Course", "Due date", "Status",
                "Submitted on", "Grade / feedback", "Action"};
        @Override public int getRowCount() { return assignments.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public boolean isCellEditable(int row, int column) { return column == 6; }
        @Override public Object getValueAt(int row, int column) {
            StudentAssignment assignment = assignments.get(row);
            return switch (column) {
                case 0 -> assignment.title();
                case 1 -> assignment.courseCode();
                case 2 -> assignment.dueAt();
                case 3 -> statusFor(assignment);
                case 4 -> assignment.submittedAt();
                case 5 -> assignment.marks() == null ? assignment.feedback()
                        : assignment.marks() + (assignment.maxMarks() == null ? "" : " / " + assignment.maxMarks());
                case 6 -> actionFor(assignment);
                default -> "";
            };
        }
    }

    private String statusFor(StudentAssignment assignment) {
        if ("GRADED".equalsIgnoreCase(assignment.status())) return "Graded";
        if ("LATE".equalsIgnoreCase(assignment.status())) return "Late";
        if (assignment.hasSubmission()) return "Submitted";
        if (isPastCutoff(assignment)) return "Overdue";
        return "Not submitted";
    }

    private record RowData(List<StudentMaterial> materials, List<StudentAssignment> assignments,
                           List<String> courseOptions, RuntimeException assignmentError) {}
    private record ContentItem(StudentMaterial material, StudentAssignment assignment, Integer weekNo) {
        static ContentItem forMaterial(StudentMaterial material, Integer weekNo) {
            return new ContentItem(material, null, weekNo);
        }
        static ContentItem forAssignment(StudentAssignment assignment, Integer weekNo) {
            return new ContentItem(null, assignment, weekNo);
        }
        String courseCode() {
            return material == null ? assignment.courseCode() : material.getCourseCode();
        }
        int sortOrder() {
            if (material == null) return 3;
            if (material.isLink()) return 1;
            if ("File".equals(material.getType())) return 0;
            return 2;
        }
        LocalDateTime sortDate() {
            return material == null ? assignment.openedAt() : material.getUploadedAt();
        }
    }

    private static final class WeekContentPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(800, 480);
        }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private record UploadValidation(Map<Path, Long> sizes, String message) {}

    private class TextRenderer extends BaseCellRenderer {
        TextRenderer(JTable table) { super(table, false); }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String full = value instanceof LocalDateTime date ? formatTime(date)
                    : value == null ? "" : value.toString();
            setToolTipText((column == 0 || column == 3 || table == assignmentTable && column == 5)
                    ? full : null);
            setText(ellipsize(full, 72));
            return this;
        }
    }

    private class BaseCellRenderer extends DefaultTableCellRenderer {
        private final JTable table;
        private final boolean badge;
        BaseCellRenderer(JTable table, boolean badge) { this.table = table; this.badge = badge; }

        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(t, value, selected, false, row, column);
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(Theme.FONT_BODY);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.SPACE_4, 0, Theme.SPACE_2)));
            setBackground(cellBackground(table, row, selected));
            setForeground(Theme.TEXT_MAIN);
            return this;
        }
    }

    private Color cellBackground(JTable table, int row, boolean selected) {
        if (selected) return Theme.PRIMARY_LIGHT;
        int hovered = table == materialTable ? materialHoverRow : assignmentHoverRow;
        return row == hovered ? Theme.TABLE_HOVER : Theme.CARD_BG;
    }

    private class BadgeRenderer implements TableCellRenderer {
        private final JTable table;
        private final boolean materialBadge;
        BadgeRenderer(JTable table, boolean materialBadge) {
            this.table = table;
            this.materialBadge = materialBadge;
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            String text = value == null ? "" : value.toString();
            Color background = cellBackground(table, row, selected);
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrapper.setOpaque(true);
            wrapper.setBackground(background);
            wrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.SPACE_4, 0, Theme.SPACE_2)));
            JLabel pill = new JLabel(text);
            pill.setFont(new Font(Theme.FONT_BODY.getName(), Font.PLAIN, 11));
            Color[] color = badgeColors(text, materialBadge);
            pill.setOpaque(true);
            pill.setBackground(color[0]);
            pill.setForeground(color[1]);
            pill.setBorder(Theme.createEmptyPadding(4, Theme.SPACE_2, 4, Theme.SPACE_2));
            pill.setPreferredSize(new Dimension(pill.getPreferredSize().width, 24));
            wrapper.add(pill);
            return wrapper;
        }
    }

    private Color[] badgeColors(String value, boolean materialBadge) {
        String state = value.toLowerCase(Locale.ROOT);
        if ("not submitted".equals(state)) return new Color[]{Theme.BADGE_GRAY_BG, Theme.BADGE_GRAY_TEXT};
        if (state.contains("due soon") || state.contains("late") || state.contains("link")) {
            return new Color[]{Theme.BADGE_AMBER_BG, Theme.BADGE_AMBER_TEXT};
        }
        if (state.contains("graded")) return new Color[]{Theme.SUCCESS_BG, Theme.SUCCESS};
        if (state.contains("overdue")) return new Color[]{Theme.DANGER_BG, Theme.DANGER};
        if (state.contains("submitted") || state.contains("file")) return new Color[]{Theme.BADGE_BLUE_BG, Theme.BADGE_BLUE_TEXT};
        if (materialBadge) return new Color[]{Theme.BADGE_GRAY_BG, Theme.BADGE_GRAY_TEXT};
        return new Color[]{Theme.BADGE_GRAY_BG, Theme.BADGE_GRAY_TEXT};
    }

    private class ActionRenderer implements TableCellRenderer {
        private final JTable table;
        private final boolean materialAction;
        ActionRenderer(JTable table, boolean materialAction) {
            this.table = table;
            this.materialAction = materialAction;
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            JPanel cell = actionCell(table, row, selected);
            JButton button = table == assignmentTable
                    ? assignmentActionButton(String.valueOf(value)) : ghostButton(String.valueOf(value), false);
            cell.add(button, BorderLayout.CENTER);
            return cell;
        }
    }

    private class ActionEditor extends DefaultCellEditor {
        private final boolean materialAction;
        private int modelRow;

        ActionEditor(boolean materialAction) {
            super(new JCheckBox());
            this.materialAction = materialAction;
            setClickCountToStart(1);
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected,
                int row, int column) {
            modelRow = table.convertRowIndexToModel(row);
            JButton button = materialAction ? ghostButton(String.valueOf(value), true)
                    : assignmentActionButton(String.valueOf(value));
            button.addActionListener(e -> {
                fireEditingStopped();
                if (materialAction) openMaterial(materials.get(modelRow).getId());
                else showAssignmentAction(assignments.get(modelRow));
            });
            return actionCell(table, row, selected, button);
        }
    }

    private JButton assignmentActionButton(String text) {
        JButton button;
        if ("Submit".equals(text)) {
            button = new PrimaryButton(text);
            button.setPreferredSize(new Dimension(84, 32));
        } else if ("Resubmit".equals(text)) {
            button = new JButton(text);
            button.setFont(Theme.FONT_BODY_BOLD);
            button.setForeground(Theme.PRIMARY);
            button.setBackground(Theme.CARD_BG);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.SPACE_2, 0, Theme.SPACE_2)));
            button.setPreferredSize(new Dimension(92, 32));
        } else {
            button = ghostButton(text, true);
        }
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel actionCell(JTable table, int row, boolean selected) {
        return actionCell(table, row, selected, null);
    }

    private JPanel actionCell(JTable table, int row, boolean selected, JButton button) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(true);
        cell.setBackground(cellBackground(table, row, selected));
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(0, Theme.SPACE_4, 0, Theme.SPACE_2)));
        if (button != null) cell.add(button, BorderLayout.CENTER);
        return cell;
    }

    private JButton ghostButton(String text, boolean underline) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BODY);
        button.setForeground(Theme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIcon(IconHelper.getIcon("downloads", 16, Theme.PRIMARY));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                button.setText("<html><u>" + button.getText() + "</u></html>");
            }
            @Override public void mouseExited(MouseEvent e) {
                button.setText(button.getText().replaceAll("</?html>|</?u>", ""));
            }
        });
        return button;
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        private final JTable table;
        private final boolean material;
        private TableRowSorter<?> sorter;
        HeaderRenderer(JTable table, boolean material) {
            this.table = table;
            this.material = material;
            setOpaque(true);
        }
        void setSorter(TableRowSorter<?> value) { sorter = value; }

        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(t, value, false, false, row, column);
            setFont(Theme.FONT_SMALL);
            setForeground(Theme.TEXT_MUTED);
            setBackground(Theme.CARD_BG);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.SPACE_4, 0, Theme.SPACE_2)));
            String label = String.valueOf(value);
            if (sorter != null) {
                List<? extends javax.swing.RowSorter.SortKey> keys = sorter.getSortKeys();
                if (!keys.isEmpty() && keys.get(0).getColumn() == column) {
                    label += keys.get(0).getSortOrder() == javax.swing.SortOrder.ASCENDING ? "  \u2191" : "  \u2193";
                }
            }
            setText(label);
            return this;
        }
    }

    private String ellipsize(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 1)) + "\u2026";
    }
}
