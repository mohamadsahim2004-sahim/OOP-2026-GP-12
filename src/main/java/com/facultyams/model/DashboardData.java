package com.facultyams.model;

import java.util.List;

/** Everything the Technical Officer dashboard shows, read from the database in one go. */
public record DashboardData(int students, int attendanceRecords, int presentToday, int absentToday, int lateToday, int pendingMedicals, int approvedMedicals,
                            int rejectedMedicals, List<RecentSession> recentSessions, List<MedicalRow> pendingList) {
}