package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import com.srms.model.ViewMode;
import com.srms.service.CourseFilter;
import com.srms.ui.components.CourseCard;
import com.srms.ui.components.CourseListRow;
import com.srms.ui.components.CourseToolbar;
import com.srms.ui.components.Icons;
import com.srms.ui.components.PrimaryButton;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** Course overview with responsive cards and observer-based filters. */
public class CourseOverviewView extends JPanel {
    private final Consumer<Course> onCourseSelected;
    private final Runnable onManageEnrollment;
    private final List<Course> allCourses = new ArrayList<>();
    private final CourseToolbar toolbar = new CourseToolbar();
    private final GridContainerPanel gridContainer = new GridContainerPanel();
    private final JLabel resultCount = new JLabel();
    private final JScrollPane scrollPane = new JScrollPane(gridContainer);
    private List<Course> visibleCourses = List.of();
    private int currentColumns = -1;

    public CourseOverviewView(Consumer<Course> onCourseSelected, Runnable onManageEnrollment) {
        this.onCourseSelected = onCourseSelected;
        this.onManageEnrollment = onManageEnrollment;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.COURSE_PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE_PADDING, Theme.PAGE_PADDING,
                Theme.PAGE_PADDING, Theme.PAGE_PADDING));
        buildHeader();
        buildCourseList();
        toolbar.addFilterListener(this::applyFilters);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateGridColumns(); }
        });
    }

    private void buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JPanel titleRow = new JPanel(new BorderLayout(16, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Course overview");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COURSE_TEXT_MAIN);
        titleRow.add(title, BorderLayout.WEST);
        if (onManageEnrollment != null) {
            JButton manage = new PrimaryButton("Manage Enrollment");
            manage.addActionListener(e -> onManageEnrollment.run());
            titleRow.add(manage, BorderLayout.EAST);
        }
        addHeaderItem(header, titleRow, 0);
        addHeaderGap(header, 1, Theme.SECTION_GAP);

        Ui.Card toolbarCard = new Ui.Card(16, Color.WHITE, Theme.CARD_BORDER);
        toolbarCard.setLayout(new BorderLayout());
        toolbarCard.add(toolbar, BorderLayout.CENTER);
        addHeaderItem(header, toolbarCard, 2);
        addHeaderGap(header, 3, Theme.CARD_GAP / 2);
        resultCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultCount.setForeground(Theme.COURSE_TEXT_MUTED);
        resultCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCount.setHorizontalAlignment(SwingConstants.LEFT);
        resultCount.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        addHeaderItem(header, resultCount, 4);
        addHeaderGap(header, 5, Theme.CARD_GAP / 2);
        add(header, BorderLayout.NORTH);
    }

    private void addHeaderItem(JPanel header, Component component, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        header.add(component, c);
    }

    private void addHeaderGap(JPanel header, int row, int height) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        header.add(Box.createRigidArea(new Dimension(0, height)), c);
    }

    private void buildCourseList() {
        gridContainer.setOpaque(false);
        gridContainer.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getViewport().setOpaque(false);
        Ui.applyThinScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setCourses(List<Course> courses) {
        allCourses.clear();
        if (courses != null) allCourses.addAll(courses);
        applyFilters(toolbar.getFilterState());
    }

    private void applyFilters(CourseToolbar.FilterState state) {
        String semester = currentSemester(allCourses);
        CourseFilter filter = new CourseFilter(
                state.searchText(),
                state.status(),
                state.sortOption(),
                semester
        );
        visibleCourses = filter.apply(allCourses);
        resultCount.setText("Showing " + visibleCourses.size() + " of " + allCourses.size() + " courses");
        currentColumns = state.viewMode() == ViewMode.GRID ? columnsForWidth(viewportWidth()) : 1;
        renderCards(state.viewMode());
    }

    private void renderCards(ViewMode viewMode) {
        gridContainer.removeAll();
        gridContainer.setPreferredSize(null);
        gridContainer.setMinimumSize(new Dimension(0, 0));
        if (visibleCourses.isEmpty()) {
            gridContainer.setLayout(new GridBagLayout());
            JPanel emptyState = new JPanel();
            emptyState.setOpaque(false);
            emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));
            JComponent icon = new EmptyStateIcon();
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel heading = new JLabel("No courses found");
            heading.setFont(Theme.FONT_SECTION);
            heading.setForeground(Theme.COURSE_TEXT_MAIN);
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel hint = new JLabel("Try changing your search or filters.");
            hint.setFont(Theme.COURSE_BODY);
            hint.setForeground(Theme.COURSE_TEXT_MUTED);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton clear = new JButton("Clear filters");
            clear.setFont(Theme.COURSE_BODY_BOLD);
            clear.setForeground(Theme.PRIMARY);
            clear.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
            clear.setContentAreaFilled(false);
            clear.setFocusPainted(false);
            clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clear.setAlignmentX(Component.CENTER_ALIGNMENT);
            clear.addActionListener(e -> toolbar.clearFilters());
            emptyState.add(icon);
            emptyState.add(Box.createVerticalStrut(Theme.CONTROL_GAP));
            emptyState.add(heading);
            emptyState.add(Box.createVerticalStrut(Theme.CONTROL_GAP / 2));
            emptyState.add(hint);
            emptyState.add(clear);
            gridContainer.setPreferredSize(new Dimension(Math.max(0, viewportWidth()),
                    Math.max(260, scrollPane.getViewport().getHeight())));
            GridBagConstraints empty = new GridBagConstraints();
            empty.gridx = 0;
            empty.gridy = 0;
            empty.weightx = 1;
            empty.weighty = 1;
            empty.anchor = GridBagConstraints.CENTER;
            gridContainer.add(emptyState, empty);
            gridContainer.setMinimumSize(new Dimension(0, Math.max(260, scrollPane.getViewport().getHeight())));
        } else if (viewMode == ViewMode.LIST) {
            gridContainer.setLayout(new GridBagLayout());
            for (int i = 0; i < visibleCourses.size(); i++) {
                CourseListRow row = new CourseListRow(visibleCourses.get(i), onCourseSelected);
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = i;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(0, 0, Theme.CARD_GAP, 0);
                gridContainer.add(row, c);
            }
            GridBagConstraints remainder = new GridBagConstraints();
            remainder.gridx = 0;
            remainder.gridy = visibleCourses.size();
            remainder.weighty = 1;
            gridContainer.add(Box.createGlue(), remainder);
        } else {
            int columns = Math.max(1, currentColumns);
            gridContainer.setLayout(new GridLayout(0, columns, Theme.CARD_GAP, Theme.CARD_GAP));
            for (int i = 0; i < visibleCourses.size(); i++) {
                CourseCard card = new CourseCard(visibleCourses.get(i), onCourseSelected, onManageEnrollment);
                gridContainer.add(card);
            }
            int remainder = visibleCourses.size() % columns;
            if (remainder != 0) {
                for (int i = remainder; i < columns; i++) {
                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    gridContainer.add(spacer);
                }
            }
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void updateGridColumns() {
        if (toolbar.getFilterState().viewMode() != ViewMode.GRID) return;
        int columns = columnsForWidth(viewportWidth());
        if (columns != currentColumns) {
            currentColumns = columns;
            renderCards(ViewMode.GRID);
        }
    }

    private int viewportWidth() {
        int width = scrollPane.getViewport().getWidth();
        return width > 0 ? width : getWidth();
    }

    private int columnsForWidth(int width) {
        if (width >= 960) return 3;
        if (width >= 620) return 2;
        return 1;
    }

    private String currentSemester(List<Course> courses) {
        for (Course course : courses) {
            if (course.progressPercent() > 0 && course.progressPercent() < 100) return course.semester();
        }
        return courses.isEmpty() ? "" : courses.get(0).semester();
    }

    private final class GridContainerPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(800, 480); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class EmptyStateIcon extends JComponent {
        EmptyStateIcon() { setPreferredSize(new Dimension(40, 40)); }
        @Override protected void paintComponent(Graphics graphics) {
            Icons.paint((Graphics2D) graphics, Icons.Type.SEARCH, 4, 4, 32, Theme.TEXT_SUBTLE);
        }
    }
}
