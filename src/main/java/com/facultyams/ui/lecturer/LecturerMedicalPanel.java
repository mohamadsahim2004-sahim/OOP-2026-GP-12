package com.facultyams.ui.lecturer;

import com.facultyams.dao.LecturerAccessDAO.MedicalRow;
import com.facultyams.model.Course;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.LecturerMedicalService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/**
 * Medical records of absences in the lecturer's own courses. Read-only and privacy-limited: the
 * reason / diagnosis is never loaded or shown, only the approval status.
 */
public class LecturerMedicalPanel extends CoursePanel {

    private static final long serialVersionUID = 1L;

    private final transient LecturerMedicalService service;
    private final DefaultTableModel model = LecturerUi.readOnlyModel(
            "Medical ID", "Reg. No", "Student", "Component", "Session", "Session date",
            "Submitted", "Status", "Reviewed by", "Reason");

    public LecturerMedicalPanel() {
        this(new LecturerAccessService(), new LecturerMedicalService());
    }

    public LecturerMedicalPanel(LecturerAccessService access, LecturerMedicalService service) {
        super(access);
        this.service = service;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(refreshButton);

        JLabel privacy = new JLabel(LecturerMedicalService.PRIVACY_NOTICE);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(privacy, BorderLayout.NORTH);
        center.add(new JScrollPane(LecturerUi.styledTable(model)), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    @Override
    protected void clearContent() {
        model.setRowCount(0);
    }

    @Override
    protected void loadCourse(Course course) {
        LecturerUi.load(this, status, () -> service.getMedicalRecords(course.getCourseId()), view -> {
            model.setRowCount(0);
            for (MedicalRow r : view.rows()) {
                model.addRow(new Object[]{r.medicalId(), r.registrationNo(), r.fullName(), r.component(),
                        r.sessionNo(), r.sessionDate(), r.submittedDate(), r.status(),
                        r.reviewedBy() == null ? "-" : r.reviewedBy(), "Confidential"});
            }
            status.setText(view.rows().isEmpty()
                    ? "No medical records for this course."
                    : view.rows().size() + " record(s): " + view.pending() + " pending, "
                    + view.approved() + " approved, " + view.rejected() + " rejected.");
        });
    }
}
