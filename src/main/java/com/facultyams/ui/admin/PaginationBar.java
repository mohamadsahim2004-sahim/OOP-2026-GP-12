package com.facultyams.ui.admin;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * "Showing 1-6 of 6 users" plus previous / page-number / next buttons, as in the reference tables.
 * Holds the full result list and hands out one page of it; {@code onChange} is called after the page changes.
 */
final class PaginationBar<T> extends JPanel {

    private static final long serialVersionUID = 1L;
    private final String noun;
    private final int pageSize;
    private final transient Runnable onChange;
    private final JLabel info = new JLabel();
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private transient List<T> all = List.of();
    private int page;

    PaginationBar(String noun, int pageSize, Runnable onChange) {
        super(new BorderLayout(12, 0));
        this.noun = noun;
        this.pageSize = pageSize;
        this.onChange = onChange;
        setOpaque(false);
        info.setFont(AdminTheme.FONT_SMALL);
        info.setForeground(AdminTheme.TEXT_MUTED);
        buttons.setOpaque(false);
        add(info, BorderLayout.WEST);
        add(buttons, BorderLayout.EAST);
        rebuild();
    }

    /** Replaces the full list; stays on the current page when it still exists. */
    void setItems(List<T> items) {
        all = items == null ? List.of() : items;
        page = Math.min(page, Math.max(0, pageCount() - 1));
        rebuild();
    }

    int total() {
        return all.size();
    }

    List<T> pageItems() {
        int from = page * pageSize;
        return all.subList(Math.min(from, all.size()), Math.min(from + pageSize, all.size()));
    }

    /** The item shown in the given row of the current page, or null. */
    T get(int rowInPage) {
        List<T> items = pageItems();
        return rowInPage >= 0 && rowInPage < items.size() ? items.get(rowInPage) : null;
    }

    private int pageCount() {
        return Math.max(1, (all.size() + pageSize - 1) / pageSize);
    }

    private void go(int target) {
        page = Math.max(0, Math.min(target, pageCount() - 1));
        rebuild();
        onChange.run();
    }

    private void rebuild() {
        int n = all.size();
        if (n == 0) {
            info.setText("No " + noun + "s found");
        } else {
            int from = page * pageSize + 1;
            int to = Math.min(n, from + pageSize - 1);
            info.setText("Showing " + from + "-" + to + " of " + n + " " + noun + (n == 1 ? "" : "s"));
        }
        buttons.removeAll();
        buttons.add(arrow(AdminIcons.Kind.CHEVRON_LEFT, "Previous page", page > 0, page - 1));
        int pages = pageCount();
        int first = Math.max(0, Math.min(page - 2, pages - 5));
        for (int p = first; p < Math.min(pages, first + 5); p++) {
            buttons.add(number(p));
        }
        buttons.add(arrow(AdminIcons.Kind.CHEVRON_RIGHT, "Next page", page < pages - 1, page + 1));
        buttons.revalidate();
        buttons.repaint();
    }

    private JButton number(int p) {
        JButton b = new JButton(String.valueOf(p + 1));
        if (p == page) {
            AdminTheme.primary(b);
        } else {
            AdminTheme.outline(b);
        }
        size(b);
        b.getAccessibleContext().setAccessibleName("Page " + (p + 1));
        b.addActionListener(e -> go(p));
        return b;
    }

    private JButton arrow(AdminIcons.Kind kind, String name, boolean enabled, int target) {
        JButton b = new JButton(AdminIcons.of(kind, 14, enabled ? AdminTheme.TEXT : AdminTheme.TEXT_MUTED));
        AdminTheme.outline(b);
        size(b);
        b.setEnabled(enabled);
        b.setToolTipText(name);
        b.getAccessibleContext().setAccessibleName(name);
        b.addActionListener(e -> go(target));
        return b;
    }

    private static void size(JButton b) {
        b.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(32, 32));
    }
}