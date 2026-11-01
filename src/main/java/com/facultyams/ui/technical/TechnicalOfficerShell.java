package com.facultyams.ui.technical;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.ui.AppShell;

import javax.swing.JFrame;

public class TechnicalOfficerShell extends AppShell {

    public static final String PAGE_DASHBOARD = "dashboard";
    public static final String PAGE_ATTENDANCE = "mark";
    public static final String PAGE_RECORDS = "records";
    public static final String PAGE_MEDICAL_RECORDS = "medicalRecords";
    public static final String PAGE_MEDICAL_SUBMIT = "submission";
    public static final String PAGE_MEDICAL_APPROVAL = "approval";
    public static final String PAGE_ATTENDANCE_REPORT = "attendanceReports";
    public static final String PAGE_MEDICAL_REPORT = "medicalReports";

    public TechnicalOfficerShell(JFrame owner, User user) {
        super(owner, user, Role.TECHNICAL_OFFICER);
        init();
    }

    @Override
    protected void registerPages() {
        addPage(PAGE_DASHBOARD, "Dashboard", "Dashboard", new TODashboardPanel(this::showPage));
        addPage(PAGE_ATTENDANCE, "Attendance Management", "Attendance Management", new ClassAttendancePanel());
        addPage(PAGE_RECORDS, "Attendance Records/Summary", "Attendance Records / Summary", new AttendancePanel());
        addPage(PAGE_MEDICAL_RECORDS, "Medical Records", "Medical Records", new MedicalRecordsPanel());
        addPage(PAGE_MEDICAL_SUBMIT, "Medical Submission", "Medical Submission", new MedicalSubmissionPanel());
        addPage(PAGE_MEDICAL_APPROVAL, "Medical Approval", "Medical Approval", new MedicalApprovalPanel());
        addPage(PAGE_ATTENDANCE_REPORT, "Attendance Reports", "Attendance Reports", new AttendanceReportPanel());
        addPage(PAGE_MEDICAL_REPORT, "Medical Reports", "Medical Reports", new MedicalReportPanel());
    }
}