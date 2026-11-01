package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.security.Role;
import com.facultyams.service.AdminService;
import com.facultyams.ui.admin.AdminIcons.Kind;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Admin home screen: four summary cards (users, departments, courses, notices) read live from the database
 * through {@link AdminService}, one quick action for each management screen, and two insight cards:
 * a "Users by Role" bar chart and a "Recent Activity" list (new users and posted notices).
 *
 * <p>The figures are loaded in the background so the window never freezes on a slow or unreachable
 * database. Each card is loaded independently: if one table cannot be read (say the notices migration was
 * not run) that card shows "-" and the reason is written under the cards with a Retry button, while the
 * other cards still show their real numbers. The two insight cards are loaded independently as well and show
 * their own message if they cannot be read.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final String LOADING = "...";
    private static final String UNAVAILABLE = "-";

    /** Counts one figure; throws what the service throws. */
    @FunctionalInterface
    private interface Count {
        int get() throws DatabaseException, AuthorizationException;
    }

    /** One loaded figure, or the reason it could not be loaded. */
    private record Figure(Integer value, String error) {
        static Figure of(Count count) {
            try {
                return new Figure(count.get(), null);
            } catch (DatabaseException e) {
                // same logging UIUtil.showException does for database errors
                System.err.println("Database error: " + e.getMessage());
                if (e.getCause() != null) {
                    e.getCause().printStackTrace();
                }
                return new Figure(null, e.getMessage());
            } catch (AuthorizationException e) {
                return new Figure(null, e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
                return new Figure(null, "Unexpected error: " + e.getMessage());
            }
        }
    }

    private record Snapshot(Figure users, Figure departments, Figure courses, Figure notices) {
    }

    /** Loads one value for an insight card; throws what the service throws. */
    @FunctionalInterface
    private interface Load<T> {
        T get() throws DatabaseException, AuthorizationException;
    }

    /** A loaded value for an insight card, or the reason it could not be loaded. */
    private record Loaded<T>(T value, String error) {
        static <T> Loaded<T> of(Load<T> load) {
            try {
                return new Loaded<>(load.get(), null);
            } catch (DatabaseException e) {
                System.err.println("Database error: " + e.getMessage());
                if (e.getCause() != null) {
                    e.getCause().printStackTrace();
                }
                return new Loaded<>(null, e.getMessage());
            } catch (AuthorizationException e) {
                return new Loaded<>(null, e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
                return new Loaded<>(null, "Unexpected error: " + e.getMessage());
            }
        }
    }

    private record Insights(Loaded<Map<Role, Integer>> roles, Loaded<List<AdminService.Activity>> activity) {
    }

    private static final DateTimeFormatter ACTIVITY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final transient AdminService adminService;
    private final StatCard usersCard = new StatCard("users", "Total Users", AdminTheme.CARD_BLUE, Kind.USERS);
    private final StatCard departmentsCard = new StatCard("departments", "Departments", AdminTheme.CARD_GREEN, Kind.BUILDING);
    private final StatCard coursesCard = new StatCard("courses", "Courses", AdminTheme.CARD_PURPLE, Kind.BOOK);
    private final StatCard noticesCard = new StatCard("notices", "Notices", AdminTheme.CARD_ORANGE, Kind.MEGAPHONE);
    private final InsightCard roleCard = new InsightCard("dashboard.roles", "Users by Role");
    private final InsightCard activityCard = new InsightCard("dashboard.activity", "Recent Activity");
    private final JTextArea errorText = AdminTheme.errorText();
    private final JButton retryButton = new JButton("Retry");
    private final JPanel errorRow = new JPanel(new BorderLayout(12, 0));

    /** Incremented by every refresh so a slow, older load can never overwrite a newer one. */
    private int generation;
    /** Background loads still running for the current generation (the figures and the insight cards). */
    private int pending;

    /** @param navigate called with an {@link AdminShell} page key when a quick action is clicked */
    public DashboardPanel(Consumer<String> navigate) {
        this(navigate, new AdminService());
    }

    public DashboardPanel(Consumer<String> navigate, AdminService adminService) {
        super(new BorderLayout());
        this.adminService = adminService;
        setBackground(Color.WHITE);

        JPanel cards = new JPanel(new GridLayout(1, 4, 18, 0));
        cards.setOpaque(false);
        cards.add(usersCard);
        cards.add(departmentsCard);
        cards.add(coursesCard);
        cards.add(noticesCard);
        cards.setPreferredSize(new Dimension(100, 110));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        cards.setAlignmentX(LEFT_ALIGNMENT);

        errorText.setName("dashboard.error");
        retryButton.setName("dashboard.retry");
        retryButton.addActionListener(e -> refresh());
        JPanel retryWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        retryWrap.setOpaque(false);
        retryWrap.add(retryButton);
        errorRow.setOpaque(false);
        errorRow.add(errorText, BorderLayout.CENTER);
        errorRow.add(retryWrap, BorderLayout.EAST);
        errorRow.setAlignmentX(LEFT_ALIGNMENT);
        errorRow.setVisible(false);

        JLabel quick = new JLabel("Quick Actions");
        quick.setFont(AdminTheme.SECTION);
        quick.setForeground(AdminTheme.TEXT);
        quick.setAlignmentX(LEFT_ALIGNMENT);

        JPanel actions = new JPanel(new GridLayout(1, 5, 14, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(100, 104));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.add(action("Users", "User", Kind.USERS, AdminShell.PAGE_USERS, navigate));
        actions.add(action("Departments", "Department", Kind.BUILDING, AdminShell.PAGE_DEPARTMENTS, navigate));
        actions.add(action("Courses", "Course", Kind.BOOK, AdminShell.PAGE_COURSES, navigate));
        actions.add(action("Notices", "Notice", Kind.MEGAPHONE, AdminShell.PAGE_NOTICES, navigate));
        actions.add(action("Timetables", "Timetable", Kind.CALENDAR, AdminShell.PAGE_TIMETABLES, navigate));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(cards);
        column.add(Box.createVerticalStrut(10));
        column.add(errorRow);
        column.add(Box.createVerticalStrut(14));
        column.add(quick);
        column.add(Box.createVerticalStrut(8));
        column.add(actions);
        add(column, BorderLayout.NORTH);

        JPanel insights = new JPanel(new GridLayout(1, 2, 18, 0));
        insights.setOpaque(false);
        insights.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        insights.setMinimumSize(new Dimension(100, 200));
        insights.add(roleCard);
        insights.add(activityCard);
        add(insights, BorderLayout.CENTER);
    }

    /**
     * One quick action: an icon above "Manage / Departments" (two lines, so all five fit side by side even at
     * the window's minimum width); the plain "Department Management" is kept as tooltip and accessible name.
     */
    private static JButton action(String plural, String subject, Kind icon, String pageKey, Consumer<String> navigate) {
        String label = subject + " Management";
        JButton b = new JButton("<html><div style='text-align:center'>Manage<br>" + plural + "</div></html>",
                AdminIcons.of(icon, 30, AdminTheme.BLUE));
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setIconTextGap(8);
        b.setName("quick." + pageKey);
        b.setToolTipText("Open " + label);
        b.getAccessibleContext().setAccessibleName(label);
        b.setActionCommand(label);
        AdminTheme.secondary(b);
        b.setBorder(BorderFactory.createLineBorder(AdminTheme.BORDER));
        b.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        b.addActionListener(e -> navigate.accept(pageKey));
        return b;
    }

    /** True while the figures, the role chart or the recent activity are being read from the database. */
    public boolean isLoading() {
        return pending > 0;
    }

    /** Re-reads all four figures from the database in the background; returns immediately. */
    @Override
    public void refresh() {
        final int myGeneration = ++generation;
        pending = 2;   // the four figures + the insight cards
        for (StatCard card : allCards()) {
            card.setValue(LOADING, null);
        }
        roleCard.showMessage("Loading...", false);
        activityCard.showMessage("Loading...", false);
        loadInsights(myGeneration);
        new SwingWorker<Snapshot, Void>() {
            @Override
            protected Snapshot doInBackground() {
                return new Snapshot(
                        Figure.of(adminService::countUsers),
                        Figure.of(adminService::countDepartments),
                        Figure.of(adminService::countCourses),
                        Figure.of(adminService::countNotices));
            }

            @Override
            protected void done() {
                if (myGeneration != generation) {
                    return; // a newer refresh is already running
                }
                pending--;
                try {
                    show(get());
                } catch (Exception e) {
                    // doInBackground cannot throw (Figure.of catches), but never leave "..." on screen
                    Figure failed = new Figure(null, "Could not load the dashboard figures: " + e.getMessage());
                    show(new Snapshot(failed, failed, failed, failed));
                }
            }
        }.execute();
    }

    /** Reads the role counts and recent activity in the background; a newer refresh makes this result obsolete. */
    private void loadInsights(int myGeneration) {
        new SwingWorker<Insights, Void>() {
            @Override
            protected Insights doInBackground() {
                return new Insights(Loaded.of(adminService::countUsersByRole),
                        Loaded.of(() -> adminService.getRecentActivity(5)));
            }

            @Override
            protected void done() {
                if (myGeneration != generation) {
                    return;
                }
                pending--;
                try {
                    showInsights(get());
                } catch (Exception e) {
                    String reason = "Could not load this section: " + e.getMessage();
                    roleCard.showMessage(reason, true);
                    activityCard.showMessage(reason, true);
                }
            }
        }.execute();
    }

    private void showInsights(Insights in) {
        if (in.roles().error() != null) {
            roleCard.showMessage(in.roles().error(), true);
        } else {
            roleCard.showContent(new RoleChart(in.roles().value()));
        }
        if (in.activity().error() != null) {
            activityCard.showMessage(in.activity().error(), true);
        } else if (in.activity().value() == null || in.activity().value().isEmpty()) {
            activityCard.showMessage("No recent activity yet.", false);
        } else {
            JPanel rows = new JPanel();
            rows.setOpaque(false);
            rows.setName("dashboard.activity.rows");
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            for (AdminService.Activity a : in.activity().value()) {
                JPanel row = new JPanel(new BorderLayout(12, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AdminTheme.BORDER),
                        BorderFactory.createEmptyBorder(9, 0, 9, 0)));
                JLabel text = new JLabel(a.text());
                text.setFont(AdminTheme.FONT);
                text.setForeground(AdminTheme.TEXT);
                text.setToolTipText(a.text());
                JLabel when = new JLabel(a.when().format(ACTIVITY_DATE));
                when.setFont(AdminTheme.FONT);
                when.setForeground(AdminTheme.TEXT_MUTED);
                row.add(text, BorderLayout.CENTER);
                row.add(when, BorderLayout.EAST);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                rows.add(row);
            }
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(rows, BorderLayout.NORTH);
            activityCard.showContent(top);
        }
        revalidate();
        repaint();
    }

    private List<StatCard> allCards() {
        return List.of(usersCard, departmentsCard, coursesCard, noticesCard);
    }

    private void show(Snapshot s) {
        usersCard.setFigure(s.users());
        departmentsCard.setFigure(s.departments());
        coursesCard.setFigure(s.courses());
        noticesCard.setFigure(s.notices());

        // one line per problem ("Notices: <reason>"); when everything failed the same way, just the reason once
        Set<String> reasons = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>();
        for (StatCard card : allCards()) {
            if (card.error != null) {
                reasons.add(card.error);
                lines.add(card.caption + ": " + card.error);
            }
        }
        boolean failed = !lines.isEmpty();
        if (failed) {
            errorText.setText(reasons.size() == 1 && lines.size() > 1
                    ? "Could not load the dashboard figures. " + reasons.iterator().next()
                    : String.join("\n", lines));
        } else {
            errorText.setText("");
        }
        errorRow.setVisible(failed);
        revalidate();
        repaint();
    }

    /** Rounded coloured tile showing one number and its caption. */
    private static final class StatCard extends JPanel {

        private static final long serialVersionUID = 1L;

        private final JLabel valueLabel = new JLabel(LOADING, SwingConstants.LEFT);
        private final String caption;
        private final Color color;
        private final JLabel iconLabel;
        private String error;

        StatCard(String key, String caption, Color color, Kind icon) {
            super(new BorderLayout());
            this.caption = caption;
            this.color = color;
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            setName("card." + key);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 14));
            iconLabel = new JLabel(AdminIcons.of(icon, 40, Color.WHITE));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
            JLabel captionLabel = new JLabel(caption, SwingConstants.LEFT);
            captionLabel.setForeground(Color.WHITE);
            captionLabel.setFont(AdminTheme.FONT_BOLD);
            valueLabel.setName("card." + key + ".value");
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setFont(valueLabel.getFont().deriveFont(java.awt.Font.BOLD, 38f));
            JPanel text = new JPanel(new BorderLayout());
            text.setOpaque(false);
            text.add(captionLabel, BorderLayout.NORTH);
            text.add(valueLabel, BorderLayout.CENTER);
            add(iconLabel, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        /** The icon is dropped on narrow cards (small windows) so the caption and the number keep their room. */
        @Override
        public void doLayout() {
            iconLabel.setVisible(getWidth() >= 180);
            super.doLayout();
        }

        void setFigure(Figure figure) {
            setValue(figure.value() == null ? UNAVAILABLE : String.valueOf(figure.value()), figure.error());
        }

        void setValue(String value, String error) {
            this.error = error;
            valueLabel.setText(value);
            setToolTipText(error);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** White rounded card with a title and one swappable body (chart, list or message). */
    private static final class InsightCard extends JPanel {

        private static final long serialVersionUID = 1L;

        private final JPanel body = new JPanel(new BorderLayout());

        InsightCard(String name, String title) {
            super(new BorderLayout(0, 10));
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            setName(name);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
            JLabel heading = new JLabel(title);
            heading.setFont(AdminTheme.SECTION);
            heading.setForeground(AdminTheme.TEXT);
            body.setOpaque(false);
            add(heading, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }

        void showContent(Component content) {
            body.removeAll();
            body.add(content, BorderLayout.CENTER);
            body.revalidate();
            body.repaint();
        }

        void showMessage(String message, boolean error) {
            if (error) {
                JTextArea text = AdminTheme.errorText();
                text.setText(message);
                showContent(text);
            } else {
                JLabel label = new JLabel(message, SwingConstants.CENTER);
                label.setFont(AdminTheme.FONT);
                label.setForeground(AdminTheme.TEXT_MUTED);
                showContent(label);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(AdminTheme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Vertical bar chart: one bar per role, value above the bar, role name (up to two lines) below. */
    private static final class RoleChart extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final Color[] COLORS = {AdminTheme.CARD_BLUE, AdminTheme.CARD_GREEN,
                AdminTheme.CARD_PURPLE, AdminTheme.CARD_ORANGE};

        private final transient Map<Role, Integer> counts;

        RoleChart(Map<Role, Integer> counts) {
            this.counts = counts == null ? Map.of() : counts;
            setOpaque(false);
            setName("dashboard.chart");
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            StringBuilder description = new StringBuilder("Users by role: ");
            for (Role role : Role.values()) {
                description.append(role.getDisplayName()).append(' ').append(valueOf(role)).append("; ");
            }
            getAccessibleContext().setAccessibleDescription(description.toString().trim());
        }

        private int valueOf(Role role) {
            Integer value = counts.get(role);
            return value == null ? 0 : value;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Role[] roles = Role.values();
            int w = getWidth();
            int h = getHeight();
            int labelArea = 38;
            int topArea = 22;
            int baseline = h - labelArea;
            int plotHeight = Math.max(10, baseline - topArea);
            int max = 1;
            for (Role role : roles) {
                max = Math.max(max, valueOf(role));
            }
            int slot = Math.max(1, w / roles.length);
            int barWidth = Math.min(54, Math.max(18, slot / 2));
            Font valueFont = AdminTheme.FONT_BOLD;
            Font labelFont = AdminTheme.FONT;
            for (int i = 0; i < roles.length; i++) {
                int value = valueOf(roles[i]);
                int barHeight = value == 0 ? 0 : Math.max(4, (int) Math.round(plotHeight * (double) value / max));
                int x = i * slot + (slot - barWidth) / 2;
                g2.setColor(COLORS[i % COLORS.length]);
                if (barHeight > 0) {
                    g2.fillRoundRect(x, baseline - barHeight, barWidth, barHeight + 6, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(x - 1, baseline, barWidth + 2, 8);   // square off the bottom on the baseline
                }
                g2.setFont(valueFont);
                g2.setColor(AdminTheme.TEXT);
                FontMetrics vm = g2.getFontMetrics();
                String number = String.valueOf(value);
                g2.drawString(number, x + (barWidth - vm.stringWidth(number)) / 2, baseline - barHeight - 5);

                g2.setFont(labelFont);
                g2.setColor(AdminTheme.TEXT_MUTED);
                FontMetrics lm = g2.getFontMetrics();
                String[] lines = roles[i].getDisplayName().split(" ", 2);
                for (int line = 0; line < lines.length; line++) {
                    g2.drawString(lines[line], i * slot + (slot - lm.stringWidth(lines[line])) / 2,
                            baseline + 16 + line * (lm.getHeight() - 1));
                }
            }
            g2.setColor(AdminTheme.BORDER);
            g2.drawLine(0, baseline, w, baseline);
            g2.dispose();
        }
    }
}
