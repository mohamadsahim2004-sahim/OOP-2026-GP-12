package com.fot.ams.ui.components;

import com.fot.ams.model.UserRole;
import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * RoleTabSelector displays a row of 4 equally-spaced segmented pill buttons
 * aligned to the form width.
 */
public class RoleTabSelector extends JPanel {
    private UserRole selectedRole = UserRole.UNDERGRADUATE;
    private final Consumer<UserRole> onRoleChanged;
    private final List<TabButton> buttons = new ArrayList<>();

    public RoleTabSelector(Consumer<UserRole> onRoleChanged) {
        this.onRoleChanged = onRoleChanged;
        setLayout(new GridLayout(1, 4, 8, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(490, 34));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        addTab("Undergraduate", UserRole.UNDERGRADUATE);
        addTab("Lecturer", UserRole.LECTURER);
        addTab("Tech Officer", UserRole.TECHNICAL_OFFICER);
        addTab("Admin", UserRole.ADMIN);
    }

    private void addTab(String title, UserRole role) {
        TabButton btn = new TabButton(title, role);
        buttons.add(btn);
        add(btn);
    }

    public UserRole getSelectedRole() {
        return selectedRole;
    }

    public void setSelectedRole(UserRole role) {
        this.selectedRole = role;
        for (TabButton b : buttons) {
            b.repaint();
        }
        if (onRoleChanged != null) {
            onRoleChanged.accept(role);
        }
    }

    private class TabButton extends JButton {
        private final UserRole role;
        private boolean isHovered = false;

        public TabButton(String text, UserRole role) {
            super(text);
            this.role = role;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 11));

            addActionListener(e -> setSelectedRole(role));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = 10;

            boolean isSelected = (role == selectedRole);

            if (isSelected) {
                // Active: Vibrant Indigo/Purple Fill
                g2.setColor(ThemeColors.BUTTON_GRADIENT_START);
                g2.fillRoundRect(1, 1, w - 2, h - 2, radius, radius);
                g2.setColor(Color.WHITE);
            } else {
                // Inactive: Balanced light chip
                g2.setColor(isHovered ? ThemeColors.CHIP_HOVER_BG : ThemeColors.CHIP_BG);
                g2.fillRoundRect(1, 1, w - 2, h - 2, radius, radius);

                g2.setColor(isHovered ? ThemeColors.BUTTON_GRADIENT_START : ThemeColors.CHIP_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);

                g2.setColor(isHovered ? ThemeColors.BUTTON_GRADIENT_START : ThemeColors.CHIP_TEXT);
            }

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }
}
