package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Common layout and behaviour of the admin "manage X" screens:
 * a table of records, a search bar (NORTH) and an edit form (SOUTH).
 */
public abstract class AbstractManagementPanel<T> extends JPanel implements Refreshable {

    protected final DefaultTableModel tableModel;
    protected final JTable table;
    private List<T> items = new ArrayList<>();

    protected AbstractManagementPanel(String[] columns) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                T selected = getSelectedItem();
                if (selected != null) {
                    fillForm(selected);
                }
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
        
        for (int i = 0; i < columns.length; i++) {
            if ("ID".equals(columns[i])) {
                table.getColumnModel().getColumn(i).setMaxWidth(70);
                table.getColumnModel().getColumn(i).setPreferredWidth(50);
            }
        }
    }

    protected abstract List<T> loadItems() throws DatabaseException, AuthorizationException;
    protected abstract Object[] toRow(T item);
    protected abstract void fillForm(T item);
    protected abstract void clearForm();

    /** Hook called when a delete action is triggered (e.g. from a row action link). */
    protected void deleteSelected() {
        // overridden by subclasses
    }

    protected void loadLookups() throws DatabaseException, AuthorizationException {
        // nothing by default
    }

    @Override
    public void refresh() {
        try {
            loadLookups();
            items = loadItems();
        } catch (DatabaseException | AuthorizationException e) {
            items = new ArrayList<>();
            UIUtil.showException(this, e);
        }
        tableModel.setRowCount(0);
        for (T item : items) {
            tableModel.addRow(toRow(item));
        }
        clearForm();
    }

    protected T getSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < items.size() ? items.get(modelRow) : null;
    }

    protected T requireSelection(String what) {
        T item = getSelectedItem();
        if (item == null) {
            UIUtil.showInfo(this, "Please select a " + what + " in the table first.");
        }
        return item;
    }

    protected static void addField(JPanel form, int row, int pairColumn, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.gridy = row;
        c.gridx = pairColumn * 2;
        c.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel(label), c);
        c.gridx = pairColumn * 2 + 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(field, c);
    }

    protected static DefaultListCellRenderer nullRenderer(String nullText) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                    boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? nullText : value,
                        index, isSelected, cellHasFocus);
            }
        };
    }
}