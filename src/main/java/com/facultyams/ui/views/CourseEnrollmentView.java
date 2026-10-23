package com.facultyams.ui.views;

import com.facultyams.model.Course;
import com.facultyams.service.DataService;
import com.facultyams.service.EnrollmentResult;
import com.facultyams.service.EnrollmentService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;

/** Course enrollment page with shared aligned course tables and live validation. */
public class CourseEnrollmentView extends JPanel {
    private final EnrollmentService enrollmentService =
            new EnrollmentService(DataService.getInstance());
    private final PageHeaderCard headerCard = new PageHeaderCard();
    private final EnrolledTableModel enrolledModel = new EnrolledTableModel();
    private final ElectivesTableModel electivesModel = new ElectivesTableModel();
    private final JPanel enrolledBody = new JPanel(new BorderLayout());
    private final JPanel electivesBody = new JPanel(new BorderLayout());
    private final JLabel enrolledCount = countBadge();
    private final JLabel electivesCount = countBadge();
    private final StyledTextField searchField = new StyledTextField("Search code, course or lecturer");
    private final StyledComboBox departmentCombo;
    private final Timer searchTimer;
    private final JPanel toastLayer = new JPanel(new GridBagLayout());
    private final ToastManager toastManager = new ToastManager(toastLayer);
    private Runnable backAction = () -> {};
    private CourseTable enrolledTable;
    private CourseTable electivesTable;
    private String searchText = "";
    private String selectedDepartment = "All departments";

    public CourseEnrollmentView() {
        this(() -> {});
    }

    public CourseEnrollmentView(Runnable backAction) {
        this.backAction = backAction == null ? () -> {} : backAction;
        List<String> departments = new ArrayList<>(new TreeSet<>(
                DataService.getInstance().getAllCourses().stream()
                        .map(Course::getDepartment).filter(value -> value != null && !value.isBlank()).toList()));
        departments.add(0, "All departments");
        departmentCombo = new StyledComboBox(departments.toArray(String[]::new));
        searchTimer = new Timer(200, e -> refreshAvailable());
        searchTimer.setRepeats(false);

        setOpaque(false);
        setLayout(new BorderLayout());
        EnrollmentContentPanel content = new EnrollmentContentPanel();

        content.addRow(createBackLink());
        content.addGap(Theme.ENROLLMENT_SECTION_GAP);
        content.addRow(headerCard);
        content.addGap(Theme.ENROLLMENT_SECTION_GAP);
        content.addRow(createEnrolledSection());
        content.addGap(Theme.ENROLLMENT_SECTION_GAP);
        content.addRow(createElectivesSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new EnrollmentScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        toastLayer.setOpaque(false);
        add(toastLayer, BorderLayout.SOUTH);

        enrollmentService.addEnrollmentListener(this::refreshData);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { updateSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { updateSearch(); }
        });
        departmentCombo.addActionListener(e -> {
            selectedDepartment = (String) departmentCombo.getSelectedItem();
            refreshAvailable();
        });
        refreshData();
    }

    public void setBackAction(Runnable action) {
        backAction = action == null ? () -> {} : action;
    }

    private JComponent createBackLink() {
        JPanel link = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        link.setOpaque(false);
        JComponent icon = new JComponent() {
            { setPreferredSize(new Dimension(16, 20)); }
            @Override protected void paintComponent(Graphics graphics) {
                int size = 16;
                EnrollmentIcons.paint((Graphics2D) graphics, EnrollmentIcons.Type.CHEVRON_LEFT,
                        0, (getHeight() - size) / 2, size, Theme.PRIMARY);
            }
        };
        JLabel text = new JLabel("Back to Course Overview");
        text.setFont(Theme.ENROLLMENT_BODY_BOLD);
        text.setForeground(Theme.PRIMARY);
        link.add(icon);
        link.add(text);
        link.setAlignmentX(Component.LEFT_ALIGNMENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { backAction.run(); }
            @Override public void mouseEntered(MouseEvent e) {
                text.setText("<html><u>Back to Course Overview</u></html>");
            }
            @Override public void mouseExited(MouseEvent e) {
                text.setText("Back to Course Overview");
            }
        };
        link.addMouseListener(click);
        text.addMouseListener(click);
        icon.addMouseListener(click);
        return link;
    }

    private JPanel createEnrolledSection() {
        JPanel card = sectionCard();
        JPanel title = sectionTitle("Enrolled Courses", enrolledCount);
        card.add(title, BorderLayout.NORTH);
        enrolledBody.setOpaque(false);
        card.add(enrolledBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel createElectivesSection() {
        JPanel card = sectionCard();
        JPanel title = sectionTitle("Available Electives & Modules", electivesCount);
        JPanel filter = createElectivesToolbar();
        JPanel header = new JPanel(new BorderLayout(Theme.ENROLLMENT_SECTION_GAP, 0));
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(filter, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        electivesBody.setOpaque(false);
        card.add(electivesBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel sectionCard() {
        RoundedPanel card = new RoundedPanel(Theme.ENROLLMENT_CARD_RADIUS);
        card.setBorderColor(Theme.ENROLLMENT_BORDER);
        card.setLayout(new BorderLayout(0, Theme.ENROLLMENT_SECTION_GAP));
        card.setBorder(Theme.createEmptyPadding(Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING, Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JPanel sectionTitle(String title, JLabel count) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(Theme.ENROLLMENT_SECTION_TITLE);
        label.setForeground(Theme.ENROLLMENT_TEXT);
        row.add(label);
        row.add(count);
        return row;
    }

    private JLabel countBadge() {
        return new JLabel("0", SwingConstants.CENTER) {
            {
                setFont(Theme.ENROLLMENT_SMALL_BOLD);
                setForeground(Theme.PRIMARY);
                setOpaque(false);
                setBorder(Theme.createEmptyPadding(3, 8, 3, 8));
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.PRIMARY_LIGHT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
    }

    private JPanel createElectivesToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchBox());
        toolbar.add(departmentCombo);
        return toolbar;
    }

    private JPanel searchBox() {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            private boolean focused;
            {
                setOpaque(true);
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                setPreferredSize(new Dimension(320, Theme.ENROLLMENT_CONTROL_HEIGHT));
                searchField.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(focused ? Theme.PRIMARY : Theme.ENROLLMENT_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                g.dispose();
            }
        };
        JComponent searchIcon = new JComponent() {
            { setPreferredSize(new Dimension(20, 20)); }
            @Override protected void paintComponent(Graphics graphics) {
                EnrollmentIcons.paint((Graphics2D) graphics, EnrollmentIcons.Type.SEARCH,
                        1, 1, 18, Theme.ENROLLMENT_MUTED);
            }
        };
        box.add(searchIcon, BorderLayout.WEST);
        box.add(searchField, BorderLayout.CENTER);
        JButton clear = new JButton();
        clear.setPreferredSize(new Dimension(24, 28));
        clear.setBorder(BorderFactory.createEmptyBorder());
        clear.setContentAreaFilled(false);
        clear.setFocusPainted(false);
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.setVisible(false);
        clear.addActionListener(e -> searchField.setText(""));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { clear.setVisible(!searchField.getText().isEmpty()); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
        clear.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics graphics, JComponent component) {
                EnrollmentIcons.paint((Graphics2D) graphics, EnrollmentIcons.Type.CLOSE,
                        5, 7, 14, Theme.ENROLLMENT_MUTED);
            }
        });
        box.add(clear, BorderLayout.EAST);
        return box;
    }

    private void updateSearch() {
        searchText = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        searchTimer.restart();
    }

    public void refreshData() {
        List<Course> enrolled = enrollmentService.getEnrolled();
        enrolledCount.setText(Integer.toString(enrolled.size()));
        headerCard.updateCredits(enrollmentService.getTotalCredits());
        if (enrolled.isEmpty()) {
            showEmpty(enrolledBody, "No enrolled courses yet",
                    "Enroll in an available elective to get started.");
        } else {
            enrolledModel.setCourses(enrolled);
            enrolledTable = new CourseTable(enrolledModel, enrollmentService, true,
                    this::requestDrop);
            enrolledTable.addPropertyChangeListener("enrollmentRejected", e ->
                    toastManager.show(String.valueOf(e.getNewValue()), false));
            enrolledBody.removeAll();
            enrolledBody.add(tableWithHeader(enrolledTable), BorderLayout.CENTER);
            enrolledBody.revalidate();
            enrolledBody.repaint();
        }
        refreshAvailable();
    }

    private void refreshAvailable() {
        if (electivesBody == null) return;
        List<Course> available = enrollmentService.getAvailable().stream()
                .filter(course -> selectedDepartment == null
                        || "All departments".equals(selectedDepartment)
                        || selectedDepartment.equals(course.getDepartment()))
                .filter(course -> matchesSearch(course, searchText))
                .sorted(Comparator.comparing(Course::getCode))
                .toList();
        electivesCount.setText(Integer.toString(available.size()));
        if (available.isEmpty()) {
            showEmpty(electivesBody, "No electives match your search",
                    "Try another course, lecturer, or department.");
        } else {
            electivesModel.setCourses(available);
            electivesTable = new CourseTable(electivesModel, enrollmentService, false,
                    course -> showResult(enrollmentService.enroll(course)));
            electivesTable.addPropertyChangeListener("enrollmentRejected", e ->
                    toastManager.show(String.valueOf(e.getNewValue()), false));
            electivesBody.removeAll();
            electivesBody.add(tableWithHeader(electivesTable), BorderLayout.CENTER);
            electivesBody.revalidate();
            electivesBody.repaint();
        }
    }

    private boolean matchesSearch(Course course, String query) {
        if (query == null || query.isBlank()) return true;
        return contains(course.getCode(), query) || contains(course.getTitle(), query)
                || contains(course.getLecturerName(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private JPanel tableWithHeader(CourseTable table) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        table.setFillsViewportHeight(false);
        wrapper.add(table.getTableHeader(), BorderLayout.NORTH);
        wrapper.add(table, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(0,
                Theme.ENROLLMENT_HEADER_HEIGHT + table.getRowCount() * Theme.ENROLLMENT_ROW_HEIGHT));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height));
        return wrapper;
    }

    private void showEmpty(JPanel body, String title, String subtitle) {
        body.removeAll();
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.ENROLLMENT_BODY_BOLD);
        heading.setForeground(Theme.ENROLLMENT_TEXT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel(subtitle);
        hint.setFont(Theme.ENROLLMENT_SMALL);
        hint.setForeground(Theme.ENROLLMENT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(heading);
        empty.add(Box.createVerticalStrut(8));
        empty.add(hint);
        JPanel centered = new JPanel(new GridBagLayout());
        centered.setOpaque(false);
        centered.setPreferredSize(new Dimension(400, 88));
        centered.add(empty);
        body.add(centered, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    private void requestDrop(Course course) {
        if (!ConfirmDialog.confirmDrop(this, course)) return;
        showResult(enrollmentService.drop(course));
    }

    private void showResult(EnrollmentResult result) {
        toastManager.show(result.message(), result.success());
    }

    private static final class EnrollmentContentPanel extends JPanel implements Scrollable {
        private int nextRow;

        EnrollmentContentPanel() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setBorder(Theme.createEmptyPadding(0, 0, Theme.ENROLLMENT_PAGE_PADDING, 0));
        }

        void addRow(Component component) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = nextRow++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            add(component, c);
        }

        void addGap(int height) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = nextRow++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            add(Box.createRigidArea(new Dimension(0, height)), c);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
