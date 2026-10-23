package com.facultyams.service;

import com.facultyams.model.Notice;

@FunctionalInterface
public interface NoticeListener {
    void onNoticeUpdated(Notice notice);
}
