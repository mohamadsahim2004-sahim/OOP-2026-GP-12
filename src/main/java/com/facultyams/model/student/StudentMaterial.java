package com.facultyams.model.student;

/**
 * A course material row for an enrolled course ({@code course_materials}). The lecturer module stores a
 * description, optional notes and a reference (web link, file path or plain text); there is no file size or
 * upload date in the database, so none is shown.
 */
public class StudentMaterial {
    private final int id;
    private final int courseId;
    private final String courseCode;
    private final String courseName;
    private final String lecturer;
    private final String title;
    private final String notes;
    private final String reference;

    public StudentMaterial(int id, int courseId, String courseCode, String courseName, String lecturer,
                           String title, String notes, String reference) {
        this.id = id;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturer = lecturer == null ? "" : lecturer;
        this.title = title == null ? "" : title;
        this.notes = notes == null ? "" : notes;
        this.reference = reference == null ? "" : reference.trim();
    }

    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getLecturer() { return lecturer; }
    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public String getReference() { return reference; }

    public boolean isLink() {
        String r = reference.toLowerCase();
        return r.startsWith("http://") || r.startsWith("https://");
    }

    /** "Link", "File" (a reference that names a file) or "Reference" (plain text). */
    public String getType() {
        if (reference.isEmpty()) return "Note";
        if (isLink()) return "Link";
        return reference.matches("(?i).*\\.[a-z0-9]{2,5}$") && !reference.contains(" ") ? "File" : "Reference";
    }
}
