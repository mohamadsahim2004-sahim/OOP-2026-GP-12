package com.facultyams.ui.components;

import java.awt.*;
import javax.swing.JTextField;

import com.facultyams.Theme;

/** Text field with a non-intrusive in-field placeholder. */
public class StyledTextField extends JTextField {
    private final String placeholder;

    public StyledTextField(String placeholder) {
        this.placeholder = placeholder;
        setFont(Theme.COURSE_BODY);
        setForeground(Theme.COURSE_TEXT_MAIN);
        setOpaque(false);
        setBorder(null);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(Theme.COURSE_TEXT_MUTED);
            Insets insets = getInsets();
            g2.drawString(placeholder, insets.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
