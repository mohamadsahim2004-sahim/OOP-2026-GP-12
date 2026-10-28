package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Thin rounded scrollbar with a transparent track. */
public final class ThinScrollBarUI extends BasicScrollBarUI {
    private boolean thumbVisible;
    private boolean pointerInside;
    private Timer hideTimer;

    @Override protected void configureScrollBarColors() {
        thumbColor = new Color(148, 163, 184, 150);
        thumbDarkShadowColor = thumbColor;
        thumbHighlightColor = thumbColor;
        thumbLightShadowColor = thumbColor;
        trackColor = new Color(0, 0, 0, 0);
    }

    @Override public Dimension getPreferredSize(JComponent component) {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(8, 0) : new Dimension(0, 8);
    }

    @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
    @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    @Override protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
    }

    @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled() || !thumbVisible) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.setColor(isDragging ? Theme.PRIMARY : getThumbColor());
        g.fillRoundRect(bounds.x + 1, bounds.y + 1, Math.max(1, bounds.width - 2),
                Math.max(1, bounds.height - 2), 8, 8);
        g.dispose();
    }

    @Override protected void installListeners() {
        super.installListeners();
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { pointerInside = true; reveal(); }
            @Override public void mouseMoved(MouseEvent e) { pointerInside = true; reveal(); }
            @Override public void mouseDragged(MouseEvent e) { pointerInside = true; reveal(); }
            @Override public void mouseExited(MouseEvent e) {
                pointerInside = false;
                if (!isDragging) scheduleHide();
            }
        };
        scrollbar.addMouseListener(mouse);
        scrollbar.addMouseMotionListener(mouse);
        scrollbar.addAdjustmentListener(e -> reveal());
    }

    public void reveal() {
        thumbVisible = true;
        scheduleHide();
        scrollbar.repaint();
    }

    private void scheduleHide() {
        if (hideTimer != null) hideTimer.stop();
        hideTimer = new Timer(750, e -> {
            if (!scrollbar.getValueIsAdjusting() && !pointerInside) {
                thumbVisible = false;
                scrollbar.repaint();
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private static JButton zeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusable(false);
        return button;
    }

    private Color getThumbColor() { return thumbColor; }
}
