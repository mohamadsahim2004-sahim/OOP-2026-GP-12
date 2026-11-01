package com.facultyams.ui.admin;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * A panel for use inside a JScrollPane that is as wide as the viewport (so content can reflow on small
 * windows) until the viewport gets narrower than {@code minWidth}, when a horizontal scroll bar appears.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;
    private final int minWidth;

    public ScrollablePanel(LayoutManager layout, int minWidth) {
        super(layout);
        this.minWidth = minWidth;
        setOpaque(false);
    }

    /** Height depends on the width (wrapping grids), so lay out again once the real width is known. */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean widthChanged = width != getWidth();
        super.setBounds(x, y, width, height);
        if (widthChanged) {
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(40, visible.height - 40);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return !(getParent() instanceof JViewport vp) || vp.getWidth() >= minWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, minWidth), d.height);
    }
}