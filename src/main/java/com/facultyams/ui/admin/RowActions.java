package com.facultyams.ui.admin;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.table.TableCellRenderer;

/**
 * The "Actions" column of the management tables: small icon buttons (blue pencil, red bin ...) like the
 * reference design. One shared implementation so every screen looks and behaves the same.
 *
 * <p>Keyboard: with a row selected, Enter runs the first action (edit) and Delete runs the delete action, so
 * the table can be operated without a mouse. The table must not use a row sorter: the view row is the
 * index into the page that is shown.
 */
final class RowActions {

    private static final int BUTTON = 28;
    private static final int GAP = 6;
    private static final int LEFT = 14;

    /** One icon button in the column. */
    record Action(AdminIcons.Kind icon, Color color, Color background, String tooltip, IntConsumer handler) {
    }

    static Action edit(IntConsumer handler) {
        return new Action(AdminIcons.Kind.EDIT, AdminTheme.BLUE, AdminTheme.BLUE_LIGHT, "Edit", handler);
    }

    static Action delete(IntConsumer handler) {
        return new Action(AdminIcons.Kind.TRASH, AdminTheme.DANGER, AdminTheme.DANGER_BG, "Delete", handler);
    }

    private RowActions() {
    }

    /** Edit and Delete only (departments, courses, notices ...). */
    static void install(JTable table, int column, IntConsumer onEdit, IntConsumer onDelete) {
        install(table, column, edit(onEdit), delete(onDelete));
    }

    static void install(JTable table, int column, Action... actions) {
        table.getColumnModel().getColumn(column).setCellRenderer(new Renderer(actions));
        table.getColumnModel().getColumn(column).setPreferredWidth(LEFT + actions.length * (BUTTON + GAP) + 6);
        table.getColumnModel().getColumn(column).setMinWidth(LEFT + actions.length * (BUTTON + GAP));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = actionAt(table, e.getPoint(), column, actions.length);
                int row = table.rowAtPoint(e.getPoint());
                if (index >= 0 && row >= 0) {
                    table.setRowSelectionInterval(row, row);
                    actions[index].handler().accept(row);
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = actionAt(table, e.getPoint(), column, actions.length);
                table.setCursor(index >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                table.setToolTipText(index >= 0 ? actions[index].tooltip() : null);
            }
        });
        bindKey(table, KeyEvent.VK_ENTER, "rowEdit", actions[0]);
        for (Action a : actions) {
            if (a.icon() == AdminIcons.Kind.TRASH) {
                bindKey(table, KeyEvent.VK_DELETE, "rowDelete", a);
            }
        }
    }

    private static void bindKey(JTable table, int key, String name, Action action) {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        table.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    action.handler().accept(row);
                }
            }
        });
    }

    /** Index of the icon under the point, or -1. Uses the same geometry the renderer paints with. */
    static int actionAt(JTable table, Point point, int column, int count) {
        int row = table.rowAtPoint(point);
        if (row < 0 || table.columnAtPoint(point) != column) {
            return -1;
        }
        Rectangle cell = table.getCellRect(row, column, false);
        int x = cell.x + LEFT;
        for (int i = 0; i < count; i++) {
            if (point.x >= x && point.x <= x + BUTTON) {
                return i;
            }
            x += BUTTON + GAP;
        }
        return -1;
    }

    private static final class Renderer extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final transient Action[] actions;
        private boolean selected;
        private transient JTable owner;

        Renderer(Action[] actions) {
            this.actions = actions;
        }

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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? owner.getSelectionBackground() : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int x = LEFT;
            int y = (getHeight() - BUTTON) / 2;
            for (Action a : actions) {
                g2.setColor(a.background());
                g2.fillRoundRect(x, y, BUTTON, BUTTON, 8, 8);
                AdminIcons.of(a.icon(), 16, a.color()).paintIcon(this, g2, x + 6, y + 6);
                x += BUTTON + GAP;
            }
            g2.dispose();
        }
    }
}