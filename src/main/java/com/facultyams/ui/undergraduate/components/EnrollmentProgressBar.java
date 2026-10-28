package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.JComponent;
import javax.swing.Timer;

/** Rounded credit meter track and animated fill. */
public final class EnrollmentProgressBar extends JComponent {
    private int value;
    private int maximum;
    private Timer animation;

    public EnrollmentProgressBar(int maximum) {
        this.maximum = Math.max(1, maximum);
        setOpaque(false);
        setPreferredSize(new Dimension(240, 8));
        setMinimumSize(new Dimension(120, 8));
    }

    public void animateTo(int newValue, int newMaximum) {
        if (animation != null) animation.stop();
        maximum = Math.max(1, newMaximum);
        int target = Math.max(0, Math.min(maximum, newValue));
        animation = new Timer(16, null);
        animation.addActionListener(event -> {
            if (value == target) {
                animation.stop();
                return;
            }
            value += Integer.compare(target, value) * Math.max(1, Math.abs(target - value) / 8);
            if (Math.abs(target - value) <= 1) value = target;
            repaint();
        });
        animation.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        int height = 8;
        int y = (getHeight() - height) / 2;
        g.setColor(Theme.ENROLLMENT_BORDER);
        g.fillRoundRect(0, y, getWidth(), height, height, height);
        double ratio = Math.min(1.0, (double) value / maximum);
        int width = (int) Math.round(getWidth() * ratio);
        Color color = ratio >= 1 ? Theme.ENROLLMENT_DANGER : ratio >= 0.9
                ? Theme.ENROLLMENT_WARNING : Theme.PRIMARY;
        if (width > 0) {
            g.setColor(color);
            g.fillRoundRect(0, y, width, height, height, height);
        }
        g.dispose();
    }
}
