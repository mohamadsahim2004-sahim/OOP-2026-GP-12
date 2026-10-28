
package com.facultyams.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.*;
import com.facultyams.service.StudentCourseService;
import com.facultyams.service.SubmissionService;
import com.facultyams.ui.components.ActivityRow;
import com.facultyams.ui.components.CollapsibleSection;

/**
 * Screen B: Course Page with course header, weekly collapsible sections,
 * and vector activity rows. Remembers section collapse states per course.
 */
public class CoursePageView extends JPanel {
    private final StudentCourseService courseService;
    private final SubmissionService submissionService;
    private final String studentId;
    private final Runnable onBack;
    private final Consumer<Assignment> onAssignmentSelected;

    private CourseSummary currentCourse;
    private final JPanel sectionsContainer = new JPanel();
    private final List<CollapsibleSection> activeSections = new ArrayList<>();
    private final JLabel titleLbl = new JLabel();
    private final JLabel codeLbl = new JLabel();

    // In-memory cache remembering collapsed sections per course
    private static final Map<String, Set<String>> COLLAPSED_STATE_CACHE = new HashMap<>();

    public CoursePageView(StudentCourseService courseService, SubmissionService submissionService,
                          String studentId, Runnable onBack, Consumer<Assignment> onAssignmentSelected) {
        this.courseService = courseService;
        this.submissionService = submissionService;
        this.studentId = studentId;
        this.onBack = onBack;
        this.onAssignmentSelected = onAssignmentSelected;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 4, 16, 4));

        initHeader();
        initSectionsArea();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // "< Back to courses" link
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
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLbl);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(codeLbl);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initSectionsArea() {
        sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.Y_AXIS));
        sectionsContainer.setOpaque(false);

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(sectionsContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollWrapper);
        Ui.applyThinScrollBar(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadCourse(CourseSummary course) {
        this.currentCourse = course;
        titleLbl.setText(course.title());
        codeLbl.setText(course.code() + " • " + course.semester());

        sectionsContainer.removeAll();
        activeSections.clear();

        // Load sections off-EDT or asynchronously via SwingWorker
        new SwingWorker<List<Section>, Void>() {
            @Override
            protected List<Section> doInBackground() {
                return courseService.sections(course.id());
            }

            @Override
            protected void done() {
                try {
                    List<Section> sections = get();
                    buildSectionsUI(sections);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CoursePageView.this,
                            "Failed to load course sections: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void buildSectionsUI(List<Section> sections) {
        sectionsContainer.removeAll();
        activeSections.clear();

        Set<String> collapsedIds = COLLAPSED_STATE_CACHE.computeIfAbsent(currentCourse.id(), k -> new HashSet<>());

        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            boolean isFirst = (i == 0);

            CollapsibleSection sectionComp = new CollapsibleSection(
                    sec.id(),
                    sec.title(),
                    isFirst,
                    this::toggleAllSections
            );

            // Populate activities
            List<Activity> acts = courseService.activities(sec.id());
            for (Activity act : acts) {
                ActivityRow row = new ActivityRow(act, studentId, submissionService, onAssignmentSelected);
                sectionComp.addActivityRow(row);
            }

            // Restore collapsed state
            if (collapsedIds.contains(sec.id())) {
                sectionComp.setExpanded(false, false);
            }

            activeSections.add(sectionComp);
            sectionsContainer.add(sectionComp);
            sectionsContainer.add(Box.createVerticalStrut(16));
        }

        updateToggleAllText();
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }

    private void toggleAllSections() {
        boolean anyExpanded = activeSections.stream().anyMatch(CollapsibleSection::isExpanded);
        boolean target = !anyExpanded;

        Set<String> collapsedIds = COLLAPSED_STATE_CACHE.computeIfAbsent(currentCourse.id(), k -> new HashSet<>());

        for (CollapsibleSection sec : activeSections) {
            sec.setExpanded(target, true);
            if (target) {
                collapsedIds.remove(sec.getSectionId());
            } else {
                collapsedIds.add(sec.getSectionId());
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
        for (CollapsibleSection sec : activeSections) {
            sec.updateAllStatus();
        }
    }
}
