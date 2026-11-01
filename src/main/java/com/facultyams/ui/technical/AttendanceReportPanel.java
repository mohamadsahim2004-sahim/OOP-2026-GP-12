package com.facultyams.ui.technical;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.ReportResult;
import com.facultyams.service.TechnicalReportService;

import java.util.concurrent.Callable;

/** Attendance Reports: per student and course totals and percentages for the chosen filters, with chart, print and export. */
public class AttendanceReportPanel extends AbstractReportPanel {

    private static final long serialVersionUID = 1L;
    private final transient TechnicalReportService reports = new TechnicalReportService();

    public AttendanceReportPanel() {
        super("attendance-report");
        buildLayout();
    }

    @Override
    protected Callable<ReportResult> prepare() throws ValidationException {
        AttendanceFilter f = new AttendanceFilter(studentField.getText(), null,
                selectedCourse() == null ? null : selectedCourse().courseId(), null, selectedBatch(),
                parseDate(fromField.getText(), "The 'from' date"), parseDate(toField.getText(), "The 'to' date"), null);
        String text = describeFilters();
        return () -> reports.attendanceReport(f, text);
    }
}