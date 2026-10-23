package com.facultyams.ui.components;

import com.facultyams.model.CourseType;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

/** Centered rounded status, course-type, or department badge renderer. */
public final class PillCellRenderer extends JPanel implements TableCellRenderer {
    private final PillLabel label = new PillLabel();
    private final boolean enrolledColumn;
    private final boolean status;

    public PillCellRenderer(boolean enrolledColumn) {
        this(enrolledColumn, false);
    }

    public PillCellRenderer(boolean enrolledColumn, boolean status) {
        this.enrolledColumn = enrolledColumn;
        this.status = status;
        setOpaque(true);
        setLayout(new GridBagLayout());
        add(label);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                   boolean focus, int row, int column) {
        String text = value == null ? "" : value.toString();
        label.setText(text);
        label.setFont(Theme.ENROLLMENT_SMALL_BOLD);
        label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        Color bg = new Color(0, 0, 0, 0);
        Color fg = Theme.ENROLLMENT_MUTED;
        if (!text.isEmpty()) {
            if (status) {
                boolean isEnrolled = "Enrolled".equalsIgnoreCase(text);
                bg = isEnrolled ? Theme.ENROLLMENT_SUCCESS_BG : Theme.ENROLLMENT_WARNING_BG;
                fg = isEnrolled ? Theme.ENROLLMENT_SUCCESS : Theme.ENROLLMENT_WARNING;
            } else if (enrolledColumn) {
                CourseType type = CourseType.from(text);
                if (type == CourseType.CORE) {
                    bg = Theme.ENROLLMENT_CORE_BG;
                    fg = Theme.ENROLLMENT_CORE_TEXT;
                } else if (type == CourseType.ELECTIVE) {
                    bg = Theme.ENROLLMENT_ELECTIVE_BG;
                    fg = Theme.ENROLLMENT_ELECTIVE_TEXT;
                } else {
                    bg = Theme.ACCENT_GRAY_BG;
                    fg = Theme.ACCENT_GRAY;
                }
            } else {
                bg = Theme.ACCENT_GRAY_BG;
            }
        }
        label.setForeground(fg);
        label.setBackground(bg);
        setBackground(row == ((CourseTable) table).getHoveredRow()
                ? Theme.ENROLLMENT_ROW_HOVER : Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ENROLLMENT_BORDER));
        return this;
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        super.paintChildren(g);
        g.dispose();
    }

    private static final class PillLabel extends JLabel {
        PillLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(getBackground());
            int height = Math.max(18, getHeight() - 16);
            g.fillRoundRect(0, (getHeight() - height) / 2, getWidth(), height, height, height);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
