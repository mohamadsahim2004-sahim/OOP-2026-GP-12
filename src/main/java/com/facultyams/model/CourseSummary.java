package com.facultyams.model;

/**
 * Immutable course record for the Moodle-style learning area.
 */
public record CourseSummary(
        String id,
        String code,
        String title,
        String semester,
        String coverSeed,
        int progressPercent,
        String coverPath,
        int credits,
        String lecturerName,
        String courseType,
        String scheduleTime,
        String lecturerEmail
) {
    public CourseSummary(String id, String code, String title, String semester, String coverSeed,
                         int progressPercent, String coverPath, int credits, String lecturerName,
                         String courseType, String scheduleTime) {
        this(id, code, title, semester, coverSeed, progressPercent, coverPath, credits, lecturerName,
                courseType, scheduleTime, "Not provided");
    }

    public CourseSummary(String id, String code, String title, String semester, String coverSeed,
                         int progressPercent, String coverPath, int credits, String lecturerName) {
        this(id, code, title, semester, coverSeed, progressPercent, coverPath, credits, lecturerName,
                "Not provided", "Not scheduled", "Not provided");
    }

    public CourseSummary(String id, String code, String title, String semester, String coverSeed,
                  int progressPercent, String coverPath) {
        this(id, code, title, semester, coverSeed, progressPercent, coverPath, 3, "", "Not provided",
                "Not scheduled", "Not provided");
    }

    public CourseSummary(String id, String code, String title, String semester, String coverSeed, int progressPercent) {
        this(id, code, title, semester, coverSeed, progressPercent, null, 3, "", "Not provided",
                "Not scheduled", "Not provided");
    }
}
