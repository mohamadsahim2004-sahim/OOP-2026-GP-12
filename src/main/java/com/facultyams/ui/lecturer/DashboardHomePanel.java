package com.facultyams.ui.lecturer;

import com.facultyams.model.Course;
import com.facultyams.model.Lecturer;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerService;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

/** Lecturer dashboard: live figures from the database and a list of the lecturer's courses. */
public class DashboardHomePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(23, 54, 92);

    private final transient LecturerService service;
    private final JLabel welcome = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JLabel[] values = new JLabel[6];
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Code", "Course", "Credits", "Level / Semester", "Students", "Evaluation scheme", "Results (published / total)");

    public DashboardHomePanel(Consumer<String> navigate) {
        this(new LecturerService(), navigate);
    }

    public DashboardHomePanel(LecturerService service, Consumer<String> navigate) {
        super(new BorderLayout(0, 12));
        this.service = service;
        setOpaque(false);

        welcome.setFont(new Font("Arial", Font.PLAIN, 15));
        welcome.setForeground(new Color(40, 69, 99));

        String[] captions = {"Assigned courses", "Students", "Courses without a complete scheme",
                "Results waiting to be published", "Published results", "Pending medical records"};
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        for (int i = 0; i < captions.length; i++) {
            values[i] = new JLabel("-", JLabel.CENTER);
            values[i].setFont(new Font("Arial", Font.BOLD, 26));
            values[i].setForeground(NAVY);
            JLabel caption = new JLabel(captions[i], JLabel.CENTER);
            caption.setFont(new Font("Arial", Font.PLAIN, 12));
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
            card.add(values[i], BorderLayout.CENTER);
            card.add(caption, BorderLayout.SOUTH);
            cards.add(card);
        }

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        links.setOpaque(false);
        for (String section : new String[]{"Evaluation Components", "Marks Entry", "Exam Eligibility",
                "Final Results and Grades", "Attendance and Medical Records"}) {
            JButton b = new JButton(section);
            b.addActionListener(e -> navigate.accept(section));
            links.add(b);
        }

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(welcome, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        top.add(links, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(LecturerUi.styledTable(model)), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        LecturerUi.load(this, status, service::getDashboardSummary, s -> {
            Lecturer l = s.lecturer();
            welcome.setText("Welcome, " + l.getFullName() + " (" + l.getDesignation() + ")");
            int[] figures = {s.courses(), s.students(), s.coursesWithoutScheme(),
                    s.resultsToPublish(), s.publishedResults(), s.pendingMedical()};
            for (int i = 0; i < figures.length; i++) {
                values[i].setText(String.valueOf(figures[i]));
            }
            model.setRowCount(0);
            for (LecturerService.CourseLine line : s.courseLines()) {
                Course c = line.course();
                model.addRow(new Object[]{c.getCourseCode(), c.getCourseName(), c.getCredits(),
                        c.getSemesterLabel(), line.students(),
                        line.schemeComplete() ? "Complete"
                                : "Incomplete (" + AcademicRules.fmt(line.totalWeight()) + "% of 100%)",
                        line.published() + " / " + line.results()});
            }
            status.setText(s.courses() == 0
                    ? "No courses are assigned to you yet. Ask the administrator to assign courses."
                    : " ");
        });
    }
}
