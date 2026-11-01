package com.facultyams.service;

import com.facultyams.model.student.StudentNotice;

@FunctionalInterface
public interface NoticeListener {
    void onNoticeUpdated(StudentNotice notice);
}
