package com.facultyams.ui.components;

import com.facultyams.model.timetable.ClassSession;
import com.facultyams.model.timetable.ClassType;
import com.facultyams.service.timetable.TimetableService;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Lightweight java.time month picker with asynchronously calculated class/event dots. */
public final class MiniCalendar extends RoundedPanel {
    private final TimetableService service;
    private final java.util.function.Consumer<LocalDate> dateSelected;
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
    private final JLabel monthTitle = new JLabel("", SwingConstants.CENTER);
    private final Map<LocalDate, DaySummary> summaries = new HashMap<>();
    private YearMonth month;
    private LocalDate selectedDate;
    private SwingWorker<Map<LocalDate, DaySummary>, Void> summaryWorker;

    public MiniCalendar(TimetableService service, LocalDate selectedDate,
                        java.util.function.Consumer<LocalDate> dateSelected) {
        super(14);
        this.service = service;
        this.selectedDate = selectedDate;
        this.month = YearMonth.from(selectedDate);
        this.dateSelected = dateSelected;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.TIMETABLE_CARD_BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        setPreferredSize(new Dimension(300, 310));
        add(createHeader(), BorderLayout.NORTH);
        grid.setOpaque(false);
        add(grid, BorderLayout.CENTER);
        rebuildMonth();
    }

    public void setSelectedDate(LocalDate date) {
        selectedDate = date;
        month = YearMonth.from(date);
        rebuildMonth();
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JButton previous = arrowButton(false);
        JButton next = arrowButton(true);
        previous.addActionListener(e -> changeMonth(-1));
        next.addActionListener(e -> changeMonth(1));
        monthTitle.setFont(Theme.FONT_BODY_BOLD);
        monthTitle.setForeground(Theme.TEXT_MAIN);
        FlatButton today = new FlatButton("Today", false);
        today.setPreferredSize(new Dimension(68, 32));
        today.addActionListener(e -> selectDate(LocalDate.now()));
        JPanel nav = new JPanel(new BorderLayout(4, 0));
        nav.setOpaque(false);
        nav.add(previous, BorderLayout.WEST);
        nav.add(monthTitle, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        header.add(nav, BorderLayout.CENTER);
        header.add(today, BorderLayout.EAST);
        return header;
    }

    private JButton arrowButton(boolean right) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(28, 28));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setIcon(new ArrowIcon(right, Theme.TEXT_MUTED));
        return button;
    }

    private void changeMonth(int amount) {
        month = month.plusMonths(amount);
        rebuildMonth();
    }

    private void rebuildMonth() {
        monthTitle.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());
        grid.removeAll();
        for (DayOfWeek day : DayOfWeek.values()) {
            JLabel label = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), SwingConstants.CENTER);
            label.setFont(Theme.FONT_TINY.deriveFont(Font.BOLD));
            label.setForeground(Theme.TEXT_MUTED);
            grid.add(label);
        }
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) grid.add(new JLabel());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            DaySummary summary = summaries.get(date);
            CalendarDay button = new CalendarDay(date, summary, selectedDate != null && date.equals(selectedDate));
            button.setToolTipText(summary == null ? "Loading schedule..." : summary.tooltip());
            button.addActionListener(e -> selectDate(date));
            grid.add(button);
        }
        grid.revalidate();
        grid.repaint();
        loadSummaries(month);
    }

    private void loadSummaries(YearMonth targetMonth) {
        if (summaryWorker != null) summaryWorker.cancel(true);
        summaryWorker = new SwingWorker<>() {
            @Override protected Map<LocalDate, DaySummary> doInBackground() {
                Map<LocalDate, DaySummary> result = new HashMap<>();
                for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
                    if (isCancelled()) return result;
                    LocalDate date = targetMonth.atDay(day);
                    List<ClassSession> sessions = service.getSessionsForDate(date);
                    int classes = (int) sessions.stream().filter(s -> s.getType() != ClassType.PERSONAL).count();
                    int events = (int) sessions.stream().filter(s -> s.getType() == ClassType.PERSONAL).count();
                    int reminders = (int) sessions.stream().filter(s -> service.isReminderEnabled(s.getId())).count();
                    result.put(date, new DaySummary(classes, events, reminders));
                }
                return result;
            }
            @Override protected void done() {
                if (isCancelled() || !targetMonth.equals(month)) return;
                try {
                    summaries.putAll(get());
                    rebuildCellsOnly();
                } catch (Exception ex) {
                    throw new IllegalStateException("Could not load calendar schedule markers.", ex);
                }
            }
        };
        summaryWorker.execute();
    }

    private void rebuildCellsOnly() {
        Component[] components = grid.getComponents();
        int firstDay = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < components.length; i++) {
            if (i >= 7 + firstDay && components[i] instanceof CalendarDay cell) {
                DaySummary summary = summaries.get(cell.date);
                cell.summary = summary;
                cell.setToolTipText(summary == null ? "" : summary.tooltip());
                cell.repaint();
            }
        }
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        dateSelected.accept(date);
        rebuildMonth();
    }

    private final class CalendarDay extends JButton {
        private final LocalDate date;
        private DaySummary summary;
        private boolean chosen;

        private CalendarDay(LocalDate date, DaySummary summary, boolean chosen) {
            this.date = date;
            this.summary = summary;
            this.chosen = chosen;
            setPreferredSize(new Dimension(34, 34));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Theme.FONT_SMALL);
            boolean outside = !date.getMonth().equals(month.getMonth()) || date.getYear() != month.getYear();
            boolean semester = date.getYear() == LocalDate.now().getYear();
            boolean weekendWithoutClass = (date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY) && (summary == null || summary.classes == 0);
            setForeground(chosen ? Color.WHITE
                    : outside || !semester || weekendWithoutClass ? Theme.TEXT_SUBTLE : Theme.TEXT_MAIN);
            setText(Integer.toString(date.getDayOfMonth()));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(summary == null ? "Loading schedule..." : summary.tooltip());
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int d = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            if (chosen) {
                g.setColor(Theme.PRIMARY);
                g.fillOval(x, y, d, d);
            } else if (date.equals(LocalDate.now())) {
                g.setColor(Theme.PRIMARY);
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(x, y, d, d);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.PRIMARY_LIGHT);
                g.fillOval(x, y, d, d);
            }
            if (summary != null) {
                int dots = (summary.classes > 0 ? 1 : 0) + (summary.events > 0 ? 1 : 0) + (summary.reminders > 0 ? 1 : 0);
                int startX = getWidth() / 2 - dots * 3;
                int dotY = getHeight() - 4;
                if (summary.classes > 0) { g.setColor(Theme.PRIMARY); g.fillOval(startX, dotY, 3, 3); startX += 4; }
                if (summary.events > 0) { g.setColor(Theme.TYPE_PERSONAL); g.fillOval(startX, dotY, 3, 3); startX += 4; }
                if (summary.reminders > 0) { g.setColor(Theme.ACCENT_AMBER); g.fillOval(startX, dotY, 3, 3); }
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private record DaySummary(int classes, int events, int reminders) {
        private String tooltip() {
            return classes + (classes == 1 ? " class" : " classes") + ", " + events
                    + (events == 1 ? " event" : " events") + (reminders > 0 ? ", " + reminders + " reminders" : "");
        }
    }

    private static final class ArrowIcon implements Icon {
        private final boolean right;
        private final Color color;
        private ArrowIcon(boolean right, Color color) { this.right = right; this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int direction = right ? 1 : -1;
            int cx = x + 8;
            int cy = y + 8;
            g.drawLine(cx - 3 * direction, cy - 4, cx + 2 * direction, cy);
            g.drawLine(cx + 2 * direction, cy, cx - 3 * direction, cy + 4);
            g.dispose();
        }
    }
}
