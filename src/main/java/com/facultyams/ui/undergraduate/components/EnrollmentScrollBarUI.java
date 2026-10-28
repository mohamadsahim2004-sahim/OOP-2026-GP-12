package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Thin enrollment-page scrollbar without arrow buttons. */
public final class EnrollmentScrollBarUI extends BasicScrollBarUI {
    @Override protected void configureScrollBarColors() {
        thumbColor = new Color(209, 213, 219);
        trackColor = new Color(0, 0, 0, 0);
    }

    @Override public Dimension getPreferredSize(JComponent component) {
        return new Dimension(8, 8);
    }

    @Override protected JButton createDecreaseButton(int orientation) { return emptyButton(); }
    @Override protected JButton createIncreaseButton(int orientation) { return emptyButton(); }

    private JButton emptyButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setVisible(false);
        return button;
    }

    @Override protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {}

    @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.setColor(thumbColor);
        g.fillRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 8, 8);
        g.dispose();
    }
}
