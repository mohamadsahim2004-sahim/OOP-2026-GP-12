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
 *
 * Subclasses only describe WHAT to show (template method pattern):
 * columns, how to load records, how to turn a record into a table row,
 * and how to copy a record into / out of the form.
 *
 * @param <T> the model type shown in the table (User, Department, Course)
 */
public abstract class AbstractManagementPanel<T> extends JPanel {

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
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** Loads the records to show (usually honouring the current search filters). */
    protected abstract List<T> loadItems() throws DatabaseException, AuthorizationException;

    /** Converts one record into table cell values (same order as the columns). */
    protected abstract Object[] toRow(T item);

    /** Copies the selected record into the form fields. */
    protected abstract void fillForm(T item);

    /** Resets the form for entering a new record. */
    protected abstract void clearForm();

    /** Reloads combo-box lists (departments, lecturers...). Override if needed. */
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        // nothing by default
    }

    /** Reloads lookups and table data. Called when the tab is shown and after every change. */
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

    /** The record of the selected row, or null when nothing is selected. */
    protected T getSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < items.size() ? items.get(modelRow) : null;
    }

    /** Like getSelectedItem() but tells the user to select a row first. */
    protected T requireSelection(String what) {
        T item = getSelectedItem();
        if (item == null) {
            UIUtil.showInfo(this, "Please select a " + what + " in the table first.");
        }
        return item;
    }

    /** Adds "label: field" to a GridBagLayout form at (row, pair column). */
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

    /** Shows a text such as "All levels" for the null (no selection) entry of a combo box. */
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