package com.facultyams.service;

import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.MedicalRow;
import com.facultyams.model.ReportResult;
import com.facultyams.model.StudentSummaryRow;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.Display;
import com.facultyams.util.ReportTable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the attendance and medical reports from live database data. Only technical officers may
 * generate them; the medical report contains personal medical information.
 */
public class TechnicalReportService {

    private final AttendanceService attendanceService;
    private final MedicalService medicalService;

    public TechnicalReportService() {
        this(new AttendanceService(), new MedicalService());
    }

    public TechnicalReportService(AttendanceService attendanceService, MedicalService medicalService) {
        this.attendanceService = attendanceService;
        this.medicalService = medicalService;
    }

    /**
     * @param filterText human-readable description of the chosen filters, printed on the report
     */
    public ReportResult attendanceReport(AttendanceFilter filter, String filterText)
            throws ValidationException, DatabaseException {
        Session.requireRole(Role.TECHNICAL_OFFICER);
        List<StudentSummaryRow> data = attendanceService.getSummary(filter);

        int eligible = 0;
        double sum = 0;
        int[] bands = new int[4];
        List<List<String>> rows = new ArrayList<>();
        for (StudentSummaryRow r : data) {
            sum += r.percentage();
            if (r.eligible()) {
                eligible++;
            }
            bands[r.percentage() >= 80 ? 3 : r.percentage() >= 65 ? 2 : r.percentage() >= 50 ? 1 : 0]++;
            rows.add(List.of(r.registrationNo(), r.fullName(), r.batch(), r.courseCode(), Display.capitalize(r.type()),
                    String.valueOf(r.present()), String.valueOf(r.late()), String.valueOf(r.absent()),
                    String.valueOf(r.approvedMedical()), String.valueOf(r.recorded()),
                    Display.percent(r.percentage()), Display.yesNo(r.eligible())));
        }
        String basis = filter.hasDateRange()
                ? "sessions recorded in the selected date range"
                : AttendanceService.TOTAL_SESSIONS + " sessions per course component";
        double avg = data.isEmpty() ? 0 : sum / data.size();
        String summary = data.size() + " student rows  |  average " + Display.percent(avg) + "  |  eligible (80%+) "
                + eligible + "  |  not eligible " + (data.size() - eligible);

        List<String> info = List.of(
                "Filters: " + filterText,
                "Attendance % = (present + late + approved medical) / " + basis,
                summary,
                "Generated " + Display.dateTime(LocalDateTime.now()));
        ReportTable table = new ReportTable("Attendance Report", info,
                List.of("Reg No", "Student", "Batch", "Course", "Type", "Present", "Late", "Absent",
                        "Medical", "Recorded", "Attendance %", "Eligible"), rows);

        Map<String, Integer> chart = new LinkedHashMap<>();
        chart.put("Below 50%", bands[0]);
        chart.put("50-64%", bands[1]);
        chart.put("65-79%", bands[2]);
        chart.put("80% and above", bands[3]);
        return new ReportResult(table, summary, chart, "Students by attendance band");
    }

    public ReportResult medicalReport(MedicalFilter filter, String filterText)
            throws ValidationException, DatabaseException {
        Session.requireRole(Role.TECHNICAL_OFFICER);
        List<MedicalRow> data = medicalService.list(filter);

        int pending = 0;
        int approved = 0;
        int rejected = 0;
        List<List<String>> rows = new ArrayList<>();
        for (MedicalRow m : data) {
            switch (m.status()) {
                case MedicalService.APPROVED -> approved++;
                case MedicalService.REJECTED -> rejected++;
                default -> pending++;
            }
            rows.add(List.of(String.valueOf(m.medicalId()), m.registrationNo(), m.studentName(), m.batch(),
                    m.courseCode(), Display.capitalize(m.type()), String.valueOf(m.sessionNo()),
                    Display.date(m.sessionDate()), Display.date(m.submittedDate()), m.reason(),
                    m.hasEvidence() ? Display.orDash(m.evidenceName()) : "-", Display.capitalize(m.status()),
                    Display.orDash(m.reviewerName()), Display.dateTime(m.decidedAt()).isEmpty() ? "-" : Display.dateTime(m.decidedAt()),
                    Display.orDash(m.rejectionReason())));
        }
        String summary = data.size() + " medicals  |  pending " + pending + "  |  approved " + approved
                + "  |  rejected " + rejected;
        List<String> info = List.of(
                "Filters: " + filterText + " (dates apply to the submission date)",
                summary,
                "Generated " + Display.dateTime(LocalDateTime.now()));
        ReportTable table = new ReportTable("Medical Report", info,
                List.of("ID", "Reg No", "Student", "Batch", "Course", "Type", "Session", "Session date", "Submitted",
                        "Reason", "Document", "Status", "Reviewed by", "Decided at", "Rejection reason"), rows);

        Map<String, Integer> chart = new LinkedHashMap<>();
        chart.put("Pending", pending);
        chart.put("Approved", approved);
        chart.put("Rejected", rejected);
        return new ReportResult(table, summary, chart, "Medicals by status");
    }
}