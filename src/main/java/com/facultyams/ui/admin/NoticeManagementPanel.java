package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Notice;
import com.facultyams.service.NoticeService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/**
 * Admin screen: search, view, add, edit and delete notices. A notice is visible to its audience as soon as it is
 * saved (the database has no draft / unpublished state), so "Add" publishes it and "Delete" withdraws it.
 */
public class NoticeManagementPanel extends AbstractManagementPanel<Notice> {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Title", "Date", "Posted by", "Actions"};
    private static final int ACTIONS_COLUMN = 4;

    private final transient NoticeService noticeService;
    final JTextField searchField = searchField("Search notices...");

    public NoticeManagementPanel() {
        this(new NoticeService());
    }

    public NoticeManagementPanel(NoticeService noticeService) {
        super("notice", COLUMNS);
        this.noticeService = noticeService;
        liveSearch(searchField, this::refresh);
        setToolbar(filterRow(260, new double[0], searchField), addButton("Add Notice", this::addNotice));
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        RowActions.install(table, ACTIONS_COLUMN,
                new RowActions.Action(AdminIcons.Kind.EYE, new Color(0x475569), new Color(0xEEF2F7), "View",
                        this::viewRow),
                RowActions.edit(this::editRow),
                RowActions.delete(this::deleteRow));
    }

    @Override
    protected List<Notice> loadItems() throws DatabaseException, AuthorizationException {
        return noticeService.searchNotices(searchField.getText());
    }

    @Override
    protected Object[] toRow(Notice n) {
        return new Object[] {code('N', n.getNoticeId()), n.getTitle(), n.getNoticeDate(),
                n.getCreatedByName() == null ? "-" : n.getCreatedByName(), ""};
    }

    // ------------------------------------------------------------------ actions

    private void addNotice() {
        if (NoticeFormPanel.showDialog(this, noticeService, null)) {
            refresh();
            success("Notice published.");
        }
    }

    private void editRow(int row) {
        Notice notice = itemAt(row);
        if (notice != null && NoticeFormPanel.showDialog(this, noticeService, notice)) {
            refresh();
            success("Notice updated.");
        }
    }

    /** Read-only view of the full notice text. */
    private void viewRow(int row) {
        Notice notice = itemAt(row);
        if (notice == null) {
            return;
        }
        JTextArea text = new JTextArea(notice.getContent(), 10, 40);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(AdminTheme.FONT);
        text.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(460, 220));
        JOptionPane.showMessageDialog(this, new Object[] {notice.getTitle() + "  (" + notice.getNoticeDate() + ")", scroll},
                "Notice", JOptionPane.PLAIN_MESSAGE);
    }

    private void deleteRow(int row) {
        Notice notice = itemAt(row);
        if (notice == null || !UIUtil.confirm(this, "Delete notice \"" + notice.getTitle() + "\"?")) {
            return;
        }
        try {
            noticeService.deleteNotice(notice.getNoticeId());
            refresh();
            success("Notice deleted.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}