package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** White rounded combo box with a compact, themed popup renderer. */
public class StyledComboBox<T> extends JComboBox<T> {
    private boolean highlighted;

    public StyledComboBox(T[] values) {
        super(values);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_MAIN);
        setBackground(Color.WHITE);
        setFocusable(true);
        setRenderer(new PopupRenderer());
        setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g2 = (Graphics2D) graphics.create();
                        Theme.setAntiAliasing(g2);
                        int x = getWidth() / 2 - 4;
                        int y = getHeight() / 2 - 1;
                        g2.setColor(Theme.TEXT_MUTED);
                        g2.drawLine(x, y, x + 4, y + 4);
                        g2.drawLine(x + 4, y + 4, x + 8, y);
                        g2.dispose();
                    }
                };
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                button.setFocusable(false);
                return button;
            }
        });
        setBorder(new RoundedBorder(Theme.CARD_BORDER));
        setPreferredSize(new Dimension(168, 40));
        setMinimumSize(new Dimension(120, 40));
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
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
                                                      boolean selected, boolean cellHasFocus) {
            boolean current = value != null && value.equals(StyledComboBox.this.getSelectedItem());
            setText((current ? "✓  " : "    ") + (value == null ? "" : value.toString()));
            setFont(Theme.FONT_BODY);
            setForeground(current ? Theme.PRIMARY : Theme.TEXT_MAIN);
            setBackground(cellHasFocus ? Theme.PRIMARY_LIGHT : Color.WHITE);
            return this;
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        RoundedBorder(Color color) { this.color = color; }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(1, 10, 1, 10); }

        @Override
        public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g2.dispose();
        }
    }
}
