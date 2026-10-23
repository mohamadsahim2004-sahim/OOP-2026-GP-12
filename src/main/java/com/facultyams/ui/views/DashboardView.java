package com.facultyams.ui.views;

import com.facultyams.model.Marks;
import com.facultyams.model.Notice;
import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.service.ResultService;
import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.theme.Theme;
import com.facultyams.util.Constants;
import com.srms.controller.NavigationListener;
import com.srms.service.DeadlineService;
import com.srms.service.MockCourseService;
import com.srms.ui.components.UpcomingDeadlinesPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Modern, clean, minimal Dashboard UI matching the exact SRMS undergraduate portal design.
 */
public class DashboardView extends JPanel {
    private final Consumer<String> navigationHandler;
    private final NavigationListener deadlineNavigation;
    private final DataService dataService = DataService.getInstance();

    // Design Tokens
    private static final Color COLOR_TEXT_PRIMARY   = new Color(15, 23, 42);    // #0F172A
    private static final Color COLOR_TEXT_SECONDARY = new Color(100, 116, 139);  // #64748B
    private static final Color COLOR_TEXT_MUTED     = new Color(148, 163, 184); // #94A3B8
    private static final Color COLOR_PRIMARY        = new Color(37, 99, 235);   // #2563EB
    private static final Color COLOR_PRIMARY_LIGHT  = new Color(219, 234, 254); // #DBEAFE
    private static final Color COLOR_SUCCESS        = new Color(22, 163, 74);   // #16A34A
    private static final Color COLOR_SUCCESS_LIGHT  = new Color(220, 252, 231); // #DCFCE7
    private static final Color COLOR_DANGER         = new Color(220, 38, 38);   // #DC2626
    private static final Color COLOR_DANGER_LIGHT   = new Color(254, 226, 226); // #FEE2E2
    private static final Color COLOR_WARNING        = new Color(245, 158, 11);  // #F59E0B
    private static final Color COLOR_WARNING_LIGHT  = new Color(254, 243, 199); // #FEF3C7
    private static final Color COLOR_BORDER         = new Color(229, 231, 235); // #E5E7EB
    private static final Color COLOR_HOVER_BG       = new Color(248, 250, 252); // #F8FAFC

    // Fonts
    private static final Font FONT_GREETING = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SECTION  = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_METRIC   = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BODY_BOLD= new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_CAPTION  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_CAP_BOLD = new Font("Segoe UI", Font.BOLD, 11);

    public DashboardView(Consumer<String> navigationHandler, NavigationListener deadlineNavigation) {
        this.navigationHandler = navigationHandler;
        this.deadlineNavigation = deadlineNavigation;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // 1. Welcome Greeting Header (clean, no illustration)
        mainContent.add(createWelcomeHeader());
        mainContent.add(Box.createVerticalStrut(20));

        // 2. Row 1: 4 KPI Metric Cards
        mainContent.add(createKpiCardsRow());
        mainContent.add(Box.createVerticalStrut(20));

        // 3. Row 2: Recent Results (65%) + Notices (35%)
        mainContent.add(createResultsAndNoticesRow());
        mainContent.add(Box.createVerticalStrut(20));

        // 4. Row 3: Performance Donut (35%) + Upcoming Deadlines (65%)
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

        Student student = dataService.getCurrentStudent();
        int hour = LocalTime.now().getHour();
        String timeGreeting = (hour < 12) ? "Good morning" : (hour < 17) ? "Good afternoon" : "Good evening";

        String firstName = "Abhishek";
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
        Student s = dataService.getCurrentStudent();
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // 1. CGPA
        row.add(createKpiCard("CGPA", String.format(Locale.ROOT, "%.2f",
                        ResultService.getInstance().getSummary().cgpa()),
                "Out of " + String.format(Locale.ROOT, "%.2f", Constants.MAX_GPA) + " scale", COLOR_SUCCESS));

        // 2. Attendance
        row.add(createKpiCard("Attendance", (int) s.getAttendancePercentage() + "%", "Above 75% minimum", COLOR_SUCCESS));

        // 3. Courses
        row.add(createKpiCard("Courses", String.valueOf(s.getEnrolledCoursesCount()), s.getCompletedExams() + " exams completed", COLOR_SUCCESS));

        // 4. Class Rank
        row.add(createKpiCard("Class Rank", String.valueOf(s.getOverallRank()), "out of " + s.getTotalStudents(), COLOR_TEXT_SECONDARY));

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
        String[] columns = {"SUBJECT", "GRADE", "SCORE", "DATE"};
        List<Marks> marks = dataService.getRecentMarks();
        int count = Math.min(marks.size(), 5);
        Object[][] data = new Object[count][4];

        for (int i = 0; i < count; i++) {
            Marks m = marks.get(i);
            data[i][0] = m.getSubject();
            data[i][1] = m.getGrade();
            data[i][2] = m.getScore() + " / " + m.getMaxScore();
            data[i][3] = m.getDate();
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
        th.setFont(FONT_CAP_BOLD);
        th.setForeground(COLOR_TEXT_SECONDARY);
        th.setBackground(new Color(248, 250, 252));
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
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
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

        List<Notice> notices = dataService.getNotices();
        int max = Math.min(notices.size(), 3);
        for (int i = 0; i < max; i++) {
            Notice n = notices.get(i);
            boolean isUnread = (i < 2); // First 2 unread (blue dot), 3rd read (gray dot) as in image
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

    private JPanel createNoticeRow(Notice n, boolean unread) {
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

        JLabel dateLbl = new JLabel(n.getDate().contains("May") ? n.getDate().replace("2024", "").trim() : n.getDate());
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

        // Custom Donut Chart & Legend
        card.add(createDonutChartComponent(), BorderLayout.CENTER);
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
                double innerRadius = size * 0.35;
                double centerX = cx + outerRadius;
                double centerY = cy + outerRadius;

                // Segments: 60% green, 20% blue, 20% orange/amber
                double startAngle = 90.0;
                double arcExcellent = (60.0 / 100.0) * 360.0;
                double arcGood = (20.0 / 100.0) * 360.0;
                double arcAverage = 360.0 - (arcExcellent + arcGood);

                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle, -arcExcellent, COLOR_SUCCESS);
                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle - arcExcellent, -arcGood, COLOR_PRIMARY);
                drawSegment(g2, centerX, centerY, outerRadius, innerRadius, startAngle - arcExcellent - arcGood, -arcAverage, COLOR_WARNING);

                // Center text: 84% + Overall
                g2.setColor(COLOR_TEXT_PRIMARY);
                g2.setFont(FONT_GREETING);
                FontMetrics fm1 = g2.getFontMetrics();
                String pct = "84%";
                int tx = (int) (centerX - fm1.stringWidth(pct) / 2);
                int ty = (int) (centerY + fm1.getAscent() / 2 - 6);
                g2.drawString(pct, tx, ty);

                g2.setColor(COLOR_TEXT_SECONDARY);
                g2.setFont(FONT_CAPTION);
                FontMetrics fm2 = g2.getFontMetrics();
                String lbl = "Overall";
                int lx = (int) (centerX - fm2.stringWidth(lbl) / 2);
                int ly = ty + fm2.getAscent() + 2;
                g2.drawString(lbl, lx, ly);

                // Legend on Right
                int lxStart = cx + size + 20;
                int lyStart = cy + 24;

                drawLegendItem(g2, lxStart, lyStart, COLOR_SUCCESS, "Excellent", "60%");
                drawLegendItem(g2, lxStart, lyStart + 38, COLOR_PRIMARY, "Good", "20%");

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
                g2.fillRoundRect(x, y + 2, 8, 14, 4, 4);

                g2.setColor(COLOR_TEXT_PRIMARY);
                g2.setFont(FONT_BODY);
                g2.drawString(name, x + 16, y + 14);

                g2.setFont(FONT_BODY_BOLD);
                g2.drawString(value, x + 84, y + 14);
            }
        };
    }

    private JPanel createUpcomingDeadlinesCard() {
        return new UpcomingDeadlinesPanel(new DeadlineService(new MockCourseService()), deadlineNavigation);
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
                link.setForeground(new Color(29, 78, 216));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                link.setForeground(COLOR_PRIMARY);
            }
        });

        return link;
    }
}
