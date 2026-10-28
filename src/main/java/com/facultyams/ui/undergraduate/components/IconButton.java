package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Compact flat button that toggles between two vector icons. */
public final class IconButton extends JButton {
    private final Icon offIcon;
    private final Icon onIcon;

    public IconButton(Icon offIcon, Icon onIcon, boolean selected, String tooltip) {
        this.offIcon = offIcon;
        this.onIcon = onIcon;
        setSelected(selected);
        setIcon(selected ? onIcon : offIcon);
        setToolTipText(tooltip);
        setPreferredSize(new Dimension(28, 28));
        setMinimumSize(getPreferredSize());
        setBorder(BorderFactory.createEmptyBorder());
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addActionListener(e -> setSelected(!isSelected()));
    }

    @Override public void setSelected(boolean selected) {
        super.setSelected(selected);
        setIcon(selected ? onIcon : offIcon);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        if (getModel().isRollover()) {
            g.setColor(Theme.PRIMARY_LIGHT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
