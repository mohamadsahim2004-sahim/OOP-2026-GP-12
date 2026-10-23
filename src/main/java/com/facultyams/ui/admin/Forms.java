package com.facultyams.ui.admin;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Builds the label-above-field grids used by the Add / Edit dialogs; every label is linked to its field. */
final class Forms {

    private Forms() {
    }

    /** An empty form panel with the standard dialog padding. */
    static JPanel grid() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
        return form;
    }

    /**
     * Adds "label over field" at the given grid cell. Each form row uses two grid rows (label, field).
     *
     * @param span number of columns the field covers
     */
    static void add(JPanel form, int column, int row, int span, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(AdminTheme.FONT_BOLD.deriveFont(12f));
        l.setForeground(AdminTheme.TEXT);
        l.setLabelFor(field);
        field.setPreferredSize(new java.awt.Dimension(span > 1 ? 450 : 220, field.getPreferredSize().height));
        field.getAccessibleContext().setAccessibleName(label.replace(" *", ""));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row * 2;
        c.gridwidth = span;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(8, 6, 0, 6);
        form.add(l, c);
        c.gridy = row * 2 + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 6, 2, 6);
        form.add(field, c);
    }
}