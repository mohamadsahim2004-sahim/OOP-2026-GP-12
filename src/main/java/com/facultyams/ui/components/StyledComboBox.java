package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Compact department selector with a vector chevron. */
public final class StyledComboBox extends JComboBox<String> {
    public StyledComboBox(String[] values) {
        super(values);
        setFont(Theme.ENROLLMENT_BODY);
        setForeground(Theme.ENROLLMENT_TEXT);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(184, Theme.ENROLLMENT_CONTROL_HEIGHT));
        setBorder(new ComboBorder());
        setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                return new JButton() {
                    {
                        setBorder(BorderFactory.createEmptyBorder());
                        setContentAreaFilled(false);
                        setFocusable(false);
                    }
                    @Override protected void paintComponent(Graphics graphics) {
                        int size = Math.min(16, getHeight() - 4);
                        EnrollmentIcons.paint((Graphics2D) graphics,
                                EnrollmentIcons.Type.CHEVRON_DOWN, (getWidth() - size) / 2,
                                (getHeight() - size) / 2, size, Theme.ENROLLMENT_MUTED);
                    }
                };
            }
        });
        setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(Theme.ENROLLMENT_BODY);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                if (!isSelected) {
                    setBackground(Color.WHITE);
                    setForeground(Theme.ENROLLMENT_TEXT);
                }
                return this;
            }
        });
    }

    private static final class ComboBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 12, 1, 12); }
        @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Theme.ENROLLMENT_BORDER);
            g.drawRoundRect(x, y, w - 1, h - 1, Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
            g.dispose();
        }
    }
}
