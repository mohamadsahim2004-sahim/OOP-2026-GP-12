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
        addPage("attendance", "Attendance", "Attendance Management", new AttendancePanel());
        addPage("medical", "Medical", "Medical Management", new MedicalPanel());
        addPage("reports", "Reports", "Attendance Reports", new AttendanceReportPanel());
    }
}