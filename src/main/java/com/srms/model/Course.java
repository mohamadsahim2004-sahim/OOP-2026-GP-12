package com.srms.model;

/**
 * Immutable course record for the Moodle-style learning area.
 */
public record Course(
        String id,
        String code,
        String title,
        String semester,
        String coverSeed,
        int progressPercent,
        String coverPath,
        int credits,
        String lecturerName
) {
    public Course(String id, String code, String title, String semester, String coverSeed,
                  int progressPercent, String coverPath) {
        this(id, code, title, semester, coverSeed, progressPercent, coverPath, 3, "");
    }

    public Course(String id, String code, String title, String semester, String coverSeed, int progressPercent) {
        this(id, code, title, semester, coverSeed, progressPercent, null, 3, "");
    }
}
