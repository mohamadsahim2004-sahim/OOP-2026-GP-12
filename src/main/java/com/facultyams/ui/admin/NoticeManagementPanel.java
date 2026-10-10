package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Notice;
import com.facultyams.service.NoticeService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Admin screen: create / view / update / delete / search notices. */
public class NoticeManagementPanel extends AbstractManagementPanel<Notice> {

    private static final int PREVIEW_LENGTH = 60;

    private final NoticeService noticeService = new NoticeService();

    private final JTextField searchField = new JTextField(25);
    private final JTextField titleField = new JTextField(30);
    private final JTextField dateField = new JTextField(10);
    private final JTextArea contentArea = new JTextArea(4, 40);

    public NoticeManagementPanel() {
        super(new String[]{"ID", "Title", "Content", "Date", "Posted by"});
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.SOUTH);
        AdminTheme.apply(this);
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");
        bar.add(new JLabel("Search title / content:"));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(showAllButton);
        searchButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Title:", titleField);
        addField(form, 0, 1, "Date (yyyy-MM-dd):", dateField);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new java.awt.Insets(3, 5, 3, 5);
        c.gridy = 1;
        c.gridx = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        form.add(new JLabel("Content:"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(new JScrollPane(contentArea), c);

        JButton addButton = new JButton("Add Notice");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        addButton.addActionListener(e -> addNotice());
        updateButton.addActionListener(e -> updateNotice());
        deleteButton.addActionListener(e -> deleteNotice());
        clearButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createTitledBorder("Notice details"));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    @Override
    protected List<Notice> loadItems() throws DatabaseException, AuthorizationException {
        return noticeService.searchNotices(searchField.getText());
    }

    @Override
    protected Object[] toRow(Notice n) {
        String content = n.getContent().replace('\n', ' ');
        if (content.length() > PREVIEW_LENGTH) {
            content = content.substring(0, PREVIEW_LENGTH) + "...";
        }
        return new Object[]{n.getNoticeId(), n.getTitle(), content, n.getNoticeDate(),
                n.getCreatedByName() == null ? "-" : n.getCreatedByName()};
    }

    @Override
    protected void fillForm(Notice n) {
        titleField.setText(n.getTitle());
        dateField.setText(n.getNoticeDate().toString());
        contentArea.setText(n.getContent());
        contentArea.setCaretPosition(0);
    }

    @Override
    protected void clearForm() {
        titleField.setText("");
        dateField.setText(LocalDate.now().toString());
        contentArea.setText("");
    }

    /** Copies the form into a Notice. Throws ValidationException for an unreadable date. */
    private Notice readForm() throws ValidationException {
        Notice n = new Notice();
        n.setTitle(titleField.getText());
        n.setContent(contentArea.getText());
        try {
            n.setNoticeDate(LocalDate.parse(dateField.getText().trim()));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must be written as yyyy-MM-dd, e.g. 2026-09-20.", e);
        }
        return n;
    }

    private void addNotice() {
        try {
            Notice n = noticeService.addNotice(readForm());
            UIUtil.showInfo(this, "Notice '" + n.getTitle() + "' published.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void updateNotice() {
        Notice selected = requireSelection("notice");
        if (selected == null) {
            return;
        }
        try {
            Notice n = readForm();
            n.setNoticeId(selected.getNoticeId());
            noticeService.updateNotice(n);
            UIUtil.showInfo(this, "Notice updated.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    @Override
    protected void deleteSelected() {
        deleteNotice();
    }

    private void deleteNotice() {
        Notice selected = requireSelection("notice");
        if (selected == null || !UIUtil.confirm(this, "Delete notice \"" + selected.getTitle() + "\"?")) {
            return;
        }
        try {
            noticeService.deleteNotice(selected.getNoticeId());
            UIUtil.showInfo(this, "Notice deleted.");
            refresh();
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }
}