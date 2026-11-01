package com.facultyams.ui.views;

import com.facultyams.model.student.StudentNotice;
import com.facultyams.model.student.StudentProfile;
import com.facultyams.model.student.StudentAssignment;
import com.facultyams.service.StudentAssignmentService;
import com.facultyams.service.StudentResultService.CourseResult;
import com.facultyams.service.StudentResultService.Summary;
import com.facultyams.service.DataService;
import com.facultyams.service.StudentResultService;
import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.theme.Theme;
import com.facultyams.util.Constants;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Modern, clean, minimal Dashboard UI matching the exact SRMS undergraduate portal design.
 */
public class DashboardView extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(DashboardView.class.getName());
    private final Consumer<String> navigationHandler;
    private BiConsumer<String, Long> assignmentNavigation = (courseCode, assignmentId) -> {};
    private BiConsumer<String, Long> submissionDetailsNavigation = (courseCode, assignmentId) -> {};
    private Runnable submissionsNavigation = () -> {};
    private final DataService dataService = DataService.getInstance();

    // Snapshot loaded from the database by refresh(); the screen is rebuilt from it.
    private StudentProfile profile;
    private Summary summary;
    private List<CourseResult> recentResults = new ArrayList<>();
    private List<StudentNotice> notices = new ArrayList<>();
    private List<CourseResult> allGradedResults = new ArrayList<>();
    private List<StudentAssignment> submissionDeadlines = new ArrayList<>();
    private String deadlineLoadError;
    private int unreadNotices;
    private String loadError;

    // Design Tokens
    private static final Color COLOR_TEXT_PRIMARY = Theme.TEXT_MAIN;
    private static final Color COLOR_TEXT_SECONDARY = Theme.TEXT_MUTED;
    private static final Color COLOR_TEXT_MUTED = Theme.TEXT_SUBTLE;
    private static final Color COLOR_PRIMARY = Theme.PRIMARY;
    private static final Color COLOR_PRIMARY_LIGHT = Theme.PRIMARY_LIGHT;
    private static final Color COLOR_SUCCESS = Theme.SUCCESS;
    private static final Color COLOR_SUCCESS_LIGHT = Theme.SUCCESS_BG;
    private static final Color COLOR_DANGER = Theme.DANGER;
    private static final Color COLOR_DANGER_LIGHT = Theme.DANGER_BG;
    private static final Color COLOR_WARNING = Theme.WARNING;
    private static final Color COLOR_WARNING_LIGHT = Theme.WARNING_BG;
    private static final Color COLOR_BORDER = Theme.CARD_BORDER;
    private static final Color COLOR_HOVER_BG = Theme.ENROLLMENT_ROW_HOVER;

    // Fonts
    private static final Font FONT_GREETING = Theme.FONT_TITLE;
    private static final Font FONT_SECTION = Theme.FONT_SECTION;
    private static final Font FONT_METRIC = Theme.FONT_METRIC;
    private static final Font FONT_BODY = Theme.FONT_BODY;
    private static final Font FONT_BODY_BOLD = Theme.FONT_BODY_BOLD;
    private static final Font FONT_CAPTION = Theme.FONT_SMALL;
    private static final Font FONT_CAP_BOLD = Theme.FONT_TINY;

    public DashboardView(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        setOpaque(false);
        setLayout(new BorderLayout());
        refresh();
    }

    public void setAssignmentNavigation(BiConsumer<String, Long> action,
                                        BiConsumer<String, Long> detailsAction,
                                        Runnable viewAllAction) {
        assignmentNavigation = action == null ? (courseCode, assignmentId) -> {} : action;
        submissionDetailsNavigation = detailsAction == null
                ? (courseCode, assignmentId) -> {} : detailsAction;
        submissionsNavigation = viewAllAction == null ? () -> {} : viewAllAction;
    }

    /** Reloads everything from the database (loading message first, then the live dashboard or an error). */
    public final void refresh() {
        removeAll();
        add(centeredMessage("Loading your dashboard…"), BorderLayout.CENTER);
        revalidate();
        repaint();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                dataService.refreshProfile();
                profile = dataService.getCurrentStudent();
                summary = StudentResultService.getInstance().getSummary();
                recentResults = StudentResultService.getInstance().getRecentResults(5);
                allGradedResults = StudentResultService.getInstance().getRecentResults(Integer.MAX_VALUE);
                notices = dataService.getNotices();
                unreadNotices = (int) notices.stream().filter(n -> !n.isRead()).count();
                try {
                    submissionDeadlines = new StudentAssignmentService().getAssignmentsForStudent(null);
                    deadlineLoadError = null;
                } catch (RuntimeException ex) {
                    submissionDeadlines = List.of();
                    deadlineLoadError = ex.getMessage() == null
                            ? "Submission deadline reminders could not be loaded." : ex.getMessage();
                    LOGGER.log(Level.SEVERE, "Could not load student assignment deadlines", ex);
                }
                return null;
            }

            @Override protected void done() {
                loadError = null;
                try {
                    get();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    loadError = cause.getMessage() == null ? "The dashboard could not be loaded." : cause.getMessage();
                }
                removeAll();
                if (loadError != null) {
                    add(centeredMessage(loadError), BorderLayout.CENTER);
                } else {
                    buildContent();
                }
                revalidate();
                repaint();
            }
        }.execute();
    }

    private JComponent centeredMessage(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(COLOR_TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }

    private void buildContent() {
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // 1. Welcome Greeting Header (clean, no illustration)
        mainContent.add(createWelcomeHeader());
        mainContent.add(Box.createVerticalStrut(24));

        // 2. Row 1: 4 KPI Metric Cards
        mainContent.add(createKpiCardsRow());
        mainContent.add(Box.createVerticalStrut(24));

        // 3. Row 2: Recent Results (65%) + Notices (35%)
        mainContent.add(createResultsAndNoticesRow());
        mainContent.add(Box.createVerticalStrut(24));

        // 4. Row 3: Performance Donut (35%) + Submission deadlines (65%)
        mainContent.add(createPerformanceAndUpcomingRow());

        // ScrollPane with smooth scrolling
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createWelcomeHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        StudentProfile student = profile;
        int hour = LocalTime.now().getHour();
        String timeGreeting = (hour < 12) ? "Good morning" : (hour < 17) ? "Good afternoon" : "Good evening";

        String firstName = "student";
        if (student != null && student.getFullName() != null) {
            firstName = student.getFullName().trim().split("\\s+")[0];
        }

        JLabel greetingLabel = new JLabel(timeGreeting + ", " + firstName);
        greetingLabel.setFont(FONT_GREETING);
        greetingLabel.setForeground(COLOR_TEXT_PRIMARY);

        JLabel subLabel = new JLabel("Here is a quick look at your academic progress.");
        subLabel.setFont(FONT_BODY);
        subLabel.setForeground(COLOR_TEXT_SECONDARY);

        header.add(greetingLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subLabel);

        return header;
    }

    private JPanel createKpiCardsRow() {
        StudentProfile s = profile;
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // 1. CGPA (project formula: sum(grade points x credits) / sum(credits))
        boolean hasResults = summary.hasResults();
        row.add(createKpiCard("CGPA", hasResults ? String.format(Locale.ROOT, "%.2f", summary.cgpa()) : "—",
                hasResults ? "Out of " + String.format(Locale.ROOT, "%.2f", Constants.MAX_GPA) + " scale"
                        : "No results published yet", hasResults ? COLOR_SUCCESS : COLOR_TEXT_SECONDARY));

        // 2. Attendance (eligibility needs 80%)
        if (s.hasAttendance()) {
            boolean ok = s.getAttendancePercentage() >= 80.0;
            row.add(createKpiCard("Attendance", String.format(Locale.ROOT, "%.0f%%", s.getAttendancePercentage()),
                    ok ? "Meets the 80% requirement" : "Below the 80% requirement", ok ? COLOR_SUCCESS : COLOR_DANGER));
        } else {
            row.add(createKpiCard("Attendance", "—", "No sessions recorded yet", COLOR_TEXT_SECONDARY));
        }

        // 3. Courses
        row.add(createKpiCard("Courses", String.valueOf(s.getEnrolledCoursesCount()),
                s.getEnrolledCredits() + " credits enrolled", COLOR_TEXT_SECONDARY));

        // 4. Unread notices
        row.add(createKpiCard("Unread Notices", String.valueOf(unreadNotices),
                unreadNotices == 0 ? "You are all caught up" : "Open the notice board",
                unreadNotices == 0 ? COLOR_SUCCESS : COLOR_WARNING));

        return row;
    }

    private JPanel createKpiCard(String label, String value, String subtext, Color subColor) {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(FONT_CAPTION);
        lblTitle.setForeground(COLOR_TEXT_SECONDARY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(FONT_METRIC);
        lblValue.setForeground(COLOR_TEXT_PRIMARY);

        JLabel lblSub = new JLabel(subtext);
        lblSub.setFont(FONT_CAP_BOLD);
        lblSub.setForeground(subColor);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(6));
        p.add(lblValue);
        p.add(Box.createVerticalStrut(6));
        p.add(lblSub);

        card.add(p, BorderLayout.CENTER);
        return card;
    }

    private JPanel createResultsAndNoticesRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Recent Results (65% width)
        gbc.gridx = 0;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 16);
        row.add(createRecentResultsCard(), gbc);

        // Notices (35% width)
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(createNoticesCard(), gbc);

        return row;
    }

    private JPanel createRecentResultsCard() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        // Header: "Recent Results" + "View all"
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Recent Results");
        title.setFont(FONT_SECTION);
        title.setForeground(COLOR_TEXT_PRIMARY);

        JLabel viewAll = createLink("View all", () -> navigationHandler.accept("Results"));
        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Results Table
        if (recentResults.isEmpty()) {
            card.add(centeredMessage("No results have been published for you yet."), BorderLayout.CENTER);
            return card;
        }
        String[] columns = {"Course", "Grade", "Overall", "Credits"};
        int count = Math.min(recentResults.size(), 5);
        Object[][] data = new Object[count][4];

        for (int i = 0; i < count; i++) {
            CourseResult m = recentResults.get(i);
            data[i][0] = m.code() + " · " + m.name();
            data[i][1] = m.grade();
            data[i][2] = m.scoreText() + " / " + m.maxScore();
            data[i][3] = m.credits();
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setBackground(Color.WHITE);
        table.setForeground(COLOR_TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setFillsViewportHeight(true);

        // Uppercase slate header
        JTableHeader th = table.getTableHeader();
        th.setPreferredSize(new Dimension(0, 32));
        th.setFont(FONT_CAPTION);
        th.setForeground(COLOR_TEXT_SECONDARY);
        th.setBackground(Color.WHITE);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        th.setReorderingAllowed(false);

        // Custom Grade Pill Renderer
        table.getColumnModel().getColumn(1).setCellRenderer((tbl, value, isSel, hasFocus, rowIdx, colIdx) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
            p.setOpaque(false);
            p.add(createGradePill(value != null ? value.toString() : ""));
            return p;
        });

        // Hover highlight
        final int[] hoveredRow = {-1};
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoveredRow[0]) {
                    hoveredRow[0] = r;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow[0] = -1;
                table.repaint();
            }
        });

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int r, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, false, r, col);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                setBackground(r == hoveredRow[0] ? COLOR_HOVER_BG : Color.WHITE);
                return c;
            }
        };

        table.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(cellRenderer);

        table.getColumnModel().getColumn(0).setPreferredWidth(210);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 240));

        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    private JLabel createGradePill(String grade) {
        String g = grade != null ? grade.trim().toUpperCase() : "";
        Color bg;
        Color fg;

        if (g.startsWith("A")) {
            bg = COLOR_SUCCESS_LIGHT;
            fg = COLOR_SUCCESS;
        } else if (g.startsWith("B")) {
            bg = COLOR_PRIMARY_LIGHT;
            fg = COLOR_PRIMARY;
        } else if (g.startsWith("C")) {
            bg = COLOR_WARNING_LIGHT;
            fg = COLOR_WARNING;
        } else {
            bg = COLOR_DANGER_LIGHT;
            fg = COLOR_DANGER;
        }

        JLabel pill = new JLabel(grade, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
                g2.dispose();
                super.paintComponent(g2d);
            }
        };
        pill.setFont(FONT_CAP_BOLD);
        pill.setForeground(fg);
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(38, 22));
        return pill;
    }

    private JPanel createNoticesCard() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        // Header: "Notices" + "View all"
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Notices");
        title.setFont(FONT_SECTION);
        title.setForeground(COLOR_TEXT_PRIMARY);

        JLabel viewAll = createLink("View all", () -> navigationHandler.accept("Notice Board"));
        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Notice list
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (notices.isEmpty()) {
            card.add(centeredMessage("There are no notices yet."), BorderLayout.CENTER);
            return card;
        }
        int max = Math.min(notices.size(), 3);
        for (int i = 0; i < max; i++) {
            StudentNotice n = notices.get(i);
            boolean isUnread = !n.isRead();
            list.add(createNoticeRow(n, isUnread));
            if (i < max - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(COLOR_BORDER);
                sep.setBackground(COLOR_BORDER);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                list.add(sep);
            }
        }
        list.add(Box.createVerticalGlue());

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createNoticeRow(StudentNotice n, boolean unread) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Dot indicator
        JPanel dotPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, 10);
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(unread ? COLOR_PRIMARY : COLOR_TEXT_MUTED);
                g2.fillOval(1, 4, 7, 7);
                g2.dispose();
            }
        };
        dotPanel.setOpaque(false);

        JPanel textP = new JPanel();
        textP.setOpaque(false);
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(FONT_BODY_BOLD);
        titleLbl.setForeground(COLOR_TEXT_PRIMARY);

        String preview = n.getContent();
        if (preview.length() > 42) {
            preview = preview.substring(0, 42) + "...";
        }
        JLabel descLbl = new JLabel(preview);
        descLbl.setFont(FONT_CAPTION);
        descLbl.setForeground(COLOR_TEXT_SECONDARY);

        JLabel dateLbl = new JLabel(n.getDate());
        dateLbl.setFont(FONT_CAP_BOLD);
        dateLbl.setForeground(COLOR_TEXT_MUTED);

        textP.add(titleLbl);
        textP.add(Box.createVerticalStrut(2));
        textP.add(descLbl);
        textP.add(Box.createVerticalStrut(3));
        textP.add(dateLbl);

        row.add(dotPanel, BorderLayout.WEST);
        row.add(textP, BorderLayout.CENTER);

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                titleLbl.setForeground(COLOR_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                titleLbl.setForeground(COLOR_TEXT_PRIMARY);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                navigationHandler.accept("Notice Board");
            }
        };

        row.addMouseListener(hover);
        titleLbl.addMouseListener(hover);
        descLbl.addMouseListener(hover);

        return row;
    }

    private JPanel createPerformanceAndUpcomingRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Performance Donut (35% width)
        gbc.gridx = 0;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 16);
        row.add(createPerformanceCard(), gbc);

        // Upcoming Deadlines (65% width)
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(createUpcomingDeadlinesCard(), gbc);

        return row;
    }

    private JPanel createPerformanceCard() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Performance");
        title.setFont(FONT_SECTION);
        title.setForeground(COLOR_TEXT_PRIMARY);

        JLabel detailsLink = createLink("Details", () -> navigationHandler.accept("Results"));
        header.add(title, BorderLayout.WEST);
        header.add(detailsLink, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        if (summary.hasResults()) {
            card.add(createDonutChartComponent(), BorderLayout.CENTER);
        } else {
            card.add(centeredMessage("Your grade mix appears once results are published."), BorderLayout.CENTER);
        }
        return card;
    }

    private JComponent createDonutChartComponent() {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(280, 150));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);

                int h = getHeight();
                int size = Math.min(130, h - 10);
                int cx = 14;
                int cy = (h - size) / 2;

                double outerRadius = size / 2.0;
                double innerRadius = outerRadius - 10;
                double centerX = cx + outerRadius;
                double centerY = cy + outerRadius;

                // Segments by grade points of the graded courses: >= 3.7 (A-range), 2.7-3.3 (B-range), below that
                int excellent = 0;
                int good = 0;
                int other = 0;
                for (CourseResult c : allGradedResults) {
                    double gp = c.gradePoint().doubleValue();
                    if (gp >= 3.7) excellent++;
                    else if (gp >= 2.7) good++;
                    else other++;
                }
                int total = Math.max(1, excellent + good + other);
                double startAngle = 90.0;
                double arcExcellent = excellent * 360.0 / total;
                double arcGood = good * 360.0 / total;
                double arcAverage = 360.0 - (arcExcellent + arcGood);

                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle, -arcExcellent, Theme.CHART_EXCELLENT);
                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle - arcExcellent, -arcGood, Theme.CHART_GOOD);
                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle - arcExcellent - arcGood, -arcAverage, Theme.CHART_AVERAGE);

                // Center text: 84% + Overall
                g2.setColor(COLOR_TEXT_PRIMARY);
                g2.setFont(FONT_GREETING);
                FontMetrics fm1 = g2.getFontMetrics();
                String pct = String.format(Locale.ROOT, "%.2f", summary.cgpa());
                int tx = (int) (centerX - fm1.stringWidth(pct) / 2);
                int ty = (int) (centerY + fm1.getAscent() / 2 - 6);
                g2.drawString(pct, tx, ty);

                g2.setColor(COLOR_TEXT_SECONDARY);
                g2.setFont(FONT_CAPTION);
                FontMetrics fm2 = g2.getFontMetrics();
                String lbl = "CGPA";
                int lx = (int) (centerX - fm2.stringWidth(lbl) / 2);
                int ly = ty + fm2.getAscent() + 2;
                g2.drawString(lbl, lx, ly);

                // Legend on Right
                int lxStart = cx + size + 20;
                int lyStart = cy + 24;

                drawLegendItem(g2, lxStart, lyStart, Theme.CHART_EXCELLENT, "A range", String.valueOf(excellent));
                drawLegendItem(g2, lxStart, lyStart + 30, Theme.CHART_GOOD, "B range", String.valueOf(good));
                drawLegendItem(g2, lxStart, lyStart + 60, Theme.CHART_AVERAGE, "Other", String.valueOf(other));

                g2.dispose();
            }

            private void drawSegment(Graphics2D g2, double cx, double cy, double outerR, double innerR, double start, double extent, Color color) {
                Arc2D.Double outerArc = new Arc2D.Double(cx - outerR, cy - outerR, outerR * 2, outerR * 2, start, extent, Arc2D.PIE);
                Area area = new Area(outerArc);
                Shape innerCircle = new Ellipse2D.Double(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
                area.subtract(new Area(innerCircle));
                g2.setColor(color);
                g2.fill(area);
            }

            private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String name, String value) {
                g2.setColor(color);
                g2.fillOval(x, y + 6, 6, 6);

                g2.setColor(COLOR_TEXT_PRIMARY);
                g2.setFont(FONT_BODY);
                g2.drawString(name, x + 14, y + 14);

                g2.setFont(FONT_BODY_BOLD);
                g2.drawString(value, x + 76, y + 14);
            }
        };
    }

    /** Upcoming deadlines and submitted work for the student's enrolled courses. */
    private JPanel createUpcomingDeadlinesCard() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Submission deadlines");
        title.setFont(FONT_SECTION);
        title.setForeground(COLOR_TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        header.add(createLink("View all", submissionsNavigation), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        if (deadlineLoadError != null) {
            card.add(centeredMessage("Deadline reminders unavailable: " + deadlineLoadError),
                    BorderLayout.CENTER);
            return card;
        }
        LocalDateTime now = LocalDateTime.now();
        List<StudentAssignment> dueAssignments = submissionDeadlines.stream()
                .filter(assignment -> assignment.dueAt() != null)
                .filter(assignment -> assignment.hasSubmission() || !"GRADED".equalsIgnoreCase(assignment.status()))
                .filter(assignment -> assignment.hasSubmission() || !now.isAfter(assignment.dueAt())
                        || assignment.allowLate() && !now.isAfter(assignment.cutoffAt() == null
                        ? assignment.dueAt() : assignment.cutoffAt()))
                .sorted(Comparator
                        .comparing((StudentAssignment assignment) -> assignment.hasSubmission())
                        .thenComparing(assignment -> assignment.hasSubmission()
                                        ? assignment.submittedAt() : assignment.dueAt(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .toList();
        if (dueAssignments.isEmpty()) {
            card.add(centeredMessage("No upcoming deadlines or submitted work."), BorderLayout.CENTER);
            return card;
        }
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        DateTimeFormatter deadlineFormat = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm");
        for (StudentAssignment assignment : dueAssignments) {
            JPanel line = new JPanel(new GridBagLayout());
            line.setOpaque(false);
            line.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                    BorderFactory.createEmptyBorder(10, 0, 10, 0)));
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, 16);

            String status = dashboardAssignmentStatus(assignment);
            JLabel deadlineLabel = new JLabel(status);
            deadlineLabel.setFont(FONT_CAP_BOLD);
            deadlineLabel.setForeground(switch (status) {
                case "Graded", "Submitted" -> COLOR_SUCCESS;
                case "Late" -> COLOR_WARNING;
                case "Overdue" -> COLOR_DANGER;
                default -> COLOR_PRIMARY;
            });
            deadlineLabel.setOpaque(true);
            deadlineLabel.setBackground(switch (status) {
                case "Graded", "Submitted" -> COLOR_SUCCESS_LIGHT;
                case "Late" -> COLOR_WARNING_LIGHT;
                case "Overdue" -> COLOR_DANGER_LIGHT;
                default -> COLOR_PRIMARY_LIGHT;
            });
            deadlineLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            c.gridx = 0;
            c.weightx = 0;
            line.add(deadlineLabel, c);

            JPanel description = new JPanel();
            description.setOpaque(false);
            description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
            JLabel assignmentLabel = new JLabel(assignment.title());
            assignmentLabel.setFont(FONT_BODY_BOLD);
            assignmentLabel.setForeground(COLOR_TEXT_PRIMARY);
            assignmentLabel.setToolTipText(assignment.title());
            JLabel courseLabel = new JLabel(assignment.courseCode() + " · " + assignment.courseName());
            courseLabel.setFont(FONT_CAPTION);
            courseLabel.setForeground(COLOR_TEXT_SECONDARY);
            String opened = assignment.openedAt() == null ? "Opened: Not provided"
                    : "Opened: " + assignment.openedAt().format(deadlineFormat);
            String deadline = "Due: " + assignment.dueAt().format(deadlineFormat);
            JLabel datesLabel = new JLabel(opened + "   ·   " + deadline);
            datesLabel.setFont(FONT_CAPTION);
            datesLabel.setForeground(COLOR_TEXT_SECONDARY);
            description.add(assignmentLabel);
            description.add(Box.createVerticalStrut(3));
            description.add(courseLabel);
            description.add(Box.createVerticalStrut(2));
            description.add(datesLabel);
            c.gridx = 1;
            c.weightx = 1;
            line.add(description, c);

            JLabel action = createLink(assignment.hasSubmission() ? "View submission" : "Add file", () -> {
                if (assignment.hasSubmission()) {
                    submissionDetailsNavigation.accept(assignment.courseCode(), assignment.id());
                } else {
                    assignmentNavigation.accept(assignment.courseCode(), assignment.id());
                }
            });
            c.gridx = 2;
            c.weightx = 0;
            c.insets = new Insets(0, 12, 0, 0);
            line.add(action, c);
            list.add(line);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private String dashboardAssignmentStatus(StudentAssignment assignment) {
        if ("GRADED".equalsIgnoreCase(assignment.status())) return "Graded";
        if ("LATE".equalsIgnoreCase(assignment.status())) return "Late";
        if (assignment.hasSubmission()) return "Submitted";
        LocalDateTime cutoff = assignment.cutoffAt() == null ? assignment.dueAt() : assignment.cutoffAt();
        if (cutoff != null && LocalDateTime.now().isAfter(cutoff)) return "Overdue";
        if (assignment.dueAt() != null && LocalDateTime.now().isAfter(assignment.dueAt())) return "Late";
        return "Not submitted";
    }

    private JLabel createLink(String text, Runnable action) {
        JLabel link = new JLabel(text);
        link.setFont(FONT_CAP_BOLD);
        link.setForeground(COLOR_PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));

        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                link.setForeground(Theme.PRIMARY_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                link.setForeground(COLOR_PRIMARY);
            }
        });

        return link;
    }
}
