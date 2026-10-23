package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import javax.swing.JTextField;

/** Text field with a non-intrusive in-field placeholder. */
public class StyledTextField extends JTextField {
    private final String placeholder;

    public StyledTextField(String placeholder) {
        this.placeholder = placeholder;
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_MAIN);
        setOpaque(false);
        setBorder(null);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(Theme.TEXT_MUTED);
            Insets insets = getInsets();
            g2.drawString(placeholder, insets.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
