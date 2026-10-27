package com.facultyams.ui.technical;

import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.Display;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/** Shows a Double column as a percentage with one decimal; the column still sorts numerically. */
final class PercentRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    PercentRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT);
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value instanceof Double d ? Display.percent(d) : value,
                selected, focus, row, column);
        if (!selected) {
            setForeground(value instanceof Double d && d < 80 ? AdminTheme.DANGER : AdminTheme.TEXT);
        }
        return this;
    }
}