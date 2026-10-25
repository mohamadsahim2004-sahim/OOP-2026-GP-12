package com.facultyams.ui.technical;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.ui.AppShell;

import javax.swing.JFrame;

public class TechnicalOfficerShell extends AppShell {

    public TechnicalOfficerShell(JFrame owner, User user) {
        super(owner, user, Role.TECHNICAL_OFFICER);
        init();
    }

    @Override
    protected void registerPages() {
        addPage("dashboard", "Dashboard", "Dashboard", new TODashboardPanel(this));
        addPage("mark", "Attendance Management", "Attendance Management", new ClassAttendancePanel());
        addPage("records", "Attendance Records/Summary", "Attendance Records / Summary", new AttendancePanel());
        addPage("medicalRecords", "Medical Records", "Medical Records", new MedicalRecordsPanel());
        addPage("submission", "Medical Submission", "Medical Submission", new MedicalSubmissionPanel());
        addPage("approval", "Medical Approval", "Medical Approval", new MedicalApprovalPanel());
        addPage("attendanceReports", "Attendance Reports", "Attendance Reports", new AttendanceReportPanel());
        addPage("medicalReports", "Medical Reports", "Medical Reports", new MedicalReportPanel());
    }
}