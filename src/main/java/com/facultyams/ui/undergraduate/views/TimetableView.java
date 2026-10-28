package com.facultyams.ui.views;

import com.facultyams.controller.timetable.TimetableController;
import com.facultyams.model.timetable.*;
import com.facultyams.service.timetable.*;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;

/** Undergraduate timetable view. Schedule rules and persistence are delegated to its controller. */
public class TimetableView extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private final FileTimetableService timetableService = new FileTimetableService();
    private final TimetableController controller = new TimetableController(timetableService, new PdfScheduleExporter());
    private final ScheduleListPanel timetableContainer = new ScheduleListPanel();
    private final List<Reminder> reminders = new ArrayList<>();
    private final List<PillTab> dayButtons = new ArrayList<>();
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All types", "Lecture", "Practical", "Tutorial"});
    private final JPanel dayTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_2, 0));
    private final JPanel filterHintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel filterHint = new JLabel();
    private final RoundedPanel viewingDateChip = new RoundedPanel(10, Theme.PRIMARY_LIGHT);
    private final JLabel viewingDateText = new JLabel();
    private JButton calendarButton;
    private LocalDate weekStart;
    private LocalDate viewingDate;
    private final javax.swing.Timer clockTimer;
    private javax.swing.Timer scrollTimer;
    private javax.swing.Timer scheduleTransition;
    private ThinScrollBarUI scrollBarUI;
    private int scrollTarget;
    private DayOfWeek selectedDay;
    private boolean allDays;
    private String searchQuery = "";
    private IntConsumer notificationCountListener = count -> {};
    private Runnable reminderPanelAction = () -> {};
    private Runnable clearSearchAction = () -> {};

    public TimetableView() {
        setOpaque(true);
        setBackground(Theme.TIMETABLE_BG);
        setLayout(new BorderLayout());
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        selectedDay = today.getValue() <= 5 ? today : DayOfWeek.MONDAY;
        weekStart = mondayOf(LocalDate.now());
        buildView();
        controller.getReminderService().addListener(reminder -> SwingUtilities.invokeLater(() -> receiveReminder(reminder)));
        clockTimer = new javax.swing.Timer(30_000, e -> renderTimetable());
        clockTimer.start();
    }

    /** "Department • Level x Semester y" of the logged-in student (blank parts are skipped). */
    private static String studentSubtitle() {
        try {
            var student = com.facultyams.service.DataService.getInstance().getCurrentStudent();
            List<String> parts = new ArrayList<>();
            if (student.getDepartment() != null && !student.getDepartment().isBlank()) parts.add(student.getDepartment());
            if (student.getCurrentSemester() != null && !student.getCurrentSemester().isBlank()) parts.add(student.getCurrentSemester());
            return parts.isEmpty() ? "Classes of your enrolled courses" : String.join(" \u2022 ", parts);
        } catch (RuntimeException ex) {
            return "Classes of your enrolled courses";
        }
    }

    public void setNotificationIntegration(IntConsumer countListener, Runnable panelAction) {
        notificationCountListener = countListener == null ? count -> {} : countListener;
        reminderPanelAction = panelAction == null ? () -> {} : panelAction;
        notificationCountListener.accept(unreadCount());
    }

    public void openReminderPanel() {
        reminderPanelAction.run();
    }

    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.trim();
        renderTimetable();
    }

    public void setClearSearchAction(Runnable action) {
        clearSearchAction = action == null ? () -> {} : action;
    }

    public void showReminders() {
        if (reminders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No reminders yet.", "Reminders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPanel content = new JPanel(new BorderLayout(0, 8));
        DefaultListModel<String> model = new DefaultListModel<>();
        reminders.forEach(r -> model.addElement((r.isRead() ? "" : "\u2022 ") + r.getMessage()
                + "   (" + r.getCreatedAt().format(DateTimeFormatter.ofPattern("h:mm a")) + ")"));
        JList<String> list = new JList<>(model);
        list.setFont(Theme.FONT_BODY);
        content.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton settings = new JButton("Reminder settings");
        settings.addActionListener(e -> showReminderSettings());
        JButton markRead = new JButton("Mark all as read");
        markRead.addActionListener(e -> {
            reminders.forEach(Reminder::markRead);
            updateNotificationCount();
            Window window = SwingUtilities.getWindowAncestor(content);
            if (window != null) window.dispose();
        });
        actions.add(settings);
        actions.add(markRead);
        content.add(actions, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, content, "Recent reminders", JOptionPane.PLAIN_MESSAGE);
    }

    private void buildView() {
        RoundedPanel header = new RoundedPanel(12);
        header.setLayout(new BorderLayout(16, 0));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.TIMETABLE_CARD_BORDER),
                BorderFactory.createEmptyBorder(Theme.SPACE_2, Theme.CARD_PADDING,
                        Theme.SPACE_2, Theme.CARD_PADDING)));
        header.setPreferredSize(new Dimension(0, 88));
        header.setMinimumSize(new Dimension(0, 88));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Weekly Academic Timetable");
        title.setFont(Theme.TIMETABLE_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        JLabel subtitle = new JLabel(studentSubtitle());
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.TEXT_MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(subtitle);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        viewingDateChip.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        viewingDateChip.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));
        viewingDateText.setFont(Theme.FONT_BODY_BOLD);
        viewingDateText.setForeground(Theme.PRIMARY);
        JButton closeDate = new JButton(new TimetableIcon(TimetableIcon.Shape.CLOSE, Theme.PRIMARY, 12));
        closeDate.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        closeDate.setContentAreaFilled(false);
        closeDate.setFocusPainted(false);
        closeDate.setToolTipText("Return to today");
        closeDate.addActionListener(e -> resetViewingDate());
        viewingDateChip.add(viewingDateText);
        viewingDateChip.add(closeDate);
        viewingDateChip.setVisible(false);
        headerActions.add(viewingDateChip);
        JButton addEvent = new FlatButton("+ Add Event", true);
        addEvent.addActionListener(e -> showEventDialog(null));
        JButton reminderSettings = new FlatButton("Reminder settings", false);
        reminderSettings.addActionListener(e -> showReminderSettings());
        JButton export = new FlatButton("Export PDF", false);
        export.addActionListener(e -> exportSchedule());
        headerActions.add(addEvent);
        headerActions.add(reminderSettings);
        headerActions.add(export);
        header.add(heading, BorderLayout.CENTER);
        header.add(headerActions, BorderLayout.EAST);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Theme.PAGE_PADDING + 8));
        top.add(header);
        top.add(Box.createVerticalStrut(Theme.CARD_GAP));

        JPanel controls = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        controls.setOpaque(false);
        controls.setPreferredSize(new Dimension(0, Theme.CONTROL_HEIGHT));
        controls.setMinimumSize(new Dimension(0, Theme.CONTROL_HEIGHT));
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.CONTROL_HEIGHT));
        dayTabs.setOpaque(false);
        rebuildDayTabs();
        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_2, 0));
        leftTools.setOpaque(false);
        JButton previousWeek = compactArrow(false);
        previousWeek.setToolTipText("Previous week");
        previousWeek.addActionListener(e -> moveWeek(-1));
        JButton nextWeek = compactArrow(true);
        nextWeek.setToolTipText("Next week");
        nextWeek.addActionListener(e -> moveWeek(1));
        JButton todayButton = new FlatButton("Today", false);
        todayButton.addActionListener(e -> resetViewingDate());
        calendarButton = new JButton(new TimetableIcon(TimetableIcon.Shape.CALENDAR, Theme.PRIMARY, 17)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        calendarButton.setToolTipText("Open calendar");
        calendarButton.setPreferredSize(new Dimension(36, 36));
        calendarButton.setBorder(BorderFactory.createLineBorder(Theme.CONTROL_BORDER));
        calendarButton.setBackground(Color.WHITE);
        calendarButton.setFocusPainted(false);
        calendarButton.setContentAreaFilled(false);
        calendarButton.addActionListener(e -> showCalendarPopup());
        leftTools.add(previousWeek);
        leftTools.add(dayTabs);
        leftTools.add(nextWeek);
        leftTools.add(todayButton);
        leftTools.add(calendarButton);
        controls.add(leftTools, BorderLayout.CENTER);
        typeFilter.setUI(new RoundedComboBoxUI());
        typeFilter.setRenderer(new TypeFilterRenderer());
        typeFilter.setPreferredSize(new Dimension(140, Theme.CONTROL_HEIGHT));
        typeFilter.setMinimumSize(new Dimension(140, Theme.CONTROL_HEIGHT));
        typeFilter.setMaximumSize(new Dimension(140, Theme.CONTROL_HEIGHT));
        typeFilter.addActionListener(e -> renderTimetable());
        controls.add(typeFilter, BorderLayout.EAST);
        top.add(controls);
        filterHintPanel.setOpaque(false);
        filterHint.setFont(Theme.FONT_SMALL);
        filterHint.setForeground(Theme.TEXT_MUTED);
        JButton clearFilters = new JButton("Clear filters");
        clearFilters.setFont(Theme.FONT_SMALL);
        clearFilters.setForeground(Theme.PRIMARY);
        clearFilters.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        clearFilters.setContentAreaFilled(false);
        clearFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFilters.addActionListener(e -> clearFilters());
        filterHintPanel.add(filterHint);
        filterHintPanel.add(clearFilters);
        filterHintPanel.setVisible(false);
        top.add(filterHintPanel);
        top.add(Box.createVerticalStrut(Theme.SPACE_2));
        add(top, BorderLayout.NORTH);

        timetableContainer.setOpaque(false);
        timetableContainer.setLayout(new BoxLayout(timetableContainer, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(timetableContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scrollBarUI = new ThinScrollBarUI();
        scroll.getVerticalScrollBar().setUI(scrollBarUI);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        scroll.addMouseWheelListener(event -> smoothScroll(event, scroll));
        add(scroll, BorderLayout.CENTER);
        renderTimetable();
    }

    private void selectDay(DayOfWeek day) {
        selectedDay = day;
        allDays = false;
        viewingDate = null;
        updateDayButtons();
        renderTimetable();
        animateScheduleReveal();
    }

    private void selectDate(LocalDate date) {
        viewingDate = date;
        weekStart = mondayOf(date);
        selectedDay = date.getDayOfWeek();
        allDays = false;
        rebuildDayTabs();
        updateViewingDateChip();
        renderTimetable();
        animateScheduleReveal();
    }

    private void rebuildDayTabs() {
        dayTabs.removeAll();
        dayButtons.clear();
        DayOfWeek[] weekdays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        for (DayOfWeek day : weekdays) {
            LocalDate date = weekStart.plusDays(day.getValue() - 1L);
            String label = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth();
            boolean selected = !allDays && selectedDay == day;
            PillTab tab = new PillTab(label, selected, date.equals(LocalDate.now()));
            tab.addActionListener(e -> selectDay(day));
            dayButtons.add(tab);
            dayTabs.add(tab);
        }
        PillTab allDaysTab = new PillTab("All Days", allDays, false);
        allDaysTab.addActionListener(e -> {
            viewingDate = null;
            allDays = true;
            updateViewingDateChip();
            updateDayButtons();
            renderTimetable();
            animateScheduleReveal();
        });
        dayButtons.add(allDaysTab);
        dayTabs.add(allDaysTab);
        dayTabs.revalidate();
        dayTabs.repaint();
    }

    private void updateDayButtons() {
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        for (int i = 0; i < 5; i++) dayButtons.get(i).setSelectedState(!allDays && selectedDay == days[i]);
        ((PillTab) dayButtons.get(5)).setSelectedState(allDays);
    }

    private void moveWeek(int amount) {
        if (viewingDate != null) viewingDate = viewingDate.plusWeeks(amount);
        weekStart = weekStart.plusWeeks(amount);
        rebuildDayTabs();
        updateViewingDateChip();
        renderTimetable();
        animateScheduleReveal();
    }

    private void resetViewingDate() {
        viewingDate = null;
        allDays = false;
        weekStart = mondayOf(LocalDate.now());
        selectedDay = LocalDate.now().getDayOfWeek().getValue() <= 5
                ? LocalDate.now().getDayOfWeek() : DayOfWeek.MONDAY;
        rebuildDayTabs();
        updateViewingDateChip();
        renderTimetable();
    }

    private void updateViewingDateChip() {
        if (viewingDate == null) {
            viewingDateChip.setVisible(false);
        } else {
            viewingDateText.setText("Viewing: " + viewingDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")));
            viewingDateChip.setVisible(true);
        }
        revalidate();
    }

    private void showCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        MiniCalendar calendar = new MiniCalendar(controller.getService(),
                viewingDate == null ? weekStart.plusDays(selectedDay.getValue() - 1L) : viewingDate,
                date -> {
                    selectDate(date);
                    popup.setVisible(false);
                });
        popup.add(calendar);
        popup.show(calendarButton, 0, calendarButton.getHeight() + 4);
    }

    private JButton compactArrow(boolean right) {
        JButton button = new JButton(new WeekArrowIcon(right)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.BUTTON_RADIUS, Theme.BUTTON_RADIUS);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setPreferredSize(new Dimension(32, Theme.CONTROL_HEIGHT));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBackground(new Color(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setToolTipText(right ? "Next week" : "Previous week");
        return button;
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static final class WeekArrowIcon implements Icon {
        private final boolean right;
        private WeekArrowIcon(boolean right) { this.right = right; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Theme.TEXT_MUTED);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int direction = right ? 1 : -1;
            g.drawLine(x + 8 - 3 * direction, y + 4, x + 8 + 2 * direction, y + 8);
            g.drawLine(x + 8 + 2 * direction, y + 8, x + 8 - 3 * direction, y + 12);
            g.dispose();
        }
    }

    private static String formatLongDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH));
    }

    private void renderTimetable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::renderTimetable);
            return;
        }
        timetableContainer.removeAll();
        try {
            renderSessions();
        } catch (RuntimeException ex) {
            addLoadError(ex.getMessage() == null ? "Your timetable could not be loaded." : ex.getMessage());
        }
        timetableContainer.revalidate();
        timetableContainer.repaint();
    }

    /** Reloads the timetable from the database (called when the screen is opened). */
    public void reload() {
        renderTimetable();
    }

    private void addLoadError(String message) {
        RoundedPanel box = new RoundedPanel(12);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
        JLabel label = new JLabel(message);
        label.setFont(Theme.FONT_BODY);
        label.setForeground(Theme.ACCENT_ROSE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton retry = new JButton("Retry");
        retry.setForeground(Theme.PRIMARY);
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(e -> renderTimetable());
        box.add(label);
        box.add(Box.createVerticalStrut(Theme.SPACE_2));
        box.add(retry);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        timetableContainer.add(box);
    }

    private void addClashWarning(List<ClassSession> sessions) {
        java.util.Set<String> clashIds = timetableService.findAcademicClashIds();
        java.util.Set<String> codes = new java.util.TreeSet<>();
        for (ClassSession s : sessions) {
            if (clashIds.contains(s.getId())) codes.add(s.getCourse().getCode());
        }
        if (codes.isEmpty()) return;
        JLabel warning = new JLabel("Timetable clash: " + String.join(", ", codes)
                + " overlap in time. Please contact your department office.");
        warning.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        warning.setForeground(Theme.ACCENT_ROSE);
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);
        warning.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        timetableContainer.add(warning);
    }

    private void renderSessions() {
        String selectedType = (String) typeFilter.getSelectedItem();
        ClassType type = switch (selectedType == null ? "" : selectedType) {
            case "Lecture" -> ClassType.LECTURE;
            case "Practical" -> ClassType.PRACTICAL;
            case "Tutorial" -> ClassType.TUTORIAL;
            default -> null;
        };
        List<ClassSession> sessions;
        if (viewingDate != null) {
            sessions = controller.findSessionsForDate(searchQuery, type, viewingDate);
        } else if (allDays) {
            sessions = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate date = weekStart.plusDays(i);
                sessions.addAll(controller.findSessionsForDate(searchQuery, type, date));
            }
        } else {
            LocalDate date = weekStart.plusDays(selectedDay.getValue() - 1L);
            sessions = controller.findSessionsForDate(searchQuery, type, date);
        }
        updateFilterHint(selectedType);
        addClashWarning(sessions);
        if (allDays) {
            for (int i = 0; i < 5; i++) {
                LocalDate date = weekStart.plusDays(i);
                DayOfWeek day = date.getDayOfWeek();
                List<ClassSession> daily = sessions.stream().filter(s -> s.getDay() == day)
                        .filter(s -> s.getType() != ClassType.PERSONAL
                                || ((PersonalEvent) s).isRepeatWeekly() || ((PersonalEvent) s).getDate().equals(date))
                        .toList();
                if (daily.isEmpty()) continue;
                timetableContainer.add(Box.createVerticalStrut(24));
                addDayHeading(date);
                timetableContainer.add(Box.createVerticalStrut(Theme.SPACE_2));
                daily.forEach(session -> addSessionCard(session, date));
            }
        } else if (viewingDate != null) {
            addDayHeading(viewingDate);
            timetableContainer.add(Box.createVerticalStrut(Theme.SPACE_2));
            if (sessions.isEmpty()) addEmptyState();
            else sessions.forEach(session -> addSessionCard(session, viewingDate));
        } else {
            if (sessions.isEmpty()) {
                addEmptyState();
            } else {
                LocalDate selectedDate = weekStart.plusDays(selectedDay.getValue() - 1L);
                addDayHeading(selectedDate);
                timetableContainer.add(Box.createVerticalStrut(Theme.SPACE_2));
                sessions.forEach(session -> addSessionCard(session, selectedDate));
            }
        }
        if (sessions.isEmpty() && allDays) addEmptyState();
    }

    private void updateFilterHint(String selectedType) {
        boolean typeActive = selectedType != null && !selectedType.equals("All types");
        boolean searchActive = !searchQuery.isBlank();
        filterHintPanel.setVisible(typeActive || searchActive);
        if (!typeActive && !searchActive) return;
        StringBuilder hint = new StringBuilder("Filters: ");
        if (typeActive) hint.append(selectedType);
        if (typeActive && searchActive) hint.append(" \u2022 ");
        if (searchActive) hint.append("Search: \"").append(searchQuery).append('"');
        filterHint.setText(hint.toString());
    }

    private void clearFilters() {
        typeFilter.setSelectedIndex(0);
        searchQuery = "";
        clearSearchAction.run();
        updateFilterHint("All types");
        renderTimetable();
    }

    private void addDayHeading(LocalDate date) {
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH)));
        title.setFont(Theme.TIMETABLE_SECTION);
        boolean today = date.equals(LocalDate.now());
        boolean past = date.isBefore(LocalDate.now());
        title.setForeground(past ? Theme.TEXT_SUBTLE : Theme.TEXT_MAIN);
        heading.add(title);
        if (today) heading.add(new Badge("Today", Theme.PRIMARY_LIGHT, Theme.PRIMARY));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        timetableContainer.add(heading);
    }

    private void addSessionCard(ClassSession session, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        boolean pastDate = date.isBefore(now.toLocalDate());
        String status = controller.getStatus(session, date, now);
        String countdown = controller.getCountdown(session, status, date, now);
        TimetableCard card = new TimetableCard(session, status, countdown,
                controller.isReminderEnabled(session.getId()),
                pastDate,
                enabled -> {
                    try {
                        controller.setReminder(session.getId(), enabled);
                    } catch (RuntimeException ex) {
                        showError("Could not update reminder preference.", ex);
                    }
                },
                () -> controller.getService().getEvent(session.getId()).ifPresent(this::showEventDialog),
                () -> deleteEvent(session));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        timetableContainer.add(card);
        timetableContainer.add(Box.createVerticalStrut(Theme.CARD_GAP));
    }

    private void addEmptyState() {
        RoundedPanel empty = new RoundedPanel(12);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(BorderFactory.createEmptyBorder(28, 20, 28, 20));
        JLabel calendar = new JLabel(new TimetableIcon(TimetableIcon.Shape.CALENDAR, Theme.TEXT_SUBTLE, 28));
        calendar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel label = new JLabel(searchQuery.isBlank()
                ? "No classes scheduled. Classes of the courses you are enrolled in appear here."
                : "No timetable items match your search.");
        label.setFont(Theme.FONT_BODY);
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(calendar);
        empty.add(Box.createVerticalStrut(Theme.SPACE_2));
        empty.add(label);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        timetableContainer.add(empty);
    }

    private void showEventDialog(PersonalEvent existing) {
        LocalDate defaultDate = existing == null
                ? (viewingDate != null ? viewingDate : weekStart.plusDays(selectedDay.getValue() - 1L))
                : existing.getDate();
        LocalDate[] eventDate = {defaultDate};
        JLabel dateLabel = new JLabel(formatLongDate(eventDate[0]));
        JButton chooseDate = new FlatButton("Choose date", false);
        chooseDate.addActionListener(e -> {
            JPopupMenu pickerPopup = new JPopupMenu();
            MiniCalendar picker = new MiniCalendar(controller.getService(), eventDate[0], date -> {
                eventDate[0] = date;
                dateLabel.setText(formatLongDate(date));
                pickerPopup.setVisible(false);
            });
            pickerPopup.add(picker);
            pickerPopup.show(chooseDate, 0, chooseDate.getHeight());
        });
        JTextField title = new JTextField(existing == null ? "" : existing.getDisplayName(), 22);
        JTextField start = new JTextField(existing == null ? "09:00" : existing.getStart().format(TIME_FORMAT), 7);
        JTextField end = new JTextField(existing == null ? "10:00" : existing.getEnd().format(TIME_FORMAT), 7);
        JTextField venue = new JTextField(existing == null ? "" : existing.getVenue().getName(), 22);
        JTextField note = new JTextField(existing == null ? "" : existing.getNote(), 22);
        JCheckBox reminder = new JCheckBox("Remind me", existing == null || existing.isReminderEnabled());
        JCheckBox repeatWeekly = new JCheckBox("Repeat weekly", existing == null || existing.isRepeatWeekly());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Title")); form.add(title);
        form.add(new JLabel("Date")); form.add(dateLabel);
        form.add(new JLabel("")); form.add(chooseDate);
        form.add(new JLabel("Start (HH:mm)")); form.add(start);
        form.add(new JLabel("End (HH:mm)")); form.add(end);
        form.add(new JLabel("Venue")); form.add(venue);
        form.add(new JLabel("Note")); form.add(note);
        form.add(new JLabel("Reminder")); form.add(reminder);
        form.add(new JLabel("Repeat")); form.add(repeatWeekly);
        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add personal event" : "Edit personal event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            String eventTitle = title.getText().trim();
            String eventVenue = venue.getText().trim();
            if (eventTitle.isEmpty() || eventVenue.isEmpty()) throw new IllegalArgumentException("Title and venue are required.");
            LocalTime eventStart = LocalTime.parse(start.getText().trim(), TIME_FORMAT);
            LocalTime eventEnd = LocalTime.parse(end.getText().trim(), TIME_FORMAT);
            if (!eventEnd.isAfter(eventStart)) throw new IllegalArgumentException("End time must be after start time.");
            if (existing == null) {
                PersonalEvent event = controller.newEvent(eventTitle, eventDate[0], eventStart, eventEnd,
                        eventVenue, note.getText(), reminder.isSelected(), repeatWeekly.isSelected());
                List<ClassSession> clashes = controller.findClashesForDate(event);
                if (!clashes.isEmpty() && JOptionPane.showConfirmDialog(this,
                        "This event overlaps with " + clashes.stream().map(ClassSession::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("")
                                + ". Save it anyway?",
                        "Schedule clash", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
                controller.saveEvent(event);
            } else {
                PersonalEvent updated = controller.previewEvent(existing.getId(), eventTitle, eventDate[0], eventStart,
                        eventEnd, eventVenue, note.getText(), reminder.isSelected(), repeatWeekly.isSelected());
                List<ClassSession> clashes = controller.findClashesForDate(updated);
                if (!clashes.isEmpty() && JOptionPane.showConfirmDialog(this,
                        "This event overlaps with " + clashes.stream().map(ClassSession::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("")
                                + ". Save it anyway?",
                        "Schedule clash", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
                controller.updateEvent(existing.getId(), eventTitle, eventDate[0], eventStart, eventEnd,
                        eventVenue, note.getText(), reminder.isSelected(), repeatWeekly.isSelected());
            }
            renderTimetable();
            if (viewingDate != null) selectDate(viewingDate);
        } catch (RuntimeException ex) {
            showError("Please check the event details. Use a 24-hour time such as 09:30.", ex);
        }
    }

    private void deleteEvent(ClassSession session) {
        if (JOptionPane.showConfirmDialog(this, "Delete personal event \"" + session.getDisplayName() + "\"?",
                "Delete event", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            controller.deleteEvent(session.getId());
            renderTimetable();
        } catch (RuntimeException ex) {
            showError("Could not delete this event.", ex);
        }
    }

    private void showReminderSettings() {
        JComboBox<ReminderOffset> offsets = new JComboBox<>(ReminderOffset.values());
        offsets.setSelectedItem(controller.getReminderOffset());
        if (JOptionPane.showConfirmDialog(this, offsets, "Reminder lead time", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                controller.setReminderOffset((ReminderOffset) offsets.getSelectedItem());
            } catch (RuntimeException ex) {
                showError("Could not save reminder settings.", ex);
            }
        }
    }

    private void receiveReminder(Reminder reminder) {
        reminders.add(0, reminder);
        while (reminders.size() > 20) reminders.remove(reminders.size() - 1);
        updateNotificationCount();
        Object[] options = {"Snooze 5 min", "Dismiss"};
        int selected = JOptionPane.showOptionDialog(this, reminder.getMessage(), "Class reminder",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
        if (selected == 0) controller.snooze(reminder.getSessionId());
    }

    private int unreadCount() {
        return (int) reminders.stream().filter(r -> !r.isRead()).count();
    }
    private void updateNotificationCount() { notificationCountListener.accept(unreadCount()); }

    private void exportSchedule() {
        String selectedType = (String) typeFilter.getSelectedItem();
        ClassType type = switch (selectedType == null ? "" : selectedType) {
            case "Lecture" -> ClassType.LECTURE;
            case "Practical" -> ClassType.PRACTICAL;
            case "Tutorial" -> ClassType.TUTORIAL;
            default -> null;
        };
        List<ClassSession> sessions = new ArrayList<>();
        if (viewingDate != null) {
            sessions.addAll(controller.findSessionsForDate(searchQuery, type, viewingDate));
        } else if (allDays) {
            for (int i = 0; i < 5; i++) {
                sessions.addAll(controller.findSessionsForDate(searchQuery, type, weekStart.plusDays(i)));
            }
        } else {
            sessions.addAll(controller.findSessionsForDate(searchQuery, type,
                    weekStart.plusDays(selectedDay.getValue() - 1L)));
        }
        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no timetable items to export.", "Export schedule", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export timetable as PDF");
        chooser.setSelectedFile(new java.io.File("timetable-" + (allDays ? "week" : selectedDay.name().toLowerCase(Locale.ROOT)) + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) destination = Path.of(destination + ".pdf");
        Path target = destination;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { controller.export(sessions, target); return null; }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(TimetableView.this, "Schedule exported to:\n" + target,
                            "Export complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Could not export the PDF schedule.", ex.getCause() == null ? ex : ex.getCause());
                }
            }
        }.execute();
    }

    private void animateScheduleReveal() {
        if (scheduleTransition != null) scheduleTransition.stop();
        timetableContainer.setRevealAlpha(0f);
        scheduleTransition = new javax.swing.Timer(15, null);
        scheduleTransition.addActionListener(event -> {
            float alpha = Math.min(1f, timetableContainer.getRevealAlpha() + 0.15f);
            timetableContainer.setRevealAlpha(alpha);
            if (alpha >= 1f) scheduleTransition.stop();
        });
        scheduleTransition.start();
    }

    private void smoothScroll(java.awt.event.MouseWheelEvent event, JScrollPane scroll) {
        event.consume();
        JScrollBar bar = scroll.getVerticalScrollBar();
        int max = Math.max(bar.getMinimum(), bar.getMaximum() - bar.getVisibleAmount());
        if (scrollTimer == null || !scrollTimer.isRunning()) scrollTarget = bar.getValue();
        scrollTarget = Math.max(bar.getMinimum(), Math.min(max, scrollTarget
                + event.getWheelRotation() * 24 * Math.max(1, event.getScrollAmount())));
        scrollBarUI.reveal();
        if (scrollTimer != null) scrollTimer.stop();
        scrollTimer = new javax.swing.Timer(12, tick -> {
            int current = bar.getValue();
            int distance = scrollTarget - current;
            if (Math.abs(distance) <= 1) {
                bar.setValue(scrollTarget);
                scrollTimer.stop();
            } else {
                bar.setValue(current + Math.max(1, Math.abs(distance) / 4) * Integer.signum(distance));
            }
        });
        scrollTimer.start();
    }

    private void showError(String message, Throwable cause) {
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        JOptionPane.showMessageDialog(this, message + "\n" + detail, "Timetable error", JOptionPane.ERROR_MESSAGE);
    }

    private static final class TypeFilterRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                 boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            label.setFont(Theme.FONT_BODY);
            label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            label.setForeground(selected ? Color.WHITE : Theme.TEXT_MAIN);
            label.setBackground(selected ? Theme.PRIMARY : Color.WHITE);
            return label;
        }
    }

    private static final class ScheduleListPanel extends JPanel implements Scrollable {
        private float revealAlpha = 1f;

        private ScheduleListPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.PAGE_PADDING, Theme.PAGE_PADDING));
        }

        private float getRevealAlpha() { return revealAlpha; }
        private void setRevealAlpha(float alpha) {
            revealAlpha = alpha;
            repaint();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(700, 420); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.SrcOver.derive(revealAlpha));
            super.paintChildren(g);
            g.dispose();
        }
    }
}
