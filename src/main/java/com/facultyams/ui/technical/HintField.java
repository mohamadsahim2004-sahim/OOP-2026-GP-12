package com.facultyams.ui.technical;

import com.facultyams.ui.admin.AdminTheme;

import javax.swing.JTextField;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A text field that shows grey hint text while empty; the hint is also its tooltip and accessible name. */
final class HintField extends JTextField {

    private static final long serialVersionUID = 1L;
    private final String hint;

    HintField(String hint, int columns) {
        super(columns);
        this.hint = hint;
        setToolTipText(hint);
        getAccessibleContext().setAccessibleName(hint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(AdminTheme.TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, getInsets().left + 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}