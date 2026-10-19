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
        String coverPath
) {
    public Course(String id, String code, String title, String semester, String coverSeed, int progressPercent) {
        this(id, code, title, semester, coverSeed, progressPercent, null);
    }
}
