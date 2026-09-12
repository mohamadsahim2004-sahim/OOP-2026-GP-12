package com.fot.ams.model;

import java.util.Arrays;
import java.util.List;

/**
 * TechnicalOfficer role model extending User.
 * Primary responsibilities per SRS 3.4: Attendance management, Medical records.
 */
public class TechnicalOfficer extends User {

    public TechnicalOfficer(int userID, String username, String passwordHash, String fullName, 
                            String email, String phone, String departmentID) {
        super(userID, username, passwordHash, UserRole.TECHNICAL_OFFICER, fullName, email, phone, departmentID);
    }

    @Override
    public String getDashboardTitle() {
        return "Technical Officer Academic Operations Hub";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(
            "Mark & Maintain Undergraduate Attendance (Theory / Practical)",
            "Attendance Eligibility Determination (80% Rule)",
            "Record & Approve Medical Submissions",
            "View Department Timetables",
            "View Academic Notices"
        );
    }

    @Override
    public String getRoleSpecificIdentifier() {
        return "TO Department: " + getDepartmentID();
    }
}
