package com.facultyams.ui.components;

import com.facultyams.model.Course;
import com.facultyams.service.EnrollmentResult;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

/** Compact action button renderer; table click handling stays in CourseTable. */
public final class ActionButtonCell extends JPanel implements TableCellRenderer {
    private final PaintedActionButton button;
    private final boolean dropAction;
    private final CourseTable table;

    public ActionButtonCell(boolean dropAction, CourseTable table) {
        this.dropAction = dropAction;
        this.table = table;
        button = new PaintedActionButton(dropAction);
        setOpaque(true);
        setLayout(new GridBagLayout());
        add(button);
    }

    @Override
    public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                   boolean focus, int row, int column) {
        button.setText(value == null ? "" : value.toString());
        button.setEnabled(true);
        button.setToolTipText(null);
        button.setValidationMessage(null);
        if (!dropAction && table.getModel() instanceof EnrollmentTableModel model && row < model.getRowCount()) {
            Course course = model.getCourseAt(row);
            EnrollmentResult validation = table.getEnrollmentService().validateEnrollment(course);
            button.setEnabled(validation.success());
            button.setValidationMessage(validation.success() ? null : validation.message());
        }
        button.setHovered(row == table.getHoveredRow());
        button.setPressed(row == table.getPressedRow());
        setBackground(row == table.getHoveredRow() ? Theme.ENROLLMENT_ROW_HOVER : Color.WHITE);
        return this;
    }

    private static final class PaintedActionButton extends JButton {
        private final boolean dropAction;
        private boolean hovered;
        private boolean pressed;
        private String validationMessage;

        PaintedActionButton(boolean dropAction) {
            this.dropAction = dropAction;
            setFont(Theme.ENROLLMENT_SMALL_BOLD);
            setPreferredSize(new Dimension(80, Theme.ENROLLMENT_BUTTON_HEIGHT));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setHovered(boolean hovered) { this.hovered = hovered; repaint(); }
        void setPressed(boolean pressed) { this.pressed = pressed; repaint(); }
        void setValidationMessage(String message) {
            validationMessage = message;
            setToolTipText(message);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (dropAction) {
                g.setColor(pressed ? Theme.ENROLLMENT_DANGER_BG.darker()
                        : hovered ? Theme.ENROLLMENT_DANGER_BG : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                g.setColor(new Color(252, 165, 165));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                setForeground(Theme.ENROLLMENT_DANGER);
            } else if (!isEnabled()) {
                g.setColor(new Color(243, 244, 246));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                setForeground(Theme.ENROLLMENT_MUTED);
            } else {
                g.setColor(pressed ? new Color(30, 64, 175)
                        : hovered ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.ENROLLMENT_CONTROL_RADIUS, Theme.ENROLLMENT_CONTROL_RADIUS);
                setForeground(Color.WHITE);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
