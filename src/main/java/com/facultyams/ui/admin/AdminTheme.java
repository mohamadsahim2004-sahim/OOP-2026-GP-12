package com.facultyams.ui.admin;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Colours, fonts and small styling helpers shared by every Admin screen so the
 * whole module has the navy-blue / white look of the reference design.
 */
public final class AdminTheme {

    public static final Color NAVY = new Color(0x0B2A66);
    public static final Color NAVY_DARK = new Color(0x08204F);
    public static final Color NAVY_ACTIVE = new Color(0x1E5FD8);
    public static final Color HEADER_BLUE = new Color(0x0D3B8E);
    public static final Color BLUE = new Color(0x1D63D8);
    public static final Color BLUE_LIGHT = new Color(0xE8F0FC);
    public static final Color PAGE_BG = new Color(0xFFFFFF);
    public static final Color PANEL_BG = new Color(0xF4F7FC);
    public static final Color BORDER = new Color(0xD5DDEB);
    public static final Color TEXT = new Color(0x1B2A41);
    public static final Color TEXT_MUTED = new Color(0x5F6F89);
    public static final Color DANGER = new Color(0xD93025);
    public static final Color SUCCESS = new Color(0x1E9E4A);
    public static final Color SUCCESS_BG = new Color(0xDDF3E4);

    public static final Color CARD_BLUE = new Color(0x1E6FE0);
    public static final Color CARD_GREEN = new Color(0x1F9D4D);
    public static final Color CARD_PURPLE = new Color(0x7B4BC4);
    public static final Color CARD_ORANGE = new Color(0xF59A1B);

    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SECTION = new Font("SansSerif", Font.BOLD, 16);

    /** Set this client property to TRUE on a component that styles itself; apply() then leaves it alone. */
    public static final String SKIP = "adminTheme.skip";

    private AdminTheme() {
        // utility class
    }

    /** Blue call-to-action button (Add, Search, Update ...). */
    public static void primary(AbstractButton b) {
        style(b, BLUE, Color.WHITE);
    }

    /** Red button for destructive actions (Delete). */
    public static void danger(AbstractButton b) {
        style(b, DANGER, Color.WHITE);
    }

    /** Light button for secondary actions (Clear, Show all). */
    public static void secondary(AbstractButton b) {
        style(b, BLUE_LIGHT, NAVY);
    }

    private static void style(AbstractButton b, Color background, Color foreground) {
        b.setBackground(background);
        b.setForeground(foreground);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    /** Gives a table the reference look: navy header, light rows, no heavy grid. */
    public static void styleTable(JTable table) {
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setSelectionBackground(BLUE_LIGHT);
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(NAVY);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(FONT_BOLD);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        header.setDefaultRenderer(headerRenderer);
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 32));
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer();
        cell.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(Integer.class, cell);
    }

    /**
     * Walks a finished screen and applies the theme to plain Swing controls, so each
     * screen only has to build its layout. Buttons named Delete are red, Clear /
     * Show all are light, every other button is blue.
     */
    public static void apply(Container root) {
        if (root instanceof JPanel rootPanel) {
            rootPanel.setOpaque(true);
            rootPanel.setBackground(PAGE_BG);
        }
        applyToChildren(root);
    }

    private static void applyToChildren(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof javax.swing.JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(SKIP))) {
                continue;
            }
            if (c instanceof JButton button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase();
                if (text.startsWith("delete") || text.startsWith("remove")) {
                    danger(button);
                } else if (text.startsWith("clear") || text.startsWith("show all") || text.startsWith("reset")) {
                    secondary(button);
                } else {
                    primary(button);
                }
            } else if (c instanceof JTable table) {
                styleTable(table);
            } else if (c instanceof JTextField || c instanceof JComboBox<?> || c instanceof JTextArea) {
                c.setFont(FONT);
            } else if (c instanceof JLabel label && label.getFont().getStyle() == Font.PLAIN) {
                label.setFont(FONT);
                label.setForeground(TEXT);
            } else if (c instanceof JScrollPane scroll) {
                scroll.setBorder(BorderFactory.createLineBorder(BORDER));
                scroll.getViewport().setBackground(Color.WHITE);
            }
            if (c instanceof JPanel panel) {
                if (panel.getBorder() instanceof TitledBorder tb) {
                    tb.setTitleColor(NAVY);
                    tb.setTitleFont(FONT_BOLD);
                }
                panel.setOpaque(true);
                panel.setBackground(PAGE_BG);
            }
            if (c instanceof Container child) {
                applyToChildren(child);
            }
        }
    }
}