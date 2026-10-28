package com.facultyams.dao;

import com.facultyams.exception.DatabaseException;
import com.facultyams.model.NoticePriority;
import com.facultyams.model.student.StudentNotice;

import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Notices for students with each student's private read state ({@code notice_reads}). */
public class StudentNoticeDAO {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

    private static final String SELECT =
            "SELECT n.notice_id, n.title, n.content, n.notice_date, n.category, n.priority, u.full_name AS publisher, "
                    + "(r.notice_id IS NOT NULL) AS is_read "
                    + "FROM notices n LEFT JOIN users u ON u.user_id = n.created_by "
                    + "LEFT JOIN notice_reads r ON r.notice_id = n.notice_id AND r.user_id = ? ";

    private static final StudentDaoSupport.RowMapper<StudentNotice> MAPPER = rs -> {
        Date date = rs.getDate("notice_date");
        String priority = rs.getString("priority");
        String publisher = rs.getString("publisher");
        StudentNotice n = new StudentNotice(rs.getInt("notice_id"), rs.getString("title"), rs.getString("content"),
                date == null ? "" : date.toLocalDate().format(DISPLAY), labelOf(rs.getString("category")),
                publisher == null || publisher.isBlank() ? "Faculty Administration" : publisher,
                priority != null && !"NORMAL".equalsIgnoreCase(priority));
        n.setPriority(priorityOf(priority));
        n.setRead(rs.getBoolean("is_read"));
        return n;
    };

    private static String labelOf(String category) {
        if (category == null || category.isBlank()) return "General";
        String c = category.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(c.charAt(0)) + c.substring(1);
    }

    private static NoticePriority priorityOf(String value) {
        if (value == null) return NoticePriority.NORMAL;
        try {
            return NoticePriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NoticePriority.NORMAL;
        }
    }

    public List<StudentNotice> findAll(int studentId) throws DatabaseException {
        return StudentDaoSupport.query(SELECT + "ORDER BY n.notice_date DESC, n.notice_id DESC", MAPPER, studentId);
    }

    public StudentNotice findById(int studentId, int noticeId) throws DatabaseException {
        return StudentDaoSupport.queryOne(SELECT + "WHERE n.notice_id = ?", MAPPER, studentId, noticeId);
    }

    public void markRead(int studentId, int noticeId) throws DatabaseException {
        StudentDaoSupport.update("INSERT IGNORE INTO notice_reads (user_id, notice_id) "
                + "SELECT ?, n.notice_id FROM notices n WHERE n.notice_id = ?", studentId, noticeId);
    }
}
