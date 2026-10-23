package com.facultyams.ui.components;

import com.facultyams.model.Course;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Small application-styled confirmation dialog for dropping a course. */
public final class ConfirmDialog {
    private ConfirmDialog() {}

    public static boolean confirmDrop(Component parent, Course course) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Confirm course drop", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RoundedPanel surface = new RoundedPanel(Theme.ENROLLMENT_CARD_RADIUS);
        surface.setBorderColor(Theme.ENROLLMENT_BORDER);
        surface.setLayout(new BorderLayout(0, Theme.ENROLLMENT_SECTION_GAP));
        surface.setBorder(Theme.createEmptyPadding(Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING, Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING));
        JLabel message = new JLabel("<html>Drop <b>" + escape(course.getCode()) + " - "
                + escape(course.getTitle()) + "</b>? You will lose " + course.getCredits() + " credits.</html>");
        message.setFont(Theme.ENROLLMENT_BODY);
        message.setForeground(Theme.ENROLLMENT_TEXT);
        surface.add(message, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new DialogButton("Cancel", false);
        JButton drop = new DialogButton("Drop", true);
        final boolean[] confirmed = {false};
        cancel.addActionListener(e -> dialog.dispose());
        drop.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        buttons.add(cancel);
        buttons.add(drop);
        surface.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(surface);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return confirmed[0];
    }

    private static final class DialogButton extends JButton {
        private final boolean danger;
        private boolean hovered;

        DialogButton(String text, boolean danger) {
            super(text);
            this.danger = danger;
            setFont(Theme.ENROLLMENT_BODY_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(88, Theme.ENROLLMENT_BUTTON_HEIGHT));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (danger) {
                g.setColor(getModel().isPressed() ? new Color(185, 28, 28)
                        : hovered ? new Color(185, 28, 28) : Theme.ENROLLMENT_DANGER);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                setForeground(Color.WHITE);
            } else {
                g.setColor(hovered ? Theme.ENROLLMENT_ROW_HOVER : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                g.setColor(Theme.ENROLLMENT_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                setForeground(Theme.ENROLLMENT_TEXT);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
