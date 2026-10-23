package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Rounded compact combo-box UI used for timetable type filtering. */
public final class RoundedComboBoxUI extends BasicComboBoxUI {
    @Override protected JButton createArrowButton() {
        return new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.TEXT_MUTED);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g.drawLine(cx - 3, cy - 1, cx, cy + 2);
                g.drawLine(cx, cy + 2, cx + 3, cy - 1);
                g.dispose();
            }
            {
                setPreferredSize(new Dimension(22, Theme.CONTROL_HEIGHT));
                setBorder(BorderFactory.createEmptyBorder());
                setContentAreaFilled(false);
                setFocusPainted(false);
                setOpaque(false);
            }
        };
    }

    @Override public void paint(Graphics graphics, JComponent component) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 10, 10);
        g.setColor(component.hasFocus() ? Theme.PRIMARY_BORDER : Theme.CONTROL_BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 10, 10);
        g.dispose();
        super.paint(graphics, component);
    }

    @Override protected void installDefaults() {
        super.installDefaults();
        comboBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
        comboBox.setOpaque(false);
        comboBox.setFont(Theme.FONT_BODY);
        comboBox.setForeground(Theme.TEXT_MAIN);
    }
}
