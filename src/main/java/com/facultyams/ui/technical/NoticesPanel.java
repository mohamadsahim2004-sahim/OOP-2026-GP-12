package com.facultyams.ui.technical;

import com.facultyams.dao.StudentNoticeDAO;
import com.facultyams.model.student.StudentNotice;
import com.facultyams.security.Session;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class NoticesPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private final ReadOnlyTableModel model =
            ReadOnlyTableModel.ofStrings("Date", "Title", "Category", "Publisher");
    private final JTable table = table(model);
    private transient List<StudentNotice> notices = List.of();

    public NoticesPanel() {
        add(statusArea(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetails();
                }
            }
        });
    }

    @Override
    public void refresh() {
        int myId = Session.getCurrentUser().getUserId();
        load("Loading notices...", () -> new StudentNoticeDAO().findAll(myId), list -> {
            notices = list;
            List<Object[]> data = new ArrayList<>();
            for (StudentNotice n : list) {
                data.add(new Object[] {n.getDate(), n.getTitle(), n.getCategory(), n.getPublisher()});
            }
            model.setRows(data);
            showInfo(list.isEmpty() ? "No notices to show." : list.size() + " notices.");
        });
    }

    private void showDetails() {
        int row = selectedModelRow(table);
        if (row < 0) {
            showError("Select a notice from the table first.");
            return;
        }
        StudentNotice n = notices.get(row);
        String text = n.getTitle() + "\n" + n.getDate() + "  |  " + n.getCategory()
                + "  |  " + n.getPublisher() + "\n\n" + n.getContent();
        JOptionPane.showMessageDialog(this, text, "Notice", JOptionPane.PLAIN_MESSAGE);
    }
}