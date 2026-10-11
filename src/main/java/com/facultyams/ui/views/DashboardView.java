package com.facultyams.ui.views;

import com.facultyams.model.Marks;
import com.facultyams.model.Notice;
import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class DashboardView extends JPanel {
    private final Consumer<String> navigationHandler;
    private final DataService dataService = DataService.getInstance();

    public DashboardView(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(Theme.createEmptyPadding(0, 0, 20, 0));

        // 1. Welcome Banner
        mainContent.add(createWelcomeBanner());
        mainContent.add(Box.createVerticalStrut(18));

        // 2. 5 Stat Metric Cards
        mainContent.add(createStatCardsPanel());
        mainContent.add(Box.createVerticalStrut(18));

        // 3. Middle Section: Recent Results (left) + Notice Board (right)
        mainContent.add(createMiddleSection());
        mainContent.add(Box.createVerticalStrut(18));

        // 4. Bottom Section: Academic Calendar + Quick Access + Performance Overview
        mainContent.add(createBottomSection());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createWelcomeBanner() {
        Student student = dataService.getCurrentStudent();

        CardPanel banner = new CardPanel(16) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                // Subtle blue gradient
                GradientPaint gp = new GradientPaint(0, 0, Theme.BANNER_GRADIENT_START, w, h, Theme.BANNER_GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.setColor(new Color(219, 234, 254));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
            }
        };
        banner.setLayout(new BorderLayout(20, 0));
        banner.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));
        banner.setPreferredSize(new Dimension(0, 140));

        // Text Left
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel greetingLbl = new JLabel("Good Morning, \uD83D\uDC4B");
        greetingLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        greetingLbl.setForeground(new Color(30, 41, 59));

        JLabel nameLbl = new JLabel(student.getFullName() + "!");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        nameLbl.setForeground(Theme.PRIMARY_HOVER);

        JLabel subLbl = new JLabel("Stay updated with your academic journey.");
        subLbl.setFont(Theme.FONT_BODY);
        subLbl.setForeground(Theme.TEXT_MUTED);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(greetingLbl);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(nameLbl);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(subLbl);
        textPanel.add(Box.createVerticalGlue());

        banner.add(textPanel, BorderLayout.CENTER);
        banner.add(new BannerIllustration(), BorderLayout.EAST);

        return banner;
    }

    private JPanel createStatCardsPanel() {
        Student s = dataService.getCurrentStudent();
        JPanel row = new JPanel(new GridLayout(1, 5, 14, 0));
        row.setOpaque(false);

        // 1. CGPA
        row.add(new StatCard("cgpa", Theme.ACCENT_BLUE_BG, Theme.ACCENT_BLUE,
                "CGPA", String.format("%.2f", s.getCgpa()), "Current CGPA", Theme.PRIMARY));

        // 2. Courses Enrolled
        row.add(new StatCard("graduation-cap", Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN,
                "Courses Enrolled", String.valueOf(s.getEnrolledCoursesCount()), "This Semester", Theme.ACCENT_GREEN));

        // 3. Exams Completed
        row.add(new StatCard("exams", Theme.ACCENT_PURPLE_BG, Theme.ACCENT_PURPLE,
                "Exams Completed", String.valueOf(s.getCompletedExams()), "This Semester", Theme.ACCENT_PURPLE));

        // 4. Overall Rank
        row.add(new StatCard("trophy", Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER,
                "Overall Rank", String.valueOf(s.getOverallRank()), "out of " + s.getTotalStudents(), Theme.ACCENT_AMBER));

        // 5. Attendance
        row.add(new StatCard("attendance", Theme.ACCENT_ROSE_BG, Theme.ACCENT_ROSE,
                "Attendance", (int) s.getAttendancePercentage() + "%", "This Semester", Theme.ACCENT_ROSE));

        return row;
    }

    private JPanel createMiddleSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Recent Results Card (Left, weightx = 0.62)
        gbc.gridx = 0;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, 14);
        section.add(createRecentResultsCard(), gbc);

        // Notice Board Card (Right, weightx = 0.38)
        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 0, 0, 0);
        section.add(createNoticeBoardCard(), gbc);

        return section;
    }

    private JPanel createRecentResultsCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(Theme.createEmptyPadding(16, 18, 16, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recent Results");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel viewAll = createLinkButton("View All Results", () -> navigationHandler.accept("Results"));
        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Table
        String[] columns = {"Subject", "Exam Type", "Grade", "Score", "Date"};
        List<Marks> marks = dataService.getRecentMarks();
        Object[][] data = new Object[marks.size()][5];

        for (int i = 0; i < marks.size(); i++) {
            Marks m = marks.get(i);
            data[i][0] = m.getSubject();
            data[i][1] = m.getExamType();
            data[i][2] = m.getGrade();
            data[i][3] = m.getScoreFormatted();
            data[i][4] = m.getDate();
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        ModernTable table = new ModernTable(model);
        // Custom Grade column renderer
        table.getColumnModel().getColumn(2).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
                p.setOpaque(false);
                p.add(BadgeLabel.createGradeBadge((String) value));
                return p;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setPreferredWidth(85);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        JScrollPane scroll = ModernTable.createModernScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 220));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createNoticeBoardCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(Theme.createEmptyPadding(16, 18, 16, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Notice Board");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel viewAll = createLinkButton("View All Notices", () -> navigationHandler.accept("Notice Board"));
        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Notice list
        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<Notice> notices = dataService.getNotices();
        int max = Math.min(notices.size(), 3);
        for (int i = 0; i < max; i++) {
            Notice n = notices.get(i);
            listPanel.add(createNoticeItem(n));
            if (i < max - 1) {
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        card.add(listPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createNoticeItem(Notice n) {
        JPanel item = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Theme.CARD_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setBorder(Theme.createEmptyPadding(10, 12, 10, 12));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigationHandler.accept("Notice Board");
            }
        });

        // Loudspeaker icon box
        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(Theme.ACCENT_BLUE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(38, 38));
        iconBox.add(new JLabel(IconHelper.getIcon("loudspeaker", 20, Theme.PRIMARY)));

        // Details
        JPanel textP = new JPanel();
        textP.setOpaque(false);
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(Theme.FONT_BODY_BOLD);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        // Short preview
        String preview = n.getContent();
        if (preview.length() > 68) {
            preview = preview.substring(0, 68) + "...";
        }
        JLabel descLbl = new JLabel("<html><body style='width: 170px;'>" + preview + "</body></html>");
        descLbl.setFont(Theme.FONT_SMALL);
        descLbl.setForeground(Theme.TEXT_MUTED);

        JLabel dateLbl = new JLabel(n.getDate());
        dateLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dateLbl.setForeground(Theme.PRIMARY);

        textP.add(titleLbl);
        textP.add(Box.createVerticalStrut(2));
        textP.add(descLbl);
        textP.add(Box.createVerticalStrut(4));
        textP.add(dateLbl);

        item.add(iconBox, BorderLayout.WEST);
        item.add(textP, BorderLayout.CENTER);

        return item;
    }

    private JPanel createBottomSection() {
        JPanel section = new JPanel(new GridLayout(1, 3, 14, 0));
        section.setOpaque(false);

        // 1. Academic Calendar
        CardPanel calCard = new CardPanel(16);
        calCard.setLayout(new BorderLayout(0, 10));
        calCard.setBorder(Theme.createEmptyPadding(16, 16, 16, 16));
        JPanel calHeader = new JPanel(new BorderLayout());
        calHeader.setOpaque(false);
        JLabel calTitle = new JLabel("Academic Calendar");
        calTitle.setFont(Theme.FONT_SECTION);
        calTitle.setForeground(Theme.TEXT_MAIN);
        JLabel calLink = createLinkButton("View Calendar", () -> navigationHandler.accept("Timetable"));
        calHeader.add(calTitle, BorderLayout.WEST);
        calHeader.add(calLink, BorderLayout.EAST);
        calCard.add(calHeader, BorderLayout.NORTH);
        calCard.add(new CalendarWidget(), BorderLayout.CENTER);
        section.add(calCard);

        // 2. Quick Access
        CardPanel qaCard = new CardPanel(16);
        qaCard.setLayout(new BorderLayout(0, 10));
        qaCard.setBorder(Theme.createEmptyPadding(16, 16, 16, 16));
        JLabel qaTitle = new JLabel("Quick Access");
        qaTitle.setFont(Theme.FONT_SECTION);
        qaTitle.setForeground(Theme.TEXT_MAIN);
        qaCard.add(qaTitle, BorderLayout.NORTH);
        qaCard.add(new QuickAccessPanel(navigationHandler), BorderLayout.CENTER);
        section.add(qaCard);

        // 3. Performance Overview
        CardPanel perfCard = new CardPanel(16);
        perfCard.setLayout(new BorderLayout(0, 10));
        perfCard.setBorder(Theme.createEmptyPadding(16, 16, 16, 16));
        JPanel perfHeader = new JPanel(new BorderLayout());
        perfHeader.setOpaque(false);
        JLabel perfTitle = new JLabel("Performance Overview");
        perfTitle.setFont(Theme.FONT_SECTION);
        perfTitle.setForeground(Theme.TEXT_MAIN);
        JLabel perfLink = createLinkButton("View Details", () -> navigationHandler.accept("Results"));
        perfHeader.add(perfTitle, BorderLayout.WEST);
        perfHeader.add(perfLink, BorderLayout.EAST);
        perfCard.add(perfHeader, BorderLayout.NORTH);
        perfCard.add(new DonutChartPanel(), BorderLayout.CENTER);
        section.add(perfCard);

        return section;
    }

    private JLabel createLinkButton(String text, Runnable action) {
        JLabel link = new JLabel(text);
        link.setFont(Theme.FONT_SMALL);
        link.setForeground(Theme.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<html><u>" + text + "</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                link.setText(text);
            }
        });
        return link;
    }
}
