package com.facultyams.ui.admin;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A grid that picks its column count from the available width: as many columns as fit with at least
 * {@code minCellWidth} each (at most one per child). Four stat cards become two rows of two on a tablet
 * and a single column on a phone-sized window. Optional {@code weights} make columns unequal when every
 * child sits in its own column (for example 3:2 for the dashboard's lower row).
 */
public final class ResponsiveGridLayout implements LayoutManager {

    private final int minCellWidth;
    private final int gap;
    private final double[] weights;
    private int lastHeight = -1;

    public ResponsiveGridLayout(int minCellWidth, int gap, double... weights) {
        this.minCellWidth = minCellWidth;
        this.gap = gap;
        this.weights = weights;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // not used
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // not used
    }

    private int columns(Container parent, int width, int n) {
        return Math.max(1, Math.min(n, (width + gap) / (minCellWidth + gap)));
    }

    private int visibleCount(Container parent) {
        int n = 0;
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                n++;
            }
        }
        return n;
    }

    private int rowHeight(Container parent, int firstIndex, int cols, java.util.List<Component> kids) {
        int h = 0;
        for (int i = firstIndex; i < Math.min(kids.size(), firstIndex + cols); i++) {
            h = Math.max(h, kids.get(i).getPreferredSize().height);
        }
        return h;
    }

    private java.util.List<Component> kids(Container parent) {
        java.util.List<Component> list = new java.util.ArrayList<>();
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        java.util.List<Component> kids = kids(parent);
        Insets in = parent.getInsets();
        int n = kids.size();
        if (n == 0) {
            return new Dimension(in.left + in.right, in.top + in.bottom);
        }
        int inner = parent.getWidth() - in.left - in.right;
        int cols = inner <= 0 ? n : columns(parent, inner, n);
        int height = 0;
        int rows = 0;
        for (int i = 0; i < n; i += cols) {
            height += rowHeight(parent, i, cols, kids);
            rows++;
        }
        height += (rows - 1) * gap;
        return new Dimension(in.left + in.right + minCellWidth, in.top + in.bottom + height);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        return new Dimension(in.left + in.right + minCellWidth, in.top + in.bottom);
    }

    @Override
    public void layoutContainer(Container parent) {
        java.util.List<Component> kids = kids(parent);
        int n = kids.size();
        if (n == 0) {
            return;
        }
        Insets in = parent.getInsets();
        int width = parent.getWidth() - in.left - in.right;
        int cols = columns(parent, width, n);
        double[] w = new double[cols];
        double sum = 0;
        for (int i = 0; i < cols; i++) {
            w[i] = weights.length == n && cols == n ? weights[i] : 1;
            sum += w[i];
        }
        double usable = width - (cols - 1) * gap;
        // column edges are rounded cumulatively so the last column ends exactly at the container's right edge
        int[] left = new int[cols + 1];
        double acc = 0;
        for (int i = 0; i < cols; i++) {
            left[i] = in.left + (int) Math.round(acc * usable / sum) + i * gap;
            acc += w[i];
        }
        left[cols] = in.left + (int) Math.round(usable) + (cols - 1) * gap + gap;
        int y = in.top;
        for (int row = 0; row * cols < n; row++) {
            int rh = rowHeight(parent, row * cols, cols, kids);
            for (int col = 0; col < cols && row * cols + col < n; col++) {
                kids.get(row * cols + col).setBounds(left[col], y, left[col + 1] - gap - left[col], rh);
            }
            y += rh + gap;
        }
        int needed = preferredLayoutSize(parent).height;
        if (needed != lastHeight) {
            lastHeight = needed;
            SwingUtilities.invokeLater(parent::revalidate); // the wrapped height changed: let the parent re-measure
        }
    }
}