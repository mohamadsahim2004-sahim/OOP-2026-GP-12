package com.facultyams.ui.components;

import com.facultyams.model.Course;
import com.facultyams.service.EnrollmentService;
import com.facultyams.service.EnrollmentResult;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;

/** Shared styled table for enrolled and available courses. */
public final class CourseTable extends JTable {
    private static final int[] PROPORTIONS = {10, 22, 8, 16, 12, 12, 8, 12};
    private final EnrollmentTableModel courseModel;
    private final EnrollmentService enrollmentService;
    private final boolean enrolledTable;
    private final Consumer<Course> action;
    private int hoveredRow = -1;
    private int pressedRow = -1;

    public CourseTable(EnrollmentTableModel model, EnrollmentService service,
                       boolean enrolledTable, Consumer<Course> action) {
        super(model);
        this.courseModel = model;
        this.enrollmentService = service;
        this.enrolledTable = enrolledTable;
        this.action = action;
        configure();
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                if (hoveredRow != row) {
                    hoveredRow = row;
                    repaint();
                }
                setCursor(column >= 0 && convertColumnIndexToModel(column) == 7
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                pressedRow = rowAtPoint(e.getPoint());
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressedRow = -1;
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                pressedRow = -1;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                if (row < 0 || convertColumnIndexToModel(column) != 7) return;
                Course course = courseModel.getCourseAt(convertRowIndexToModel(row));
                if (!enrolledTable) {
                    EnrollmentResult result = enrollmentService.validateEnrollment(course);
                    if (!result.success()) {
                        firePropertyChange("enrollmentRejected", null, result.message());
                        return;
                    }
                }
                action.accept(course);
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        setToolTipText("");
    }

    public EnrollmentService getEnrollmentService() { return enrollmentService; }
    public int getHoveredRow() { return hoveredRow; }
    public int getPressedRow() { return pressedRow; }

    private void configure() {
        setFont(Theme.ENROLLMENT_BODY);
        setForeground(Theme.ENROLLMENT_TEXT);
        setBackground(Color.WHITE);
        setRowHeight(Theme.ENROLLMENT_ROW_HEIGHT);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionBackground(Theme.ENROLLMENT_ROW_HOVER);
        setSelectionForeground(Theme.ENROLLMENT_TEXT);
        setFillsViewportHeight(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        setTableHeader(createHeader());
        setDefaultRenderer(Object.class, new TextCellRenderer());
        getColumnModel().getColumn(2).setCellRenderer(new TextCellRenderer(SwingConstants.CENTER));
        if (enrolledTable) getColumnModel().getColumn(4).setCellRenderer(new PillCellRenderer(true));
        getColumnModel().getColumn(5).setCellRenderer(new ScheduleCellRenderer());
        getColumnModel().getColumn(6).setCellRenderer(enrolledTable
                ? new PillCellRenderer(true, true) : new TextCellRenderer());
        getColumnModel().getColumn(7).setCellRenderer(new ActionButtonCell(enrolledTable, this));
        setPreferredSize(new Dimension(0,
                Theme.ENROLLMENT_HEADER_HEIGHT + getRowCount() * Theme.ENROLLMENT_ROW_HEIGHT));
    }

    private JTableHeader createHeader() {
        JTableHeader header = new JTableHeader(getColumnModel());
        header.setPreferredSize(new Dimension(0, Theme.ENROLLMENT_HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderCellRenderer());
        header.setBackground(Theme.ENROLLMENT_ROW_HOVER);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ENROLLMENT_BORDER));
        return header;
    }

    @Override
    public void doLayout() {
        int width = getParent() == null ? getWidth() : getParent().getWidth();
        if (width > 0) {
            int total = 0;
            for (int part : PROPORTIONS) total += part;
            int assignedWidth = 0;
            for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                int columnWidth = i == getColumnModel().getColumnCount() - 1
                        ? width - assignedWidth : width * PROPORTIONS[i] / total;
                getColumnModel().getColumn(i).setPreferredWidth(columnWidth);
                assignedWidth += columnWidth;
            }
        }
        super.doLayout();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int column = columnAtPoint(event.getPoint());
        if (row < 0 || column < 0) return null;
        int modelColumn = convertColumnIndexToModel(column);
        Course course = courseModel.getCourseAt(convertRowIndexToModel(row));
        if (modelColumn == 7 && !enrolledTable) {
            EnrollmentResult result = enrollmentService.validateEnrollment(course);
            return result.success() ? "Enroll in " + course.getCode() : result.message();
        }
        Object value = getValueAt(row, column);
        String text = value == null ? "" : value.toString();
        if (modelColumn == 5) text = text.replaceAll("\\s+", " ");
        int available = getCellRect(row, column, false).width - 2 * Theme.ENROLLMENT_CELL_PADDING_X;
        return getFontMetrics(Theme.ENROLLMENT_BODY).stringWidth(text) > available ? text : null;
    }

    private String ellipsizeText(String text, int columnWidth) {
        FontMetrics metrics = getFontMetrics(Theme.ENROLLMENT_BODY);
        int maxWidth = Math.max(0, columnWidth - 2 * Theme.ENROLLMENT_CELL_PADDING_X);
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "\u2026";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) end--;
        return end == 0 ? ellipsis : text.substring(0, end).stripTrailing() + ellipsis;
    }

    private final class TextCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        TextCellRenderer() { this(SwingConstants.LEFT); }
        TextCellRenderer(int alignment) { this.alignment = alignment; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            String fullText = value == null ? "" : value.toString();
            super.getTableCellRendererComponent(table,
                    ellipsizeText(fullText, getColumnModel().getColumn(column).getWidth()), selected, focus, row, column);
            setHorizontalAlignment(alignment);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(Theme.ENROLLMENT_BODY);
            setForeground(Theme.ENROLLMENT_TEXT);
            setBackground(row == hoveredRow ? Theme.ENROLLMENT_ROW_HOVER : Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ENROLLMENT_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.ENROLLMENT_CELL_PADDING_X, 0,
                            Theme.ENROLLMENT_CELL_PADDING_X)));
            return this;
        }

    }

    private final class HeaderCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setText(value == null ? "" : value.toString().toUpperCase(java.util.Locale.ROOT));
            setFont(Theme.ENROLLMENT_SMALL_BOLD);
            setForeground(Theme.ENROLLMENT_MUTED);
            setBackground(Theme.ENROLLMENT_ROW_HOVER);
            setHorizontalAlignment(column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ENROLLMENT_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.ENROLLMENT_CELL_PADDING_X, 0,
                            Theme.ENROLLMENT_CELL_PADDING_X)));
            return this;
        }
    }

    private final class ScheduleCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            String text = value == null ? "" : value.toString();
            text = ellipsizeText(text, getColumnModel().getColumn(column).getWidth());
            String[] pieces = text.split("\\s+", 2);
            String day = pieces.length > 0 ? pieces[0] : "";
            String time = pieces.length > 1 ? pieces[1] : "";
            setText("<html><b>" + day + "</b>&nbsp;&nbsp;<span style='color:#6B7280'>"
                    + time + "</span></html>");
            setFont(Theme.ENROLLMENT_BODY);
            setForeground(Theme.ENROLLMENT_TEXT);
            setBackground(row == hoveredRow ? Theme.ENROLLMENT_ROW_HOVER : Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ENROLLMENT_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.ENROLLMENT_CELL_PADDING_X, 0,
                            Theme.ENROLLMENT_CELL_PADDING_X)));
            return this;
        }
    }
}
