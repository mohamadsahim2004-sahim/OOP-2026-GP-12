package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import com.srms.ui.components.CourseCard;
import com.srms.ui.components.ResponsiveGridLayout;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Screen A: Course Overview card grid with live search, filters, sorting,
 * and access to the legacy enrollment manager.
 */
public class CourseOverviewView extends JPanel {
    private final Consumer<Course> onCourseSelected;
    private final Runnable onManageEnrollment;

    private final List<Course> allCourses = new ArrayList<>();
    private final JPanel gridContainer = new JPanel();
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[]{ "In progress", "Future", "Past", "All" });
    private final JTextField searchField = new JTextField(12);
    private final JComboBox<String> sortCombo = new JComboBox<>(new String[]{ "Sort by short name", "Sort by title", "Sort by last accessed" });
    private final JComboBox<String> viewToggle = new JComboBox<>(new String[]{ "Card", "List" });

    private boolean isListView = false;

    public CourseOverviewView(Consumer<Course> onCourseSelected, Runnable onManageEnrollment) {
        this.onCourseSelected = onCourseSelected;
        this.onManageEnrollment = onManageEnrollment;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 4, 16, 4));

        initHeader();
        initGrid();
        initListeners();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // Top Row: Title + "Manage Enrollment" action button
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel titleLbl = new JLabel("Course overview");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_MAIN);
        titleRow.add(titleLbl, BorderLayout.WEST);

        if (onManageEnrollment != null) {
            JButton manageBtn = new JButton("Manage Enrollment");
            manageBtn.setFont(Theme.FONT_BODY_BOLD);
            manageBtn.setForeground(Theme.PRIMARY);
            manageBtn.setBackground(Theme.PRIMARY_LIGHT);
            manageBtn.setFocusPainted(false);
            manageBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.PRIMARY_BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            manageBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            manageBtn.addActionListener(e -> onManageEnrollment.run());
            titleRow.add(manageBtn, BorderLayout.EAST);
        }
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(14));

        // Toolbar Row
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        styleComboBox(filterCombo);
        styleComboBox(sortCombo);
        styleComboBox(viewToggle);

        searchField.setFont(Theme.FONT_BODY);
        searchField.setPreferredSize(new Dimension(180, 36));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        toolbar.add(filterCombo);
        toolbar.add(searchField);
        toolbar.add(sortCombo);
        toolbar.add(viewToggle);

        headerPanel.add(toolbar);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setFont(Theme.FONT_BODY);
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width + 12, 36));
        cb.setFocusable(false);
    }

    private void initGrid() {
        gridContainer.setOpaque(false);
        gridContainer.setLayout(new ResponsiveGridLayout(20, 20, 275));

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(gridContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollWrapper);
        Ui.applyThinScrollBar(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void initListeners() {
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        filterCombo.addActionListener(e -> applyFilters());
        sortCombo.addActionListener(e -> applyFilters());
        viewToggle.addActionListener(e -> {
            isListView = "List".equals(viewToggle.getSelectedItem());
            applyFilters();
        });
    }

    public void setCourses(List<Course> courses) {
        allCourses.clear();
        if (courses != null) {
            allCourses.addAll(courses);
        }
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        String filter = (String) filterCombo.getSelectedItem();
        String sort = (String) sortCombo.getSelectedItem();

        List<Course> filtered = new ArrayList<>();
        for (Course c : allCourses) {
            // Text search
            if (!query.isEmpty()) {
                boolean match = c.code().toLowerCase().contains(query) ||
                                c.title().toLowerCase().contains(query);
                if (!match) continue;
            }

            // Status filter
            if ("In progress".equals(filter) && c.progressPercent() >= 100) continue;
            if ("Past".equals(filter) && c.progressPercent() < 100) continue;
            if ("Future".equals(filter) && c.progressPercent() > 0) continue;

            filtered.add(c);
        }

        // Sorting
        if ("Sort by title".equals(sort)) {
            filtered.sort(Comparator.comparing(Course::title));
        } else if ("Sort by last accessed".equals(sort)) {
            filtered.sort((a, b) -> Integer.compare(b.progressPercent(), a.progressPercent()));
        } else {
            // Short name (code)
            filtered.sort(Comparator.comparing(Course::code));
        }

        renderCards(filtered);
    }

    private void renderCards(List<Course> courses) {
        gridContainer.removeAll();

        if (isListView) {
            gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
            for (Course c : courses) {
                CourseCard card = new CourseCard(c, onCourseSelected);
                card.setMaximumSize(new Dimension(Short.MAX_VALUE, 260));
                gridContainer.add(card);
                gridContainer.add(Box.createVerticalStrut(14));
            }
        } else {
            gridContainer.setLayout(new ResponsiveGridLayout(20, 20, 275));
            for (Course c : courses) {
                CourseCard card = new CourseCard(c, onCourseSelected);
                gridContainer.add(card);
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }
}
