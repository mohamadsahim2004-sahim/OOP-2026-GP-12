package com.facultyams.model.student;

import com.facultyams.model.NoticeAttachment;
import com.facultyams.model.NoticeCategory;
import com.facultyams.model.NoticePriority;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A notice as shown to a student: the shared notice plus this student's own read state. */
public class StudentNotice {
    private int id;
    private String title;
    private String content;
    private String date;
    private String category;
    private String publisher;
    private boolean isImportant;
    private boolean read;
    private NoticePriority priority = NoticePriority.NORMAL;
    private final List<NoticeAttachment> attachments = new ArrayList<>();

    public StudentNotice() {
    }

    public StudentNotice(int id, String title, String content, String date, String category, String publisher, boolean isImportant) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.category = category;
        this.publisher = publisher;
        this.isImportant = isImportant;
        this.priority = isImportant ? NoticePriority.IMPORTANT : NoticePriority.NORMAL;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
        priority = important ? NoticePriority.IMPORTANT : NoticePriority.NORMAL;
    }

    public String getBody() { return content; }
    public String getIssuer() { return publisher; }
    public String getDepartment() {
        if (publisher == null) return "";
        int separator = publisher.lastIndexOf('/');
        return separator < 0 ? "" : publisher.substring(separator + 1).trim();
    }
    public NoticeCategory getNoticeCategory() { return NoticeCategory.fromLabel(category); }
    public NoticePriority getPriority() { return priority; }
    public void setPriority(NoticePriority priority) {
        this.priority = priority == null ? NoticePriority.NORMAL : priority;
        isImportant = this.priority != NoticePriority.NORMAL;
    }
    public LocalDate getLocalDate() {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public List<NoticeAttachment> getAttachments() { return List.copyOf(attachments); }
    public void setAttachments(List<NoticeAttachment> files) {
        attachments.clear();
        if (files != null) attachments.addAll(files);
    }
}
