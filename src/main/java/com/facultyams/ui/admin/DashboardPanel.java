package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.service.AdminService;

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Admin home screen: four summary cards (users, departments, courses, notices) read live from the database
 * through {@link AdminService}, and one quick action for each management screen.
 *
 * <p>The figures are loaded in the background so the window never freezes on a slow or unreachable
 * database. Each card is loaded independently: if one table cannot be read (say the notices migration was
 * not run) that card shows "-" and the reason is written under the cards with a Retry button, while the
 * other cards still show their real numbers.
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

    private final transient AdminService adminService;
    private final StatCard usersCard = new StatCard("users", "Total Users", AdminTheme.CARD_BLUE);
    private final StatCard departmentsCard = new StatCard("departments", "Departments", AdminTheme.CARD_GREEN);
    private final StatCard coursesCard = new StatCard("courses", "Courses", AdminTheme.CARD_PURPLE);
    private final StatCard noticesCard = new StatCard("notices", "Notices", AdminTheme.CARD_ORANGE);
    private final JTextArea errorText = AdminTheme.errorText();
    private final JButton retryButton = new JButton("Retry");
    private final JPanel errorRow = new JPanel(new BorderLayout(12, 0));

    /** Incremented by every refresh so a slow, older load can never overwrite a newer one. */
    private int generation;
    private boolean loading;

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
        actions.setPreferredSize(new Dimension(100, 80));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.add(action("User", AdminShell.PAGE_USERS, navigate));
        actions.add(action("Department", AdminShell.PAGE_DEPARTMENTS, navigate));
        actions.add(action("Course", AdminShell.PAGE_COURSES, navigate));
        actions.add(action("Notice", AdminShell.PAGE_NOTICES, navigate));
        actions.add(action("Timetable", AdminShell.PAGE_TIMETABLES, navigate));

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
    }

    /**
     * One quick action. The text is split over two lines ("Department" / "Management") so all five fit
     * side by side even at the window's minimum width; the plain name is kept as tooltip and accessible name.
     */
    private static JButton action(String subject, String pageKey, Consumer<String> navigate) {
        String label = subject + " Management";
        JButton b = new JButton("<html><div style='text-align:center'>" + subject + "<br>Management</div></html>");
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

    /** True while the figures are being read from the database. */
    public boolean isLoading() {
        return loading;
    }

    /** Re-reads all four figures from the database in the background; returns immediately. */
    @Override
    public void refresh() {
        final int myGeneration = ++generation;
        loading = true;
        for (StatCard card : allCards()) {
            card.setValue(LOADING, null);
        }
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
                loading = false;
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

        private final JLabel valueLabel = new JLabel(LOADING, SwingConstants.CENTER);
        private final String caption;
        private final Color color;
        private String error;

        StatCard(String key, String caption, Color color) {
            super(new BorderLayout());
            this.caption = caption;
            this.color = color;
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            setName("card." + key);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
            JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
            captionLabel.setForeground(Color.WHITE);
            captionLabel.setFont(AdminTheme.FONT_BOLD);
            valueLabel.setName("card." + key + ".value");
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setFont(valueLabel.getFont().deriveFont(java.awt.Font.BOLD, 38f));
            add(captionLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
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
}
