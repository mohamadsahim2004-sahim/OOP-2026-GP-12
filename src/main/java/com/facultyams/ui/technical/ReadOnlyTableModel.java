package com.facultyams.ui.technical;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Table model whose cells cannot be edited (except the columns named), with real column types so sorting is numeric. */
final class ReadOnlyTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;
    private final transient Class<?>[] types;
    private final Set<Integer> editable;

    ReadOnlyTableModel(String[] columns, Class<?>[] types, int... editableColumns) {
        super(columns, 0);
        this.types = types;
        this.editable = IntStream.of(editableColumns).boxed().collect(Collectors.toSet());
    }

    static ReadOnlyTableModel ofStrings(String... columns) {
        Class<?>[] types = new Class<?>[columns.length];
        java.util.Arrays.fill(types, String.class);
        return new ReadOnlyTableModel(columns, types);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editable.contains(column);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return types[column];
    }

    void setRows(List<Object[]> rows) {
        setRowCount(0);
        for (Object[] r : rows) {
            addRow(r);
        }
    }
}