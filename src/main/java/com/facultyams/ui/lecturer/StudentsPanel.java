package com.facultyams.ui.lecturer;

import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.model.Course;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.LecturerService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** Undergraduate details: only the students of the lecturer's own courses. */
public class StudentsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final Course ALL = new Course(0, "ALL", "All my courses", 1, 1, 1, Course.CourseType.THEORY, 1);

    private final transient LecturerService service;
    private final transient LecturerAccessService access;
    private final JComboBox<Course> courseCombo = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel model = LecturerUi.readOnlyModel("Reg. No", "Name", "Batch", "Student status");
    private boolean populating;

    public StudentsPanel() {
        this(new LecturerAccessService(), new LecturerService());
    }

    public StudentsPanel(LecturerAccessService access, LecturerService service) {
        super(new BorderLayout(0, 8));
        this.access = access;
        this.service = service;
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(new JLabel("Course:"));
        bar.add(courseCombo);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        bar.add(refreshButton);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(LecturerUi.styledTable(model)), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        courseCombo.addActionListener(e -> {
            if (!populating) {
                loadStudents();
            }
        });
    }

    @Override
    public void refresh() {
        Course previous = (Course) courseCombo.getSelectedItem();
        LecturerUi.load(this, status, access::getAssignedCourses, (List<Course> courses) -> {
            populating = true;
            courseCombo.removeAllItems();
            courseCombo.addItem(ALL);
            Course select = ALL;
            for (Course c : courses) {
                courseCombo.addItem(c);
                if (previous != null && previous.getCourseId() == c.getCourseId()) {
                    select = c;
                }
            }
            courseCombo.setSelectedItem(select);
            populating = false;
            loadStudents();
        });
    }

    private void loadStudents() {
        Course chosen = (Course) courseCombo.getSelectedItem();
        Integer courseId = chosen == null || chosen == ALL ? null : chosen.getCourseId();
        LecturerUi.load(this, status, () -> service.getStudents(courseId), list -> {
            model.setRowCount(0);
            for (StudentRow s : list) {
                model.addRow(new Object[]{s.registrationNo(), s.fullName(), s.batch(), s.status()});
            }
            status.setText(list.isEmpty()
                    ? "No students are enrolled in your courses yet."
                    : list.size() + " student(s).");
        });
    }
}
