package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import com.srms.model.CourseStatus;
import com.srms.model.SortOption;
import com.srms.model.ViewMode;
import com.srms.service.CourseFilter;
import com.srms.ui.components.CourseCard;
import com.srms.ui.components.CourseListRow;
import com.srms.ui.components.CourseToolbar;
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
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 20, 16));
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
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout(16, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Course overview");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        titleRow.add(title, BorderLayout.WEST);
        if (onManageEnrollment != null) {
            JButton manage = new JButton("Manage Enrollment");
            manage.setFont(Theme.FONT_BODY_BOLD);
            manage.setForeground(Color.WHITE);
            manage.setBackground(Theme.PRIMARY);
            manage.setFocusPainted(false);
            manage.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            manage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            manage.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { manage.setBackground(Theme.PRIMARY_DARK); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { manage.setBackground(Theme.PRIMARY); }
            });
            manage.addActionListener(e -> onManageEnrollment.run());
            titleRow.add(manage, BorderLayout.EAST);
        }
        header.add(titleRow);
        header.add(Box.createVerticalStrut(16));

        Ui.Card toolbarCard = new Ui.Card(16, Color.WHITE, Theme.CARD_BORDER);
        toolbarCard.setLayout(new BorderLayout());
        toolbarCard.add(toolbar, BorderLayout.CENTER);
        header.add(toolbarCard);
        header.add(Box.createVerticalStrut(8));
        resultCount.setFont(Theme.FONT_SMALL);
        resultCount.setForeground(Theme.TEXT_MUTED);
        resultCount.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        header.add(resultCount);
        add(header, BorderLayout.NORTH);
    }

    private void buildCourseList() {
        gridContainer.setOpaque(false);
        gridContainer.setBorder(BorderFactory.createEmptyBorder(4, 4, 12, 4));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
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
        if (visibleCourses.isEmpty()) {
            gridContainer.setLayout(new GridBagLayout());
            JPanel emptyState = new JPanel();
            emptyState.setOpaque(false);
            emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));
            JLabel icon = new JLabel("⌕", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI", Font.PLAIN, 36));
            icon.setForeground(Theme.TEXT_SUBTLE);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel heading = new JLabel("No courses found");
            heading.setFont(Theme.FONT_SECTION);
            heading.setForeground(Theme.TEXT_MAIN);
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel hint = new JLabel("Try changing your search or filters.");
            hint.setFont(Theme.FONT_BODY);
            hint.setForeground(Theme.TEXT_MUTED);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton clear = new JButton("Clear filters");
            clear.setFont(Theme.FONT_BODY_BOLD);
            clear.setForeground(Theme.PRIMARY);
            clear.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
            clear.setContentAreaFilled(false);
            clear.setFocusPainted(false);
            clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clear.setAlignmentX(Component.CENTER_ALIGNMENT);
            clear.addActionListener(e -> toolbar.clearFilters());
            emptyState.add(icon);
            emptyState.add(Box.createVerticalStrut(8));
            emptyState.add(heading);
            emptyState.add(Box.createVerticalStrut(4));
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
                c.insets = new Insets(0, 8, 12, 8);
                gridContainer.add(row, c);
            }
            GridBagConstraints remainder = new GridBagConstraints();
            remainder.gridx = 0;
            remainder.gridy = visibleCourses.size();
            remainder.weighty = 1;
            gridContainer.add(Box.createGlue(), remainder);
        } else {
            int columns = Math.max(1, currentColumns);
            gridContainer.setLayout(new GridBagLayout());
            for (int i = 0; i < visibleCourses.size(); i++) {
                CourseCard card = new CourseCard(visibleCourses.get(i), onCourseSelected, onManageEnrollment);
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % columns;
                c.gridy = i / columns;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(4, 8, 12, 8);
                gridContainer.add(card, c);
            }
            for (int col = 0; col < columns; col++) {
                GridBagConstraints remainder = new GridBagConstraints();
                remainder.gridx = col;
                remainder.gridy = (visibleCourses.size() + columns - 1) / columns;
                remainder.weighty = 1;
                gridContainer.add(Box.createGlue(), remainder);
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
}
