package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JTextField;

/** Enrollment search input with an in-field placeholder. */
public final class StyledTextField extends JTextField {
    private final String placeholder;

    public StyledTextField(String placeholder) {
        this.placeholder = placeholder;
        setFont(Theme.ENROLLMENT_BODY);
        setForeground(Theme.ENROLLMENT_TEXT);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Theme.ENROLLMENT_MUTED);
            g.drawString(placeholder, 0, getHeight() / 2 + g.getFontMetrics().getAscent() / 2 - 1);
            g.dispose();
        }
    }
}
