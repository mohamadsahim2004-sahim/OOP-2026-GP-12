package com.facultyams;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Reusable clean UI components: Card, Pill, NavButton, Cols, ThinScrollBarUI.
 */
public final class Ui {
    private Ui() {}

    /**
     * White card with 12px corner radius and 1px #E5E7EB border.
     */
    public static class Card extends JPanel {
        private final int radius;
        private Color borderColor;
        private Color bgColor;

        public Card() {
            this(12, Theme.CARD_BG, Theme.CARD_BORDER);
        }

        public Card(int radius, Color bg, Color border) {
            this.radius = radius;
            this.bgColor = bg;
            this.borderColor = border;
            setOpaque(false);
        }

        public void setBorderColor(Color color) {
            this.borderColor = color;
            repaint();
        }

        public void setCardBackground(Color color) {
            this.bgColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);
            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(bgColor != null ? bgColor : Theme.CARD_BG);
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, w, h, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Small pill badge with rounded pill shape.
     */
    public static class Pill extends JLabel {
        private Color bgColor;
        private Color textColor;

        public Pill(String text, Color bg, Color textCol) {
            super(text, SwingConstants.CENTER);
            this.bgColor = bg;
            this.textColor = textCol;
            setFont(Theme.FONT_TINY);
            setForeground(textCol);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        public void setColors(Color bg, Color textCol) {
            this.bgColor = bg;
            this.textColor = textCol;
            setForeground(textCol);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);
            int h = getHeight();
            int w = getWidth();

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Minimal flat navigation button with hover effect.
     */
    public static class NavButton extends JButton {
        private boolean hover = false;
        private Color normalBg = Color.WHITE;
        private Color hoverBg = Theme.HOVER_BG;

        public NavButton(String text) {
            super(text);
            setFont(Theme.FONT_BODY_BOLD);
            setForeground(Theme.TEXT_MAIN);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(hover ? hoverBg : normalBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(Theme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Spacing and grid helpers.
     */
    public static final class Cols {
        public static final int GAP_XS = 4;
        public static final int GAP_SM = 8;
        public static final int GAP_MD = 16;
        public static final int GAP_LG = 24;
        public static final int GAP_XL = 32;

        public static Component rigid(int width, int height) {
            return Box.createRigidArea(new Dimension(width, height));
        }
    }

    /**
     * Sleek, thin scrollbar UI.
     */
    public static class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(203, 213, 225); // Slate 300
            this.trackColor = Color.TRANSLUCENT == 0 ? new Color(0, 0, 0, 0) : new Color(248, 250, 252);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(8, 8);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            btn.setVisible(false);
            return btn;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Invisible/flat track
        }
    }

    public static void applyThinScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
    }
}
