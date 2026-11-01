package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Base of every course-based Lecturer screen: a course picker that lists ONLY the logged-in
 * lecturer's assigned courses, a status line (loading / error / empty state) and the reload
 * behaviour. Subclasses fill the centre and react to {@link #loadCourse(Course)}.
 */
abstract class CoursePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    protected final JComboBox<Course> courseCombo = new JComboBox<>();
    protected final JLabel status = new JLabel(" ");
    protected final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final transient LecturerAccessService access;
    private boolean populating;

    CoursePanel(LecturerAccessService access) {
        super(new BorderLayout(0, 8));
        this.access = access;
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Course:"));
        courseCombo.setPrototypeDisplayValue(new Course(0, "ICT0000", "Long course name for the picker", 1, 1, 1,
                Course.CourseType.THEORY, 1));
        toolbar.add(courseCombo);
        status.setFont(status.getFont().deriveFont(Font.ITALIC));
        add(toolbar, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);
        courseCombo.addActionListener(e -> {
            if (!populating && selectedCourse() != null) {
                loadCourse(selectedCourse());
            }
        });
    }

    /** Called whenever a course is chosen or the screen is refreshed. */
    protected abstract void loadCourse(Course course);

    /** Called when the lecturer has no assigned course. */
    protected abstract void clearContent();

    protected Course selectedCourse() {
        return (Course) courseCombo.getSelectedItem();
    }

    @Override
    public void refresh() {
        Course previous = selectedCourse();
        LecturerUi.load(this, status, access::getAssignedCourses, (List<Course> courses) -> {
            populating = true;
            courseCombo.removeAllItems();
            Course toSelect = null;
            for (Course c : courses) {
                courseCombo.addItem(c);
                if (previous != null && previous.getCourseId() == c.getCourseId()) {
                    toSelect = c;
                }
            }
            if (toSelect != null) {
                courseCombo.setSelectedItem(toSelect);
            }
            populating = false;
            if (courses.isEmpty()) {
                clearContent();
                status.setText("No courses are assigned to you yet. Ask the administrator to assign courses.");
            } else {
                loadCourse(selectedCourse());
            }
        });
    }
}
