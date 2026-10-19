package com.srms.ui.components;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsive grid layout that reflows 3, 2, or 1 columns depending on the
 * available container width. Avoids fixed card widths so cards smoothly resize.
 */
public class ResponsiveGridLayout implements LayoutManager {
    private final int hgap;
    private final int vgap;
    private final int cardHeight;
    private final List<Component> components = new ArrayList<>();

    public ResponsiveGridLayout(int hgap, int vgap, int cardHeight) {
        this.hgap = hgap;
        this.vgap = vgap;
        this.cardHeight = cardHeight;
    }

    public ResponsiveGridLayout() {
        this(20, 20, 275);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        if (!components.contains(comp)) {
            components.add(comp);
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        components.remove(comp);
    }

    private int computeCols(int width) {
        if (width >= 960) return 3;
        if (width >= 620) return 2;
        return 1;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth();
            if (width <= 0) width = 1000;
            int usableWidth = width - (insets.left + insets.right);
            int cols = Math.max(1, computeCols(usableWidth));

            int count = 0;
            for (Component c : components) {
                if (c.isVisible()) count++;
            }
            if (count == 0) {
                return new Dimension(usableWidth, insets.top + insets.bottom);
            }

            int rows = (count + cols - 1) / cols;
            int height = insets.top + insets.bottom + (rows * cardHeight) + ((rows - 1) * vgap);
            return new Dimension(width, height);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth();
            int usableWidth = width - (insets.left + insets.right);
            int cols = Math.max(1, computeCols(usableWidth));

            List<Component> visible = new ArrayList<>();
            for (Component c : components) {
                if (c.isVisible()) visible.add(c);
            }
            if (visible.isEmpty()) return;

            int colWidth = (usableWidth - (cols - 1) * hgap) / cols;

            for (int i = 0; i < visible.size(); i++) {
                int r = i / cols;
                int c = i % cols;

                int x = insets.left + c * (colWidth + hgap);
                int y = insets.top + r * (cardHeight + vgap);

                visible.get(i).setBounds(x, y, colWidth, cardHeight);
            }
        }
    }
}
