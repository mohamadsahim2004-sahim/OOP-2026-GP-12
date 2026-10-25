package com.facultyams.ui.lecturer;

import com.facultyams.dao.MarksDAO;
import com.facultyams.model.Course;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.ResultService;
import com.facultyams.util.UIUtil;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Final results and grades of a course; calculate (recalculate), publish and unpublish. */
public class ResultsPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient ResultService service;
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Reg. No", "Name", "CA %", "Final %", "Overall", "Grade", "Grade points", "Exam eligible", "Status");
    private final JButton publishButton = new JButton("Publish Results");
    private final JButton unpublishButton = new JButton("Unpublish");
    private final JButton recalcButton = new JButton("Recalculate");

    public ResultsPanel() {
        this(new LecturerAccessService(), new ResultService());
    }

    public ResultsPanel(LecturerAccessService access, ResultService service) {
        super(access);
        this.service = service;
        JButton scaleButton = new JButton("Grading Scale");
        scaleButton.addActionListener(e -> showScale());
        toolbar.add(recalcButton);
        toolbar.add(publishButton);
        toolbar.add(unpublishButton);
        toolbar.add(scaleButton);
        recalcButton.addActionListener(e -> recalculate());
        publishButton.addActionListener(e -> publish());
        unpublishButton.addActionListener(e -> unpublish());
        add(new JScrollPane(LecturerUi.styledTable(model)), BorderLayout.CENTER);
        setButtons(false);
    }

    private void setButtons(boolean on) {
        publishButton.setEnabled(on);
        unpublishButton.setEnabled(on);
        recalcButton.setEnabled(on);
    }

    @Override
    protected void clearContent() {
        model.setRowCount(0);
        setButtons(false);
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.getCourseResults(course.getCourseId()), sheet -> {
            model.setRowCount(0);
            for (ResultService.ResultLine l : sheet.lines()) {
                MarksDAO.ResultRow r = l.result();
                if (l.complete()) {
                    model.addRow(new Object[]{l.student().registrationNo(), l.student().fullName(),
                            LecturerUi.num(r.caPercent()), LecturerUi.num(r.finalPercent()),
                            LecturerUi.num(r.overall()), r.grade(), LecturerUi.num(r.gradePoints()),
                            LecturerUi.yesNo(r.eligible()), r.published() ? "Published" : "Draft"});
                } else {
                    model.addRow(new Object[]{l.student().registrationNo(), l.student().fullName(),
                            "-", "-", "-", "-", "-", "-",
                            "Incomplete (" + l.marksEntered() + "/" + l.componentCount() + " marks)"});
                }
            }
            setButtons(true);
            status.setText(sheet.published() + " published, " + sheet.drafts() + " draft, "
                    + sheet.incomplete() + " incomplete."
                    + (sheet.scheme().complete() ? "" : "  Scheme incomplete: " + sheet.scheme().problem()));
        });
    }

    private void recalculate() {
        Course course = selectedCourse();
        LecturerUi.load(this, status, () -> service.recalculate(course.getCourseId()), n -> {
            UIUtil.showInfo(this, n + " result(s) recalculated from the latest marks and attendance. "
                    + "Published results are never changed.");
            loadCourse(course);
        });
    }

    private void publish() {
        Course course = selectedCourse();
        LecturerUi.load(this, status, () -> service.previewPublish(course.getCourseId()), p -> {
            if (p.toPublish() == 0) {
                UIUtil.showInfo(this, "There are no calculated draft results to publish.");
                return;
            }
            String message = "Publish " + p.toPublish() + " result(s) of " + course + "?\n\n"
                    + (p.incomplete() > 0 ? p.incomplete() + " student(s) have incomplete marks and will be skipped.\n" : "")
                    + (p.notConfirmedEligible() > 0 ? p.notConfirmedEligible()
                    + " result(s) belong to students whose exam eligibility is NOT confirmed.\n" : "")
                    + "\nPublished results are locked until you unpublish them.";
            if (!UIUtil.confirm(this, message)) {
                return;
            }
            LecturerUi.load(this, status, () -> service.publish(course.getCourseId()), done -> {
                UIUtil.showInfo(this, done.toPublish() + " result(s) published.");
                loadCourse(course);
            });
        });
    }

    private void unpublish() {
        Course course = selectedCourse();
        if (!UIUtil.confirm(this, "Take all published results of " + course + " back to draft?")) {
            return;
        }
        LecturerUi.load(this, status, () -> service.unpublish(course.getCourseId()), n -> {
            UIUtil.showInfo(this, n + " result(s) unpublished.");
            loadCourse(course);
        });
    }

    private void showScale() {
        DefaultTableModel scale = LecturerUi.readOnlyModel("Overall mark", "Grade", "Grade points");
        double upper = 100;
        for (AcademicRules.Band b : AcademicRules.gradeBands()) {
            scale.addRow(new Object[]{AcademicRules.fmt(b.minMark()) + " - " + AcademicRules.fmt(upper),
                    b.grade(), String.format("%.1f", b.points())});
            upper = b.minMark() - 0.01;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Grading Scale");
        dialog.add(new JScrollPane(LecturerUi.styledTable(scale)), BorderLayout.CENTER);
        dialog.add(new JLabel("  Overall = sum of (mark / maximum x weightage) over all components."),
                BorderLayout.SOUTH);
        dialog.setSize(new Dimension(420, 460));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
