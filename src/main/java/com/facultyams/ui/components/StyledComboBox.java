package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

import com.facultyams.Theme;

/** White rounded combo box with a compact, themed popup renderer. */
public class StyledComboBox<T> extends JComboBox<T> {
    private boolean highlighted;
    private final String displayPrefix;

    public StyledComboBox(T[] values) {
        this(values, "");
    }

    public StyledComboBox(T[] values, String displayPrefix) {
        super(values);
        this.displayPrefix = displayPrefix;
        setFont(Theme.COURSE_BODY);
        setForeground(Theme.COURSE_TEXT_MAIN);
        setBackground(Color.WHITE);
        setFocusable(true);
        setRenderer(new PopupRenderer());
        setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        int size = Math.min(18, Math.min(getWidth(), getHeight() - 4));
                        Icons.paint((Graphics2D) graphics, Icons.Type.CHEVRON,
                                (getWidth() - size) / 2, (getHeight() - size) / 2,
                                size, Theme.COURSE_TEXT_MUTED);
                    }
                };
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                button.setFocusable(false);
                return button;
            }
        });
        setBorder(new RoundedBorder(Theme.CARD_BORDER));
        setPreferredSize(new Dimension(Theme.CONTROL_HEIGHT, Theme.CONTROL_HEIGHT));
        setMinimumSize(new Dimension(Theme.CONTROL_HEIGHT, Theme.CONTROL_HEIGHT));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { highlighted = true; updateBorder(); }
            @Override public void mouseExited(MouseEvent e) { highlighted = false; updateBorder(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { updateBorder(); }
            @Override public void focusLost(FocusEvent e) { updateBorder(); }
        });
    }

    private void updateBorder() {
        setBorder(new RoundedBorder(isFocusOwner() || highlighted ? Theme.PRIMARY : Theme.CARD_BORDER));
    }

    private final class PopupRenderer extends JLabel implements ListCellRenderer<T> {
        PopupRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(Theme.CONTROL_GAP / 2,
                    Theme.CONTROL_GAP, Theme.CONTROL_GAP / 2, Theme.CONTROL_GAP));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
                                                      boolean selected, boolean cellHasFocus) {
            boolean current = value != null && value.equals(StyledComboBox.this.getSelectedItem());
            setText(displayPrefix + (value == null ? "" : value.toString()));
            setFont(Theme.COURSE_BODY);
            setForeground(index >= 0 && current ? Theme.PRIMARY : Theme.COURSE_TEXT_MAIN);
            setBackground(cellHasFocus ? Theme.PRIMARY_LIGHT : Color.WHITE);
            setIcon(new CheckIcon(index >= 0 && current));
            return this;
        }
    }

    private static final class CheckIcon implements Icon {
        private final boolean checked;
        CheckIcon(boolean checked) { this.checked = checked; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            if (checked) Icons.paint((Graphics2D) graphics, Icons.Type.CHECK, x, y, 14, Theme.PRIMARY);
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        RoundedBorder(Color color) { this.color = color; }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, Theme.CONTROL_GAP, 1, Theme.CONTROL_GAP);
        }

        @Override
        public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1,
                    Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            g2.dispose();
        }
    }
}
