package com.srms.ui.components;

import com.srms.Theme;
import com.srms.model.ViewMode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.*;

/** Two-button grid/list selector painted as a single segmented control. */
public class SegmentedToggle extends JPanel {
    private ViewMode selected = ViewMode.GRID;
    private final JButton grid = new IconButton(Icons.Type.GRID);
    private final JButton list = new IconButton(Icons.Type.LIST);
    private Consumer<ViewMode> listener;

    public SegmentedToggle() {
        setOpaque(false);
        setLayout(new GridLayout(1, 2));
        setPreferredSize(new Dimension(80, Theme.CONTROL_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setBackground(Color.WHITE);
        add(grid);
        add(list);
        grid.addActionListener(e -> select(ViewMode.GRID));
        list.addActionListener(e -> select(ViewMode.LIST));
        updateButtons();
    }

    public void setViewModeListener(Consumer<ViewMode> listener) {
        this.listener = listener;
    }

    public ViewMode getSelectedMode() {
        return selected;
    }

    public void setSelectedMode(ViewMode mode) {
        select(mode);
    }

    private void select(ViewMode mode) {
        if (selected == mode) return;
        selected = mode;
        updateButtons();
        if (listener != null) listener.accept(mode);
    }

    private void updateButtons() {
        styleButton(grid, selected == ViewMode.GRID);
        styleButton(list, selected == ViewMode.LIST);
    }

    private void styleButton(JButton button, boolean active) {
        button.setBackground(active ? Theme.PRIMARY : Color.WHITE);
        button.setForeground(active ? Color.WHITE : Theme.TEXT_MUTED);
        button.setOpaque(true);
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL));
        super.paintChildren(g);
        g.setColor(Theme.CARD_BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
        g.dispose();
    }

    private final class IconButton extends JButton {
        private final Icons.Type icon;
        private boolean hovered;

        IconButton(Icons.Type icon) {
            this.icon = icon;
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean active = (icon == Icons.Type.GRID && selected == ViewMode.GRID)
                    || (icon == Icons.Type.LIST && selected == ViewMode.LIST);
            g.setColor(active ? Theme.PRIMARY : hovered ? Theme.PRIMARY_LIGHT : Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            Icons.paint(g, icon, (getWidth() - 18) / 2, (getHeight() - 18) / 2, 18,
                    active ? Color.WHITE : Theme.TEXT_MUTED);
            g.dispose();
        }
    }
}
