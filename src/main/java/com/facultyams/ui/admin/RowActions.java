package com.facultyams.ui.admin;

import javax.swing.JComponent;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.table.TableCellRenderer;

/**
 * The "Actions" column of the management tables: clickable Edit and Delete links in the last column.
 * One shared implementation for the screens so they look and behave the same. The table must not use a
 * row sorter (the view row is the index into the screen's list).
 */
final class RowActions {

    static final int EDIT = 0;
    static final int DELETE = 1;
    private static final String[] LABELS = {"Edit", "Delete"};
    private static final int LEFT = 8;
    private static final int GAP = 18;

    private RowActions() {
    }

    private static Color color(int action) {
        return action == EDIT ? AdminTheme.BLUE : AdminTheme.DANGER;
    }

    /** Paints the links in {@code column} and calls onEdit / onDelete with the clicked row. */
    static void install(JTable table, int column, IntConsumer onEdit, IntConsumer onDelete) {
        table.getColumnModel().getColumn(column).setCellRenderer(new Renderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int action = actionAt(table, e.getPoint(), column);
                if (action < 0) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (action == EDIT) {
                    onEdit.accept(row);
                } else {
                    onDelete.accept(row);
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                table.setCursor(actionAt(table, e.getPoint(), column) >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    /** EDIT, DELETE, or -1 when the point is not on a link. Uses the same geometry the renderer paints with. */
    static int actionAt(JTable table, Point point, int column) {
        int row = table.rowAtPoint(point);
        if (row < 0 || table.columnAtPoint(point) != column) {
            return -1;
        }
        Rectangle cell = table.getCellRect(row, column, false);
        FontMetrics fm = table.getFontMetrics(AdminTheme.FONT_BOLD);
        int x = cell.x + LEFT;
        for (int action = 0; action < LABELS.length; action++) {
            int width = fm.stringWidth(LABELS[action]);
            if (point.x >= x && point.x <= x + width) {
                return action;
            }
            x += width + GAP;
        }
        return -1;
    }

    private static final class Renderer extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private boolean selected;
        private transient JTable owner;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            this.selected = isSelected;
            this.owner = t;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(selected ? owner.getSelectionBackground() : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(AdminTheme.FONT_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int x = LEFT;
            for (int action = 0; action < LABELS.length; action++) {
                g2.setColor(color(action));
                g2.drawString(LABELS[action], x, baseline);
                g2.drawLine(x, baseline + 2, x + fm.stringWidth(LABELS[action]), baseline + 2);
                x += fm.stringWidth(LABELS[action]) + GAP;
            }
            g2.dispose();
        }
    }
}