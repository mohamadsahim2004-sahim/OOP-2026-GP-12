package com.facultyams.service;

import com.facultyams.model.Notice;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NoticeService {
    private static final NoticeService INSTANCE = new NoticeService(DataService.getInstance());
    private final DataService dataService;
    private final List<NoticeListener> listeners = new CopyOnWriteArrayList<>();

    public NoticeService(DataService dataService) {
        this.dataService = dataService;
    }

    public static NoticeService getInstance() {
        return INSTANCE;
    }

    public Notice getNotice(int id) {
        return dataService.getNotices().stream()
                .filter(notice -> notice.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public void markAsRead(Notice notice) {
        if (notice == null || notice.isRead()) return;
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
