package com.facultyams.model;

import java.time.LocalDate;

/** Totals of one recorded session, for the dashboard's recent-activity list. */
public record RecentSession(LocalDate date, String courseCode, String type, int sessionNo,
                            int present, int absent, int late) {
}