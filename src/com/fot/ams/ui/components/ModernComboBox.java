package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * ModernComboBox provides a sleek rounded dropdown matching ModernTextField.
 */
public class ModernComboBox<E> extends JComboBox<E> {
    private final int cornerRadius = 12;

    public ModernComboBox(E[] items) {
        super(items);
        initStyle();
    }

    private void initStyle() {
        setOpaque(false);
        setFont(ThemeColors.FONT_INPUT);
        setForeground(ThemeColors.TEXT_HEADLINE);
        setBackground(ThemeColors.INPUT_BG);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setRenderer(new ListCellRenderer<E>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setFont(ThemeColors.FONT_INPUT);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 12, 8, 12));
                if (isSelected) {
                    label.setBackground(ThemeColors.CHIP_HOVER_BG);
                    label.setForeground(ThemeColors.BUTTON_GRADIENT_START);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(ThemeColors.TEXT_HEADLINE);
                }
                return label;
            }
        });

        setUI(new BasicComboBoxUI() {
            @Override
            protected javax.swing.JButton createArrowButton() {
                javax.swing.JButton btn = new javax.swing.JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(ThemeColors.TEXT_MUTED);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                        g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                        g2.dispose();
                    }
                };
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setPreferredSize(new Dimension(30, 30));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                // Background painted in ModernComboBox.paintComponent
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.setBorder(new javax.swing.border.LineBorder(ThemeColors.INPUT_BORDER, 1));
                return popup;
            }
        });

        setBorder(new EmptyBorder(6, 12, 6, 8));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2.setColor(ThemeColors.INPUT_BG);
        g2.fillRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);

        // Border
        g2.setColor(hasFocus() ? ThemeColors.INPUT_BORDER_FOCUS : ThemeColors.INPUT_BORDER);
        g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1.0f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
