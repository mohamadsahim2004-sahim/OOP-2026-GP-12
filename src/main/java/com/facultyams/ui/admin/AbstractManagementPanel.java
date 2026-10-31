package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Common layout and behaviour of the admin "manage X" screens: a toolbar (search, filters, Add button) above
 * a white card holding the table and a pagination bar. Loading, empty and error states are shown inside the
 * card. Subclasses supply the data ({@link #loadItems}), one table row per item ({@link #toRow}) and the
 * toolbar; Add / Edit forms open in dialogs.
 */
public abstract class AbstractManagementPanel<T> extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    /** Rows per page. */
    protected static final int PAGE_SIZE = 10;
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";

    protected final DefaultTableModel tableModel;
    protected final JTable table;
    protected final PaginationBar<T> pager;

    private final CardLayout bodyCards = new CardLayout();
    private final JPanel body = new JPanel(bodyCards);
    private final JPanel footer = new JPanel(new BorderLayout());
    private final JPanel toolbarSlot = new JPanel(new BorderLayout());
    private final JLabel emptyLabel = new JLabel(" ", JLabel.CENTER);
    private final JTextArea errorText = AdminTheme.errorText();
    private final JButton retryButton = new JButton("Retry");
    private final String noun;
    private final JLabel banner = new JLabel(" ");
    private final RoundedPanel bannerBox = new RoundedPanel(new BorderLayout(), 10);
    private final Timer bannerTimer = new Timer(4000, e -> bannerBox.setVisible(false));
    private String view = TABLE;

    /**
     * @param noun    singular name of the records, used in "Showing 1-6 of 6 users" and the empty message
     * @param columns table column titles (the last one is usually "Actions")
     */
    protected AbstractManagementPanel(String noun, String[] columns) {
        super(new BorderLayout(0, 14));
        this.noun = noun;
        setOpaque(false);

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;

            /** A cut-off cell ("Department of Inf...") shows its full text as a tooltip. */
            @Override
            public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int col = columnAtPoint(event.getPoint());
                if (row < 0 || col < 0) {
                    return super.getToolTipText(event);
                }
                Object value = getValueAt(row, col);
                String text = value == null ? "" : value.toString();
                if (text.isBlank()) {
                    return null;
                }
                Component cell = prepareRenderer(getCellRenderer(row, col), row, col);
                return cell.getPreferredSize().width > getCellRect(row, col, false).width ? text : null;
            }

            /** Fill the card while there is nearly enough room; below that scroll sideways instead of crushing columns. */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                if (!(getParent() instanceof javax.swing.JViewport vp)) {
                    return super.getScrollableTracksViewportWidth();
                }
                int wanted = 0;
                for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                    wanted += getColumnModel().getColumn(i).getPreferredWidth();
                }
                return vp.getWidth() >= wanted * 0.85;
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        AdminTheme.styleTable(table);
        for (int i = 0; i < columns.length; i++) {
            if ("ID".equals(columns[i])) {
                table.getColumnModel().getColumn(i).setMaxWidth(80);
                table.getColumnModel().getColumn(i).setPreferredWidth(60);
            }
        }

        pager = new PaginationBar<>(noun, PAGE_SIZE, this::renderPage);
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AdminTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        footer.add(pager, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        scroll.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        AdminTheme.slimScroll(scroll);
        body.setOpaque(false);
        body.add(scroll, TABLE);
        body.add(buildEmptyState(), EMPTY);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        card.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        banner.setFont(AdminTheme.FONT_BOLD);
        banner.setForeground(AdminTheme.SUCCESS);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bannerBox.colors(AdminTheme.SUCCESS_BG, new java.awt.Color(0xBBF7D0));
        bannerBox.add(banner, BorderLayout.CENTER);
        bannerBox.setVisible(false);
        bannerTimer.setRepeats(false);
        toolbarSlot.setOpaque(false);
        toolbarSlot.add(bannerBox, BorderLayout.SOUTH);
        add(toolbarSlot, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    private JPanel buildEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JLabel icon = new JLabel(AdminIcons.of(AdminIcons.Kind.INBOX, 40, new java.awt.Color(0xA7B4C8)));
        emptyLabel.setFont(AdminTheme.FONT_BOLD);
        emptyLabel.setForeground(AdminTheme.TEXT_MUTED);
        errorText.setName("management.error");
        errorText.setVisible(false);
        AdminTheme.primary(retryButton);
        retryButton.setVisible(false);
        retryButton.addActionListener(e -> refresh());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(4, 24, 4, 24);
        c.gridy = 0;
        empty.add(icon, c);
        c.gridy = 1;
        empty.add(emptyLabel, c);
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        empty.add(errorText, c);
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        empty.add(retryButton, c);
        return empty;
    }

    // ------------------------------------------------------------------ for subclasses

    protected abstract List<T> loadItems() throws DatabaseException, AuthorizationException;

    protected abstract Object[] toRow(T item);

    protected void loadLookups() throws DatabaseException, AuthorizationException {
        // nothing by default
    }

    /** Called after every successful or failed load with the full (unpaged) list. */
    protected void afterLoad(List<T> items) {
        // nothing by default
    }

    /** The toolbar above the card: filters on the left, the Add button on the right. */
    protected final void setToolbar(JComponent filters, JComponent action) {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        bar.add(filters, BorderLayout.CENTER);
        if (action != null) {
            JPanel right = new JPanel(new BorderLayout());
            right.setOpaque(false);
            right.add(action, BorderLayout.NORTH);
            bar.add(right, BorderLayout.EAST);
        }
        toolbarSlot.add(bar, BorderLayout.CENTER);
    }

    /** Lays out the filter controls in a responsive row that wraps on narrow windows. */
    protected static JPanel filterRow(int minCellWidth, double[] weights, JComponent... parts) {
        JPanel row = new JPanel(new ResponsiveGridLayout(minCellWidth, 10, weights));
        row.setOpaque(false);
        for (JComponent part : parts) {
            row.add(part);
        }
        return row;
    }

    /** Additional alternative bodies (for example a weekly timetable grid) that replace the table. */
    protected final void addView(String name, JComponent component) {
        body.add(component, name);
    }

    protected final void setView(String name) {
        view = name;
        render();
    }

    protected final String getView() {
        return view;
    }

    /** Shows a green confirmation ("User created.") above the table for a few seconds. */
    protected final void success(String message) {
        banner.setText(message);
        bannerBox.setVisible(true);
        bannerTimer.restart();
        revalidate();
    }

    protected static JButton addButton(String text, Runnable action) {
        JButton b = new JButton(text, AdminIcons.of(AdminIcons.Kind.PLUS, 16, java.awt.Color.WHITE));
        AdminTheme.primary(b);
        b.addActionListener(e -> action.run());
        return b;
    }

    protected static HintTextField searchField(String hint) {
        return new HintTextField(hint, 18).icon(AdminIcons.Kind.SEARCH);
    }

    /** Searches while typing (after a short pause) and immediately on Enter. */
    protected static void liveSearch(JTextField field, Runnable search) {
        Timer pause = new Timer(350, e -> search.run());
        pause.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                pause.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                pause.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                pause.restart();
            }
        });
        field.addActionListener(e -> {
            pause.stop();
            search.run();
        });
    }

    /** "U007" style id shown in the first column. */
    protected static String code(char prefix, int id) {
        return String.format("%c%03d", prefix, id);
    }

    /** The record shown in the given table row of the current page. */
    protected final T itemAt(int row) {
        return pager.get(row);
    }

    protected final T getSelectedItem() {
        return pager.get(table.getSelectedRow());
    }

    // ------------------------------------------------------------------ loading and rendering

    @Override
    public void refresh() {
        List<T> items;
        String error = null;
        try {
            loadLookups();
            items = loadItems();
        } catch (DatabaseException | AuthorizationException e) {
            items = new ArrayList<>();
            error = e.getMessage();
            if (e instanceof DatabaseException) {
                System.err.println("Database error: " + e.getMessage());
                if (e.getCause() != null) {
                    e.getCause().printStackTrace();
                }
            }
        }
        pager.setItems(items);
        errorText.setText(error == null ? "" : error);
        errorText.setVisible(error != null);
        retryButton.setVisible(error != null);
        emptyLabel.setText(error != null ? "Could not load " + noun + "s" : "No " + noun + "s found");
        renderPage();
        afterLoad(items);
    }

    /** Fills the table with the current page and shows the table, the alternative view or the empty state. */
    private void renderPage() {
        tableModel.setRowCount(0);
        for (T item : pager.pageItems()) {
            tableModel.addRow(toRow(item));
        }
        render();
    }

    private void render() {
        boolean empty = pager.total() == 0;
        bodyCards.show(body, empty ? EMPTY : view);
        footer.setVisible(!empty && TABLE.equals(view));
        revalidate();
        repaint();
    }

    protected static DefaultListCellRenderer nullRenderer(String nullText) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? nullText : value,
                        index, isSelected, cellHasFocus);
            }
        };
    }
}