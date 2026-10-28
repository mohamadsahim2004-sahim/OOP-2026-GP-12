package com.facultyams.service;

import com.facultyams.dao.StudentNoticeDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.student.StudentNotice;
import com.facultyams.security.StudentAccess;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Notices for the logged-in student plus that student's private read state. */
public class StudentNoticeService {
    private static StudentNoticeService instance;
    private static int instanceOwner = -1;

    private final int studentId;
    private final StudentNoticeDAO dao = new StudentNoticeDAO();
    private final List<NoticeListener> listeners = new CopyOnWriteArrayList<>();

    private StudentNoticeService(int studentId) {
        this.studentId = studentId;
    }

    public static synchronized StudentNoticeService getInstance() {
        int me = StudentAccess.currentStudentId();
        if (instance == null || instanceOwner != me) {
            instance = new StudentNoticeService(me);
            instanceOwner = me;
        }
        return instance;
    }

    public List<StudentNotice> getNotices() {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return dao.findAll(studentId);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Notices could not be loaded. Please try again.", e);
        }
    }

    public StudentNotice getNotice(int id) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return dao.findById(studentId, id);
        } catch (DatabaseException e) {
            throw new StudentServiceException("The notice could not be loaded. Please try again.", e);
        }
    }

    public int unreadCount() {
        return (int) getNotices().stream().filter(n -> !n.isRead()).count();
    }

    public void markAsRead(StudentNotice notice) {
        if (notice == null || notice.isRead()) return;
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            dao.markRead(studentId, notice.getId());
        } catch (DatabaseException e) {
            throw new StudentServiceException("The notice could not be marked as read.", e);
        }
        notice.setRead(true);
        for (NoticeListener listener : listeners) listener.onNoticeUpdated(notice);
    }

    public void addListener(NoticeListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(NoticeListener listener) {
        listeners.remove(listener);
    }
}
