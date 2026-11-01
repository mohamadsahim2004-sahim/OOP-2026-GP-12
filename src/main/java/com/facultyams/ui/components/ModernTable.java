package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class ModernTable extends JTable {

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setRowHeight(40);
        setShowGrid(false);
        setShowHorizontalLines(true);
        setGridColor(new Color(241, 245, 249));
        setSelectionBackground(new Color(239, 246, 255));
        setSelectionForeground(Theme.TEXT_MAIN);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_MAIN);
        setBackground(Color.WHITE);
        setFillsViewportHeight(true);

        // Header style
        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(0, 36));
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setForeground(Theme.TEXT_MUTED);
        header.setBackground(new Color(248, 250, 252));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Cell padding renderer
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 255));
                }
                return c;
            }
        });
    }

    public static JScrollPane createModernScrollPane(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBackground(Color.WHITE);
        return scroll;
    }
}
