package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.*;

/** Reusable aligned label, input, and inline validation field. */
public final class FormField extends JPanel {
    private final JLabel error = new JLabel(" ");
    private final JComponent input;
    private final boolean readOnly;
    private final JPanel inputRow;
    private Color borderColor = Theme.CONTROL_BORDER;

    /** Creates a labeled profile field using a consistent vertical grid. */
    public FormField(String label, JComponent input, boolean readOnly) {
        super();
        this.input = input;
        this.readOnly = readOnly;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel(label);
        title.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        title.setForeground(Theme.TEXT_MUTED);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));
        input.setPreferredSize(new Dimension(100, Theme.PROFILE_FIELD_HEIGHT));
        input.setMinimumSize(new Dimension(80, Theme.PROFILE_FIELD_HEIGHT));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.PROFILE_FIELD_HEIGHT));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(input.getBackground());
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g.setColor(borderColor);
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g.dispose();
            }

            @Override protected void paintChildren(Graphics graphics) {
                super.paintChildren(graphics);
                if (!FormField.this.readOnly) return;
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                new ProfileIcon(ProfileIcon.Shape.LOCK, Theme.TEXT_SUBTLE, 14)
                        .paintIcon(this, g, getWidth() - 26, (getHeight() - 14) / 2);
                g.dispose();
            }
        };
        inputRow.setOpaque(false);
        inputRow.setPreferredSize(new Dimension(100, Theme.PROFILE_FIELD_HEIGHT));
        inputRow.setMinimumSize(new Dimension(80, Theme.PROFILE_FIELD_HEIGHT));
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.PROFILE_FIELD_HEIGHT));
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.setToolTipText(readOnly ? "Managed by the university. Contact the registrar to change" : null);
        add(inputRow);
        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (!readOnly && error.getText().isBlank()) setBorderColor(Theme.PRIMARY);
            }
            @Override public void focusLost(FocusEvent e) {
                if (!readOnly && error.getText().isBlank()) setBorderColor(Theme.CONTROL_BORDER);
            }
        });
        error.setFont(Theme.FONT_SMALL);
        error.setForeground(Theme.PROFILE_ERROR);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(error);
        setEditable(false);
    }

    @Override public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(100, preferred.height);
    }

    @Override public Dimension getMinimumSize() {
        Dimension minimum = super.getMinimumSize();
        return new Dimension(80, minimum.height);
    }

    /** Sets the field's editability and appropriate surface style. */
    public void setEditable(boolean editable) {
        input.setEnabled(input instanceof JTextField || input instanceof JTextArea || readOnly ? true : editable);
        input.setFocusable(!readOnly);
        if (input instanceof JTextField field) field.setEditable(editable && !readOnly);
        if (input instanceof JTextArea area) area.setEditable(editable && !readOnly);
        input.setBackground(readOnly ? Theme.PROFILE_LOCKED_BG : Color.WHITE);
        input.setForeground(Theme.TEXT_MAIN);
        input.setToolTipText(readOnly ? "Managed by the university. Contact the registrar to change" : null);
        input.setOpaque(false);
        if (input instanceof JTextField) {
            input.setBorder(BorderFactory.createEmptyBorder(0, Theme.PROFILE_FIELD_INSET, 0,
                    readOnly ? Theme.PROFILE_FIELD_INSET + 18 : Theme.PROFILE_FIELD_INSET));
        } else {
            input.setBorder(BorderFactory.createEmptyBorder());
        }
        inputRow.repaint();
        if (input instanceof JComboBox<?> combo) combo.setEnabled(editable && !readOnly);
    }

    /** Shows or clears a validation message. */
    public void setError(String message) {
        error.setText(message == null || message.isBlank() ? " " : message);
        setBorderColor(message == null || message.isBlank() ? Theme.CONTROL_BORDER : Theme.PROFILE_ERROR);
    }

    /** Returns whether the field currently has a validation error. */
    public boolean hasError() {
        return !error.getText().isBlank();
    }

    private void setBorderColor(Color color) {
        borderColor = color;
        inputRow.repaint();
    }

    /** Returns the input component. */
    public JComponent getInput() { return input; }

    /** Returns whether this field is managed and read-only. */
    public boolean isReadOnly() { return readOnly; }
}
