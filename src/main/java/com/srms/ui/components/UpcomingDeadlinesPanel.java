package com.srms.ui.components;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.controller.NavigationListener;
import com.srms.model.Deadline;
import com.srms.model.DeadlineTarget;
import com.srms.service.DeadlineService;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.*;

public final class UpcomingDeadlinesPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM");

    public UpcomingDeadlinesPanel(DeadlineService service, NavigationListener navigation) {
        setOpaque(false);
        List<Deadline> deadlines = service.getUpcomingDeadlines(5);
        Ui.Card card = new Ui.Card(Theme.RADIUS_CARD, Color.WHITE, Theme.CARD_BORDER);
        card.setLayout(new BorderLayout(0, Theme.SECTION_GAP));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));

        JLabel heading = new JLabel("Upcoming Deadlines");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(Theme.COURSE_TEXT_MAIN);
        card.add(heading, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        if (deadlines.isEmpty()) {
            JLabel empty = new JLabel("No upcoming deadlines", SwingConstants.CENTER);
            empty.setFont(Theme.COURSE_BODY_BOLD);
            empty.setForeground(Theme.COURSE_TEXT_MAIN);
            JLabel hint = new JLabel("You're all caught up for now.", SwingConstants.CENTER);
            hint.setFont(Theme.FONT_SMALL);
            hint.setForeground(Theme.COURSE_TEXT_MUTED);
            JPanel emptyState = new JPanel(new GridLayout(2, 1, 0, 4));
            emptyState.setOpaque(false);
            emptyState.add(empty);
            emptyState.add(hint);
            rows.add(emptyState);
        } else {
            for (int index = 0; index < deadlines.size(); index++) {
                Deadline deadline = deadlines.get(index);
                rows.add(new DeadlineRow(deadline, navigation));
                if (index < deadlines.size() - 1) rows.add(new Divider());
            }
        }
        card.add(rows, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(card, BorderLayout.CENTER);
    }

    private static final class DeadlineRow extends JPanel {
        private final Deadline deadline;
        private final NavigationListener navigation;
        private boolean hovered;
        private boolean pressed;

        DeadlineRow(Deadline deadline, NavigationListener navigation) {
            this.deadline = deadline;
            this.navigation = navigation;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, Theme.DEADLINE_ROW_HEIGHT));
            setMinimumSize(new Dimension(0, Theme.DEADLINE_ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.DEADLINE_ROW_HEIGHT));
            setToolTipText(deadline.type().isSubmittable() ? "Open submission" : "Open course");
            buildRow();
            installInteraction(this);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "open");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "open");
            getActionMap().put("open", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { open(); }
            });
        }

        private void buildRow() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP);
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.weightx = 0;
            add(new IconBadge(deadline), c);

            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP);
            JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
            details.setOpaque(false);
            JLabel title = new EllipsizedLabel(deadline.title(), Theme.COURSE_BODY_BOLD, Theme.COURSE_TEXT_MAIN);
            JLabel course = new EllipsizedLabel(deadline.courseTitle() + "  ·  " + deadline.courseCode(),
                    Theme.FONT_SMALL, Theme.COURSE_TEXT_MUTED);
            details.add(title);
            details.add(course);
            add(details, c);

            c.gridx++;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            add(new TypePill(deadline.type()), c);

            c.gridx++;
            c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP);
            add(new DateBlock(deadline.dueDate()), c);

            c.gridx++;
            c.insets = new Insets(0, 0, 0, 0);
            add(new Chevron(), c);
        }

        private void installInteraction(Component component) {
            if (component instanceof JComponent swingComponent) {
                swingComponent.setToolTipText(getToolTipText());
            }
            component.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!contains(SwingUtilities.convertPoint(component, e.getPoint(), DeadlineRow.this))) {
                        hovered = false;
                        pressed = false;
                        repaint();
                    }
                }
                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    pressed = true;
                    requestFocusInWindow();
                    repaint();
                }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                    boolean activate = pressed && SwingUtilities.convertPoint(component, e.getPoint(), DeadlineRow.this) != null
                            && contains(SwingUtilities.convertPoint(component, e.getPoint(), DeadlineRow.this));
                    pressed = false;
                    repaint();
                    if (activate) open();
                }
            });
            if (component instanceof Container container) {
                for (Component child : container.getComponents()) installInteraction(child);
            }
        }

        private void open() {
            if (navigation == null) return;
            if (deadline.navigationTarget() == DeadlineTarget.TIMETABLE) navigation.openTimetable();
            else if (deadline.type().isSubmittable()) navigation.openSubmission(deadline.courseCode(), deadline.id());
            else navigation.openCourse(deadline.courseCode());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (pressed) g.setColor(new Color(239, 246, 255));
            else if (hovered) g.setColor(Theme.HOVER_BG);
            else if (hasFocus()) g.setColor(Color.WHITE);
            else g.setColor(new Color(0, 0, 0, 0));
            if (hovered || pressed || hasFocus()) {
                g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            if (hasFocus()) {
                g.setColor(Theme.PRIMARY);
                g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3,
                        Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class IconBadge extends JComponent {
        private final Deadline deadline;
        IconBadge(Deadline deadline) {
            this.deadline = deadline;
            setPreferredSize(new Dimension(36, 36));
            setMinimumSize(getPreferredSize());
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(deadline.type().badgeBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            DeadlineIcons.paint(g, deadline.type() == com.srms.model.DeadlineType.EXAM
                    ? "check" : "document", 9, 9, 18, deadline.type().badgeForeground());
            g.dispose();
        }
    }

    private static final class DateBlock extends JPanel {
        DateBlock(LocalDate date) {
            setOpaque(false);
            setLayout(new GridLayout(2, 1, 0, 2));
            JLabel dateLabel = new JLabel(date.format(DATE_FORMAT), SwingConstants.RIGHT);
            dateLabel.setFont(Theme.COURSE_BODY_BOLD);
            dateLabel.setForeground(Theme.COURSE_TEXT_MAIN);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
            String relative = days == 0 ? "Today" : days == 1 ? "Tomorrow" : "in " + days + " days";
            JLabel hint = new JLabel(relative, SwingConstants.RIGHT);
            hint.setFont(Theme.FONT_SMALL);
            hint.setForeground(days <= 2 ? Theme.DANGER : Theme.COURSE_TEXT_MUTED);
            add(dateLabel);
            add(hint);
        }
    }

    private static final class Chevron extends JComponent {
        Chevron() { setPreferredSize(new Dimension(16, 16)); }
        @Override protected void paintComponent(Graphics graphics) {
            DeadlineIcons.paint((Graphics2D) graphics, "chevron", 0, 0, 16,
                    getParent() instanceof DeadlineRow row && row.hovered ? Theme.PRIMARY : Theme.COURSE_TEXT_MUTED);
        }
    }

    private static final class Divider extends JComponent {
        Divider() {
            setPreferredSize(new Dimension(1, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        }
        @Override protected void paintComponent(Graphics graphics) {
            graphics.setColor(Theme.DIVIDER);
            graphics.fillRect(0, 0, getWidth(), 1);
        }
    }

    private static final class EllipsizedLabel extends JLabel {
        private final String fullText;
        EllipsizedLabel(String text, Font font, Color color) {
            super(text);
            fullText = text;
            setFont(font);
            setForeground(color);
            setToolTipText(text);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Insets insets = getInsets();
            int available = getWidth() - insets.left - insets.right;
            FontMetrics metrics = graphics.getFontMetrics(getFont());
            String shown = fullText;
            if (metrics.stringWidth(shown) > available) {
                String suffix = "\u2026";
                int end = shown.length();
                while (end > 0 && metrics.stringWidth(shown.substring(0, end) + suffix) > available) end--;
                shown = end == 0 ? suffix : shown.substring(0, end).stripTrailing() + suffix;
            }
            graphics.setColor(getForeground());
            graphics.setFont(getFont());
            graphics.drawString(shown, insets.left, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }
}
