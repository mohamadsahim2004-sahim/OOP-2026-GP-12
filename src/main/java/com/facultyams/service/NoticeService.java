package com.facultyams.service;

import com.facultyams.dao.NoticeDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Notice;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Notice rules. Posting, editing and deleting are Admin-only; reading is open to every logged-in user
 * so each role's own screen (student, lecturer, technical officer) can show the notices.
 * The permission check always runs first, before any argument is looked at.
 */
public class NoticeService {

    public static final int MAX_TITLE_LENGTH = 150;     // notices.title is VARCHAR(150)
    public static final int MAX_CONTENT_LENGTH = 5000;  // notices.content is TEXT; keep notices readable
    /** Guards against typos such as 0026-09-20 or 20260-... that would otherwise be stored silently. */
    public static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);
    public static final LocalDate LATEST_DATE = LocalDate.of(2100, 12, 31);

    private final NoticeDAO noticeDAO;

    public NoticeService() {
        this(new NoticeDAO());
    }

    public NoticeService(NoticeDAO noticeDAO) {
        this.noticeDAO = noticeDAO;
    }

    // ----- reading: any logged-in user -----

    /** Every notice, newest first. */
    public List<Notice> getAllNotices() throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return noticeDAO.findAll();
    }

    /** Notices whose title or content contains the keyword; a blank keyword returns every notice. */
    public List<Notice> searchNotices(String keyword) throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return noticeDAO.search(keyword);
    }

    public Notice getNoticeById(int noticeId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        ValidationUtil.requirePositiveId(noticeId, "notice id");
        Notice notice = noticeDAO.findById(noticeId);
        if (notice == null) {
            throw new ValidationException("Notice with id " + noticeId + " was not found.");
        }
        return notice;
    }

    // ----- changing: Admin only -----

    /** Validates and saves a new notice, recording the logged-in Admin as its author. Returns the saved notice. */
    public Notice addNotice(Notice notice) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (notice == null) {
            throw new ValidationException("Notice details are required.");
        }
        validate(notice);
        User admin = Session.getCurrentUser();
        notice.setNoticeId(0);
        notice.setCreatedBy(admin.getUserId());
        notice.setCreatedByName(admin.getFullName());
        noticeDAO.insert(notice);
        return notice;
    }

    /** Changes title, content and date of an existing notice. The original author is kept. */
    public void updateNotice(Notice notice) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (notice == null) {
            throw new ValidationException("Notice details are required.");
        }
        getNoticeById(notice.getNoticeId());
        validate(notice);
        noticeDAO.update(notice);
    }

    public void deleteNotice(int noticeId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getNoticeById(noticeId);
        noticeDAO.delete(noticeId);
    }

    /** Checks every field and stores the trimmed title and content back into the notice. */
    private void validate(Notice notice) throws ValidationException {
        String title = ValidationUtil.requireNotBlank(notice.getTitle(), "Title");
        ValidationUtil.requireMaxLength(title, MAX_TITLE_LENGTH, "Title");
        String content = ValidationUtil.requireNotBlank(notice.getContent(), "Content");
        ValidationUtil.requireMaxLength(content, MAX_CONTENT_LENGTH, "Content");

        LocalDate date = notice.getNoticeDate();
        if (date == null) {
            throw new ValidationException("Notice date is required.");
        }
        if (date.isBefore(EARLIEST_DATE) || date.isAfter(LATEST_DATE)) {
            throw new ValidationException("Notice date must be between " + EARLIEST_DATE + " and "
                    + LATEST_DATE + ".");
        }
        notice.setTitle(title);
        notice.setContent(content);
    }
}