package com.facultyams.ui.technical;

import com.facultyams.ui.admin.AdminTheme;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Locale;

/** Shows a status word in bold colour (the word itself is always shown, so colour is never the only cue). */
final class StatusRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;
    private static final Color AMBER = new Color(0xB45F06);

    StatusRenderer() {
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, focus, row, column);
        String s = value == null ? "" : value.toString().toLowerCase(Locale.ROOT);
        Color color = switch (s) {
            case "present", "approved", "yes" -> AdminTheme.SUCCESS;
            case "absent", "rejected", "no" -> AdminTheme.DANGER;
            case "late", "pending" -> AMBER;
            default -> AdminTheme.TEXT;
        };
        setForeground(selected ? AdminTheme.NAVY : color);
        setFont(getFont().deriveFont(color == AdminTheme.TEXT ? Font.PLAIN : Font.BOLD));
        return this;
    }
}