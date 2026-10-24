package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.model.TimeTable;
import com.facultyams.service.CourseService;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.TimeTableService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Admin screen: timetable slots as a readable weekly grid (time x day) or as a list with Edit / Delete, filtered
 * by department, year (the course's academic level) and semester. Clicking a slot in the grid opens it for editing.
 * Clash checks and validation are in {@link TimeTableService}.
 */
public class TimeTableManagementPanel extends AbstractManagementPanel<TimeTable> {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Day", "Time", "Course Code", "Course Name", "Department", "Sem",
            "Lecture Hall", "Actions"};
    private static final int ACTIONS_COLUMN = 8;
    private static final String WEEK = "week";
    private static final String LIST = "table";

    private final transient TimeTableService timeTableService;
    private final transient DepartmentService departmentService;
    private final transient CourseService courseService;

    final JTextField searchField = searchField("Search timetable...");
    final JComboBox<Department> departmentFilter = new JComboBox<>();
    final JComboBox<Integer> yearFilter = new JComboBox<>(new Integer[] {null, 1, 2, 3, 4});
    final JComboBox<Integer> semesterFilter = new JComboBox<>(new Integer[] {null, 1, 2});
    private final WeekGrid grid = new WeekGrid(this::editSlot);
    private final JButton weekButton = new JButton();
    private final JButton listButton = new JButton();
    private boolean loadingFilter;

    public TimeTableManagementPanel() {
        this(new TimeTableService(), new DepartmentService(), new CourseService());
    }

    public TimeTableManagementPanel(TimeTableService timeTableService, DepartmentService departmentService,
                                    CourseService courseService) {
        super("slot", COLUMNS);
        this.timeTableService = timeTableService;
        this.departmentService = departmentService;
        this.courseService = courseService;

        departmentFilter.setRenderer(nullRenderer("All Departments"));
        departmentFilter.addItem(null);
        yearFilter.setRenderer(labelRenderer("All Years", "Year "));
        semesterFilter.setRenderer(labelRenderer("All Semesters", "Semester "));
        departmentFilter.getAccessibleContext().setAccessibleName("Filter by department");
        yearFilter.getAccessibleContext().setAccessibleName("Filter by year");
        semesterFilter.getAccessibleContext().setAccessibleName("Filter by semester");
        for (JComboBox<?> filter : List.of(departmentFilter, yearFilter, semesterFilter)) {
            filter.addActionListener(e -> {
                if (!loadingFilter) {
                    refresh();
                }
            });
        }
        liveSearch(searchField, this::refresh);

        JPanel views = new JPanel(new GridLayout(1, 2, 4, 0));
        views.setOpaque(false);
        weekButton.setToolTipText("Weekly view");
        weekButton.getAccessibleContext().setAccessibleName("Weekly view");
        listButton.setToolTipText("List view");
        listButton.getAccessibleContext().setAccessibleName("List view");
        weekButton.addActionListener(e -> showView(WEEK));
        listButton.addActionListener(e -> showView(LIST));
        views.add(weekButton);
        views.add(listButton);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(views);
        actions.add(addButton("Add Timetable", this::addSlot));
        setToolbar(filterRow(110, new double[] {2, 1.5, 1, 1.4}, searchField, departmentFilter, yearFilter, semesterFilter), actions);

        addView(WEEK, grid.scroll());
        int[] widths = {0, 100, 125, 95, 160, 165, 55, 110};
        for (int i = 1; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        RowActions.install(table, ACTIONS_COLUMN, this::editRow, this::deleteRow);
        showView(WEEK);
    }

    private static DefaultListCellRenderer labelRenderer(String none, String prefix) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? none : prefix + value, index, selected,
                        focus);
            }
        };
    }

    private void showView(String view) {
        boolean week = WEEK.equals(view);
        style(weekButton, week, AdminIcons.Kind.GRID);
        style(listButton, !week, AdminIcons.Kind.LIST);
        setView(view);
    }

    /** The active view button is solid blue, the other one an outlined button. */
    private static void style(JButton b, boolean active, AdminIcons.Kind icon) {
        if (active) {
            AdminTheme.primary(b);
        } else {
            AdminTheme.outline(b);
        }
        b.setIcon(AdminIcons.of(icon, 16, active ? Color.WHITE : AdminTheme.TEXT));
        b.setBorder(BorderFactory.createEmptyBorder(8, 11, 8, 11));
        b.setSelected(active);
    }

    @Override
    protected void loadLookups() throws DatabaseException, AuthorizationException {
        Department keep = (Department) departmentFilter.getSelectedItem();
        loadingFilter = true;
        try {
            departmentFilter.removeAllItems();
            departmentFilter.addItem(null);
            for (Department d : departmentService.getAllDepartments()) {
                departmentFilter.addItem(d);
                if (keep != null && d.getDepartmentId() == keep.getDepartmentId()) {
                    departmentFilter.setSelectedItem(d);
                }
            }
        } finally {
            loadingFilter = false;
        }
    }

    @Override
    protected List<TimeTable> loadItems() throws DatabaseException, AuthorizationException {
        Department department = (Department) departmentFilter.getSelectedItem();
        Integer year = (Integer) yearFilter.getSelectedItem();
        List<TimeTable> slots = timeTableService.searchTimeTable(searchField.getText(),
                department == null ? null : department.getDepartmentId(), (Integer) semesterFilter.getSelectedItem());
        if (year == null) {
            return slots;
        }
        List<TimeTable> filtered = new ArrayList<>();
        for (TimeTable t : slots) {
            if (t.getAcademicLevel() == year) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    @Override
    protected Object[] toRow(TimeTable t) {
        return new Object[] {code('T', t.getTimetableId()), TimeTableFormPanel.dayName(t.getDayOfWeek()),
                t.getTimeRange(), t.getCourseCode(), t.getCourseName(), t.getDepartmentName(), t.getSemester(),
                t.getVenue(), ""};
    }

    @Override
    protected void afterLoad(List<TimeTable> items) {
        grid.setSlots(items);
    }

    // ------------------------------------------------------------------ actions

    private void addSlot() {
        if (TimeTableFormPanel.showDialog(this, timeTableService, courseService, null)) {
            refresh();
            success("Timetable slot added.");
        }
    }

    private void editRow(int row) {
        editSlot(itemAt(row));
    }

    private void editSlot(TimeTable slot) {
        if (slot != null && TimeTableFormPanel.showDialog(this, timeTableService, courseService, slot)) {
            refresh();
            success("Timetable slot updated.");
        }
    }

    private void deleteRow(int row) {
        TimeTable slot = itemAt(row);
        if (slot == null || !UIUtil.confirm(this, "Delete timetable slot " + slot + "?")) {
            return;
        }
        try {
            timeTableService.deleteSlot(slot.getTimetableId());
            refresh();
            success("Timetable slot deleted.");
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    // ------------------------------------------------------------------ weekly grid

    /** Time rows x weekday columns; each slot is a small clickable chip showing course code and hall. */
    private static final class WeekGrid {
        private static final Color LINE = new Color(0xE6ECF5);
        private final ScrollablePanel panel = new ScrollablePanel(new GridBagLayout(), 720);
        private final JScrollPane scroll = new JScrollPane(panel);
        private final Consumer<TimeTable> onClick;

        WeekGrid(Consumer<TimeTable> onClick) {
            this.onClick = onClick;
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(Color.WHITE);
            scroll.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            AdminTheme.slimScroll(scroll);
        }

        JScrollPane scroll() {
            return scroll;
        }

        void setSlots(List<TimeTable> slots) {
            panel.removeAll();
            List<DayOfWeek> days = new ArrayList<>(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
            if (slots.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.SATURDAY)) {
                days.add(DayOfWeek.SATURDAY);
            }
            TreeSet<LocalTime[]> times = new TreeSet<>(
                    (a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
            slots.forEach(s -> times.add(new LocalTime[] {s.getStartTime(), s.getEndTime()}));

            header(0, "Time");
            for (int d = 0; d < days.size(); d++) {
                header(d + 1, TimeTableFormPanel.dayName(days.get(d)));
            }
            int row = 1;
            for (LocalTime[] range : times) {
                JLabel time = new JLabel(range[0] + " - " + range[1], JLabel.CENTER);
                time.setFont(AdminTheme.FONT_SMALL);
                time.setForeground(AdminTheme.TEXT_MUTED);
                cell(0, row, time);
                for (int d = 0; d < days.size(); d++) {
                    JPanel box = new JPanel(new GridLayout(0, 1, 0, 4));
                    box.setOpaque(false);
                    for (TimeTable s : slots) {
                        if (s.getDayOfWeek() == days.get(d) && s.getStartTime().equals(range[0])
                                && s.getEndTime().equals(range[1])) {
                            box.add(new Chip(s, onClick));
                        }
                    }
                    cell(d + 1, row, box);
                }
                row++;
            }
            panel.revalidate();
            panel.repaint();
        }

        private void header(int column, String text) {
            JLabel l = new JLabel(text, JLabel.CENTER);
            l.setFont(AdminTheme.FONT_BOLD.deriveFont(12f));
            l.setForeground(AdminTheme.TEXT_MUTED);
            l.setOpaque(true);
            l.setBackground(new Color(0xF3F6FB));
            l.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            place(l, column, 0);
        }

        private void cell(int column, int row, JComponent c) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, LINE),
                    BorderFactory.createEmptyBorder(8, 6, 8, 6)));
            wrap.add(c, BorderLayout.CENTER);
            place(wrap, column, row);
        }

        private void place(Component c, int column, int row) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = column;
            gc.gridy = row;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = column == 0 ? 0 : 1;
            gc.ipadx = column == 0 ? 60 : 0;
            panel.add(c, gc);
        }
    }

    /** One slot in the grid: rounded light-blue chip "ICT2132 / LT-01"; click to edit. */
    private static final class Chip extends JLabel {
        private static final long serialVersionUID = 1L;

        Chip(TimeTable slot, Consumer<TimeTable> onClick) {
            super("<html><div style='text-align:center'><b>" + escape(slot.getCourseCode()) + "</b><br>"
                    + escape(slot.getVenue()) + "</div></html>", CENTER);
            setForeground(AdminTheme.BLUE_DARK);
            setFont(AdminTheme.FONT_SMALL);
            setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
            setToolTipText(slot.getCourseName() + " - " + slot.getDepartmentName() + " (click to edit)");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.accept(slot);
                }
            });
        }

        private static String escape(String s) {
            return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AdminTheme.BLUE_LIGHT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}