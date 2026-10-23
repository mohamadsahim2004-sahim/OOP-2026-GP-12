package com.srms.ui.components;

import com.srms.Theme;
import com.srms.model.DeadlineType;
import java.awt.*;
import javax.swing.*;

public final class TypePill extends JComponent {
    private final DeadlineType type;

    public TypePill(DeadlineType type) {
        this.type = type;
        setPreferredSize(new Dimension(76, 24));
        setMinimumSize(new Dimension(72, 24));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.setColor(type.badgeBackground());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.setColor(type.badgeForeground());
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        String label = type.displayLabel();
        g.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
                (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g.dispose();
    }
}
