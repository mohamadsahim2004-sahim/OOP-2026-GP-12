package com.facultyams.ui.technical;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.ReportResult;
import com.facultyams.service.TechnicalReportService;

import javax.swing.JComboBox;
import java.util.Locale;
import java.util.concurrent.Callable;

/** Medical Reports: submissions and decisions for the chosen filters (dates are submission dates), with totals per status. */
public class MedicalReportPanel extends AbstractReportPanel {

    private static final long serialVersionUID = 1L;
    private final transient TechnicalReportService reports = new TechnicalReportService();
    private final JComboBox<String> statusBox = new JComboBox<>(new String[] {"All statuses", "Pending", "Approved", "Rejected"});

    public MedicalReportPanel() {
        super("medical-report");
        filters.add(labeled("Status", statusBox));
        buildLayout();
    }

    @Override
    protected Callable<ReportResult> prepare() throws ValidationException {
        String status = statusBox.getSelectedIndex() > 0
                ? String.valueOf(statusBox.getSelectedItem()).toLowerCase(Locale.ROOT) : null;
        MedicalFilter f = new MedicalFilter(studentField.getText(), null,
                selectedCourse() == null ? null : selectedCourse().courseId(), selectedBatch(), status,
                parseDate(fromField.getText(), "The 'from' date"), parseDate(toField.getText(), "The 'to' date"));
        String text = describeFilters(status == null ? null : "Status: " + status);
        return () -> reports.medicalReport(f, text);
    }
}