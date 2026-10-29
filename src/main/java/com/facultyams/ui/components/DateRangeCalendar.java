package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.model.MedicalPolicy;

/**
 * High-definition Java2D DateRangeCalendar supporting range picking,
 * absent-session dots, presets, keyboard navigation, and inline inputs.
 */
public class DateRangeCalendar extends JPanel {
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String[] WEEKDAYS = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

    private YearMonth currentMonth;
    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private LocalDate hoverDate;
    private LocalDate focusedDate;

    private final Set<LocalDate> absentDates = new HashSet<>();
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JLabel inlineError = new JLabel();
    private final CalendarCanvas canvas = new CalendarCanvas();

    public DateRangeCalendar() {
        this(LocalDate.now());
    }

    public DateRangeCalendar(LocalDate initialFocus) {
        this.currentMonth = YearMonth.from(initialFocus);
        this.focusedDate = initialFocus;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        initUI();
    }

    private void initUI() {
        // 1. Presets Row
        JPanel presetsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        presetsRow.setOpaque(false);

        presetsRow.add(createPresetChip("Today", () -> setRange(LocalDate.now(), LocalDate.now())));
        presetsRow.add(createPresetChip("Yesterday", () -> setRange(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1))));
        presetsRow.add(createPresetChip("Last 3 days", () -> setRange(LocalDate.now().minusDays(2), LocalDate.now())));
        presetsRow.add(createPresetChip("This week", () -> {
            LocalDate mon = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate end = LocalDate.now();
            setRange(mon, end);
        }));

        // 2. Month Nav Header
        JPanel navHeader = new JPanel(new BorderLayout());
        navHeader.setOpaque(false);
        navHeader.setBorder(BorderFactory.createEmptyBorder(6, 4, 8, 4));

        JButton prevBtn = createNavArrow("<", -1);
        JButton nextBtn = createNavArrow(">", 1);

        JLabel monthLbl = new JLabel(currentMonth.format(MONTH_FMT), SwingConstants.CENTER) {
            @Override
            public void setText(String text) {
                super.setText(text);
                repaint();
            }
        };
        monthLbl.setFont(Theme.FONT_SUBTITLE);
        monthLbl.setForeground(Theme.TEXT_MAIN);

        navHeader.add(prevBtn, BorderLayout.WEST);
        navHeader.add(monthLbl, BorderLayout.CENTER);
        navHeader.add(nextBtn, BorderLayout.EAST);

        // Update month label listener
        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            monthLbl.setText(currentMonth.format(MONTH_FMT));
            canvas.repaint();
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthLbl.setText(currentMonth.format(MONTH_FMT));
            canvas.repaint();
        });

        // 3. Inputs Row (From / To)
        JPanel inputsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        inputsRow.setOpaque(false);

        styleInput(fromField, true);
        styleInput(toField, false);

        inputsRow.add(new JLabel("From:"));
        inputsRow.add(fromField);
        inputsRow.add(new JLabel("To:"));
        inputsRow.add(toField);

        inlineError.setFont(Theme.FONT_TINY);
        inlineError.setForeground(Theme.DANGER);
        inlineError.setVisible(false);

        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setOpaque(false);
        topStack.add(presetsRow);
        topStack.add(Box.createVerticalStrut(10));
        topStack.add(navHeader);
        topStack.add(Box.createVerticalStrut(6));
        topStack.add(inputsRow);
        topStack.add(Box.createVerticalStrut(4));
        topStack.add(inlineError);

        add(topStack, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
    }

    private JButton createPresetChip(String title, Runnable onClick) {
        JButton btn = new JButton(title);
        btn.setFont(Theme.FONT_TINY);
        btn.setForeground(Theme.TEXT_MUTED);
        btn.setBackground(new Color(241, 245, 249));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private JButton createNavArrow(String arrow, int offset) {
        JButton b = new JButton(arrow);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Theme.TEXT_MUTED);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleInput(JTextField tf, boolean isFrom) {
        tf.setFont(Theme.FONT_BODY);
        tf.setPreferredSize(new Dimension(100, 30));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        tf.addActionListener(e -> parseInputs());
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                parseInputs();
            }
        });
    }

    private void parseInputs() {
        String fromText = fromField.getText().trim();
        String toText = toField.getText().trim();
        if (fromText.isEmpty() && toText.isEmpty()) return;

        try {
            LocalDate f = LocalDate.parse(fromText, INPUT_FMT);
            LocalDate t = toText.isEmpty() ? f : LocalDate.parse(toText, INPUT_FMT);
            setRange(f, t);
            inlineError.setVisible(false);
        } catch (Exception ex) {
            inlineError.setText("Invalid date format. Use dd MMM yyyy (e.g. 14 May 2026).");
            inlineError.setVisible(true);
        }
    }

    public void setAbsentDates(Collection<LocalDate> dates) {
        this.absentDates.clear();
        if (dates != null) this.absentDates.addAll(dates);
        canvas.repaint();
    }

    public void setRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        // Validate range policy
        String err = validateRange(start, end);
        if (err != null) {
            inlineError.setText(err);
            inlineError.setVisible(true);
            return;
        }
        inlineError.setVisible(false);

        LocalDate oldStart = this.rangeStart;
        LocalDate oldEnd = this.rangeEnd;
        this.rangeStart = start;
        this.rangeEnd = end;

        if (start != null) fromField.setText(start.format(INPUT_FMT));
        if (end != null) toField.setText(end.format(INPUT_FMT));

        canvas.repaint();
        firePropertyChange("range", new LocalDate[]{ oldStart, oldEnd }, new LocalDate[]{ start, end });
    }

    public LocalDate getRangeStart() {
        return rangeStart;
    }

    public LocalDate getRangeEnd() {
        return rangeEnd;
    }

    private String validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        LocalDate today = LocalDate.now();

        if (end.isAfter(today)) {
            return "Cannot select future dates for medical submission.";
        }
        long daysAgo = ChronoUnit.DAYS.between(start, today);
        if (daysAgo > MedicalPolicy.WINDOW_DAYS) {
            return "Dates must be within the last " + MedicalPolicy.WINDOW_DAYS + " days.";
        }
        long rangeLen = ChronoUnit.DAYS.between(start, end) + 1;
        if (rangeLen > MedicalPolicy.MAX_RANGE_DAYS) {
            return "Maximum medical excuse range is " + MedicalPolicy.MAX_RANGE_DAYS + " days.";
        }
        return null;
    }

    private boolean isSelectable(LocalDate date) {
        if (date == null) return false;
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) return false;
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        long daysAgo = ChronoUnit.DAYS.between(date, today);
        return daysAgo <= MedicalPolicy.WINDOW_DAYS;
    }

    /**
     * Inner Canvas rendering the anti-aliased 7x6 month grid.
     */
    private class CalendarCanvas extends JComponent {
        private final int CELL_SIZE = 38;

        public CalendarCanvas() {
            setPreferredSize(new Dimension(CELL_SIZE * 7 + 10, CELL_SIZE * 7 + 20));
            setFocusable(true);
            initMouse();
            initKeyboard();
        }

        private void initMouse() {
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    LocalDate d = dateAt(e.getPoint());
                    hoverDate = d;
                    if (d != null && !isSelectable(d)) {
                        setToolTipText(getDisabledReason(d));
                    } else {
                        setToolTipText(null);
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverDate = null;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    LocalDate d = dateAt(e.getPoint());
                    if (d == null || !isSelectable(d)) return;

                    if (rangeStart == null || (rangeStart != null && rangeEnd != null)) {
                        // First click = start
                        setRange(d, null);
                        rangeEnd = null;
                    } else {
                        // Second click = end
                        setRange(rangeStart, d);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void initKeyboard() {
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (focusedDate == null) focusedDate = LocalDate.now();
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT -> focusedDate = focusedDate.minusDays(1);
                        case KeyEvent.VK_RIGHT -> focusedDate = focusedDate.plusDays(1);
                        case KeyEvent.VK_UP -> focusedDate = focusedDate.minusWeeks(1);
                        case KeyEvent.VK_DOWN -> focusedDate = focusedDate.plusWeeks(1);
                        case KeyEvent.VK_ENTER -> {
                            if (isSelectable(focusedDate)) {
                                if (rangeStart == null || (rangeStart != null && rangeEnd != null)) {
                                    setRange(focusedDate, null);
                                    rangeEnd = null;
                                } else {
                                    setRange(rangeStart, focusedDate);
                                }
                            }
                        }
                        case KeyEvent.VK_ESCAPE -> setRange(null, null);
                    }
                    currentMonth = YearMonth.from(focusedDate);
                    repaint();
                }
            });
        }

        private String getDisabledReason(LocalDate d) {
            LocalDate today = LocalDate.now();
            if (d.isAfter(today)) return "Future date (not selectable)";
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return "Weekend (no classes)";
            long daysAgo = ChronoUnit.DAYS.between(d, today);
            if (daysAgo > MedicalPolicy.WINDOW_DAYS) return "Past policy window (" + MedicalPolicy.WINDOW_DAYS + " days)";
            return null;
        }

        private LocalDate dateAt(Point p) {
            int topY = 28;
            if (p.y < topY) return null;
            int col = p.x / CELL_SIZE;
            int row = (p.y - topY) / CELL_SIZE;
            if (col < 0 || col >= 7 || row < 0 || row >= 6) return null;

            LocalDate firstOfMonth = currentMonth.atDay(1);
            int dayOffset = (firstOfMonth.getDayOfWeek().getValue() - 1); // Mon=0
            int index = row * 7 + col;
            return firstOfMonth.plusDays(index - dayOffset);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);

            int w = getWidth();
            LocalDate today = LocalDate.now();

            // 1. Weekday headers
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(Theme.TEXT_MUTED);
            for (int i = 0; i < 7; i++) {
                int x = i * CELL_SIZE;
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (CELL_SIZE - fm.stringWidth(WEEKDAYS[i])) / 2;
                g2.drawString(WEEKDAYS[i], tx, 16);
            }

            // 2. Days grid
            int topY = 24;
            LocalDate firstOfMonth = currentMonth.atDay(1);
            int dayOffset = (firstOfMonth.getDayOfWeek().getValue() - 1);

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 7; c++) {
                    int index = r * 7 + c;
                    LocalDate d = firstOfMonth.plusDays(index - dayOffset);
                    int x = c * CELL_SIZE;
                    int y = topY + r * CELL_SIZE;

                    boolean isCurrentMonth = d.getMonth() == currentMonth.getMonth();
                    boolean selectable = isSelectable(d);
                    boolean isAbsent = absentDates.contains(d);

                    // Range preview/actual
                    LocalDate effectiveStart = rangeStart;
                    LocalDate effectiveEnd = rangeEnd != null ? rangeEnd : (rangeStart != null ? hoverDate : null);
                    if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
                        LocalDate temp = effectiveStart;
                        effectiveStart = effectiveEnd;
                        effectiveEnd = temp;
                    }

                    boolean isStart = (rangeStart != null && d.equals(rangeStart));
                    boolean isEnd = (rangeEnd != null && d.equals(rangeEnd));
                    boolean inRange = (effectiveStart != null && effectiveEnd != null &&
                            !d.isBefore(effectiveStart) && !d.isAfter(effectiveEnd));

                    // Paint range band
                    if (inRange && selectable) {
                        g2.setColor(new Color(239, 246, 255)); // Light primary tint band
                        g2.fillRect(x, y + 4, CELL_SIZE, CELL_SIZE - 8);
                    }

                    // Paint start & end circles
                    if ((isStart || isEnd) && selectable) {
                        g2.setColor(Theme.PRIMARY);
                        g2.fillOval(x + 4, y + 4, CELL_SIZE - 8, CELL_SIZE - 8);
                    } else if (d.equals(today)) {
                        // Today thin ring
                        g2.setColor(Theme.PRIMARY);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                    } else if (d.equals(focusedDate)) {
                        g2.setColor(new Color(203, 213, 225));
                        g2.drawOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                    }

                    // Number text
                    g2.setFont(Theme.FONT_BODY);
                    if ((isStart || isEnd) && selectable) {
                        g2.setColor(Color.WHITE);
                    } else if (!selectable || !isCurrentMonth) {
                        g2.setColor(new Color(203, 213, 225)); // Greyed
                    } else {
                        g2.setColor(Theme.TEXT_MAIN);
                    }

                    String dayStr = String.valueOf(d.getDayOfMonth());
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = x + (CELL_SIZE - fm.stringWidth(dayStr)) / 2;
                    int ty = y + (CELL_SIZE + fm.getAscent() - fm.getDescent()) / 2 - 2;
                    g2.drawString(dayStr, tx, ty);

                    // Red dot under absent dates
                    if (isAbsent && isCurrentMonth) {
                        g2.setColor(Theme.DANGER);
                        g2.fillOval(x + CELL_SIZE / 2 - 2, y + CELL_SIZE - 7, 4, 4);
                    }
                }
            }

            g2.dispose();
        }
    }
}
