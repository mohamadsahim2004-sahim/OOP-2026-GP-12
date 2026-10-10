package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.service.AdminService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.function.Consumer;

/** Admin home screen: four live summary cards and quick-action shortcuts. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final AdminService adminService = new AdminService();
    private final StatCard usersCard = new StatCard("Total Users", AdminTheme.CARD_BLUE);
    private final StatCard departmentsCard = new StatCard("Departments", AdminTheme.CARD_GREEN);
    private final StatCard coursesCard = new StatCard("Courses", AdminTheme.CARD_PURPLE);
    private final StatCard noticesCard = new StatCard("Notices", AdminTheme.CARD_ORANGE);

    /** @param navigate called with a page key (see AdminDashboardFrame) when a quick action is clicked */
    public DashboardPanel(Consumer<String> navigate) {
        super(new BorderLayout(0, 20));
        setBackground(Color.WHITE);

        JPanel cards = new JPanel(new GridLayout(1, 4, 18, 0));
        cards.setOpaque(false);
        cards.add(usersCard);
        cards.add(departmentsCard);
        cards.add(coursesCard);
        cards.add(noticesCard);
        cards.setPreferredSize(new Dimension(100, 110));

        JLabel quick = new JLabel("Quick Actions");
        quick.setFont(AdminTheme.SECTION);
        quick.setForeground(AdminTheme.TEXT);

        JPanel actions = new JPanel(new GridLayout(1, 5, 14, 0));
        actions.setOpaque(false);
        actions.add(action("Manage Users", "users", navigate));
        actions.add(action("Manage Departments", "departments", navigate));
        actions.add(action("Manage Courses", "courses", navigate));
        actions.add(action("Manage Notices", "notices", navigate));
        actions.add(action("Manage Timetables", "timetables", navigate));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(quick, BorderLayout.NORTH);
        actions.setPreferredSize(new Dimension(100, 80));
        JPanel actionsWrap = new JPanel(new BorderLayout());
        actionsWrap.setOpaque(false);
        actionsWrap.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        actionsWrap.add(actions, BorderLayout.NORTH);
        bottom.add(actionsWrap, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(cards, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
    }

    private static JButton action(String text, String pageKey, Consumer<String> navigate) {
        JButton b = new JButton(text);
        AdminTheme.secondary(b);
        b.setBorder(BorderFactory.createLineBorder(AdminTheme.BORDER));
        b.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        b.addActionListener(e -> navigate.accept(pageKey));
        return b;
    }

    @Override
    public void refresh() {
        try {
            Map<String, Integer> summary = adminService.getDashboardSummary();
            usersCard.setValue(String.valueOf(summary.get("Total Users")));
            departmentsCard.setValue(String.valueOf(summary.get("Departments")));
            coursesCard.setValue(String.valueOf(summary.get("Courses")));
            noticesCard.setValue(String.valueOf(summary.get("Notices")));
        } catch (DatabaseException | AuthorizationException e) {
            usersCard.setValue("-");
            departmentsCard.setValue("-");
            coursesCard.setValue("-");
            noticesCard.setValue("-");
            UIUtil.showException(this, e);
        }
    }

    /** Rounded coloured tile showing one number and its caption. */
    private static final class StatCard extends JPanel {

        private final JLabel valueLabel = new JLabel("--", SwingConstants.CENTER);
        private final Color color;

        StatCard(String caption, Color color) {
            super(new BorderLayout());
            this.color = color;
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
            JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
            captionLabel.setForeground(Color.WHITE);
            captionLabel.setFont(AdminTheme.FONT_BOLD);
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setFont(valueLabel.getFont().deriveFont(java.awt.Font.BOLD, 38f));
            add(captionLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
        }

        void setValue(String value) {
            valueLabel.setText(value);
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