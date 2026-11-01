package com.facultyams.ui.admin;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A rounded text field with an optional leading icon that shows grey hint text while it is empty (the search boxes). */
final class HintTextField extends JTextField {

    private static final long serialVersionUID = 1L;
    private final String hint;
    private transient AdminIcons.Kind icon;

    HintTextField(String hint, int columns) {
        super(columns);
        this.hint = hint;
        setOpaque(false);
        AdminTheme.field(this);
        setBorder(new AdminTheme.FieldBorder(8, 10));
        getAccessibleContext().setAccessibleName(hint);
        setToolTipText(hint);
    }

    /** Adds a leading icon (e.g. the magnifier) inside the field. */
    HintTextField icon(AdminIcons.Kind kind) {
        this.icon = kind;
        setBorder(BorderFactory.createCompoundBorder(new AdminTheme.FieldBorder(8, 10),
                BorderFactory.createEmptyBorder(0, 22, 0, 0)));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? Color.WHITE : AdminTheme.PANEL_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        if (icon != null) {
            AdminIcons.of(icon, 16, AdminTheme.TEXT_MUTED).paintIcon(this, g2, 12, (getHeight() - 16) / 2);
        }
        g2.dispose();
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D h = (Graphics2D) g.create();
            h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            h.setColor(new Color(0x94A3B8));
            h.setFont(getFont());
            FontMetrics fm = h.getFontMetrics();
            h.drawString(hint, getInsets().left + 1, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            h.dispose();
        }
    }
}