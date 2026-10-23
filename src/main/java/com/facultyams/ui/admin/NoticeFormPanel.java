package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Notice;
import com.facultyams.service.NoticeService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Add / Edit notice form, shown in a dialog. {@link NoticeService} validates title, content and date. */
final class NoticeFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient NoticeService noticeService;
    /** null when adding. */
    private final transient Notice existing;

    final JTextField titleField = new JTextField(24);
    final JTextField dateField = new JTextField(10);
    final JTextArea contentArea = new JTextArea(7, 30);

    NoticeFormPanel(NoticeService noticeService, Notice existing) {
        super(new BorderLayout());
        this.noticeService = noticeService;
        this.existing = existing;
        setOpaque(false);

        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(AdminTheme.FONT);
        contentArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(contentArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xC9D4E5)));
        scroll.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);

        JPanel form = Forms.grid();
        Forms.add(form, 0, 0, 1, "Title *", titleField);
        Forms.add(form, 1, 0, 1, "Date (yyyy-MM-dd) *", dateField);
        Forms.add(form, 0, 1, 2, "Content *", scroll);
        contentArea.getAccessibleContext().setAccessibleName("Content");
        add(form, BorderLayout.CENTER);

        if (existing == null) {
            dateField.setText(LocalDate.now().toString());
        } else {
            titleField.setText(existing.getTitle());
            dateField.setText(existing.getNoticeDate().toString());
            contentArea.setText(existing.getContent());
            contentArea.setCaretPosition(0);
        }
    }

    /** Saves through the service; returns false (after showing the message) if it was refused. */
    boolean save() {
        try {
            Notice n = new Notice();
            n.setTitle(titleField.getText());
            n.setContent(contentArea.getText());
            try {
                n.setNoticeDate(LocalDate.parse(dateField.getText().trim()));
            } catch (DateTimeParseException e) {
                throw new ValidationException("Date must be written as yyyy-MM-dd, e.g. 2026-09-20.", e);
            }
            if (existing == null) {
                noticeService.addNotice(n);
            } else {
                n.setNoticeId(existing.getNoticeId());
                noticeService.updateNotice(n);
            }
            return true;
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
            return false;
        }
    }

    static boolean showDialog(Component parent, NoticeService noticeService, Notice existing) {
        NoticeFormPanel form = new NoticeFormPanel(noticeService, existing);
        return FormDialog.show(parent, existing == null ? "Add Notice" : "Edit Notice", form,
                existing == null ? "Publish notice" : "Save changes", form::save);
    }
}