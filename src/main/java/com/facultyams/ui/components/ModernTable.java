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
        setRowHeight(44);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionBackground(Theme.PRIMARY_LIGHT);
        setSelectionForeground(Theme.TEXT_MAIN);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_MAIN);
        setBackground(Color.WHITE);
        setFillsViewportHeight(true);

        // Header style
        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setFont(Theme.FONT_SMALL);
        header.setForeground(Theme.TEXT_MUTED);
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Cell padding renderer
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                if (!isSelected) {
                    setBackground(Color.WHITE);
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
