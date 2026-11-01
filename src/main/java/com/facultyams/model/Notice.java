package com.facultyams.model;

import java.time.LocalDate;

/**
 * A faculty notice (table {@code notices}). Written by an Admin, read by every logged-in role.
 * {@code createdBy} / {@code createdByName} identify the Admin who posted it; the name is read-only
 * and only filled by the DAO's join with {@code users}.
 */
public class Notice {

    private int noticeId;
    private String title;
    private String content;
    private LocalDate noticeDate;       // publication date
    private Integer createdBy;          // users.user_id, null if that account was removed
    private String createdByName;       // read-only, filled by DAO joins for display

    public Notice() {
    }

    public Notice(int noticeId, String title, String content, LocalDate noticeDate) {
        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.noticeDate = noticeDate;
    }

    public int getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(int noticeId) {
        this.noticeId = noticeId;
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

    public LocalDate getNoticeDate() {
        return noticeDate;
    }

    public void setNoticeDate(LocalDate noticeDate) {
        this.noticeDate = noticeDate;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    @Override
    public String toString() {
        return title + " (" + noticeDate + ")";
    }
}