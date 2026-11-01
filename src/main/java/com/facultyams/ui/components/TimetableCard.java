package com.facultyams.ui.components;

import com.facultyams.model.timetable.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.*;

/** Full-width timetable row with type accent, vector metadata, and reminder actions. */
public final class TimetableCard extends RoundedPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private final boolean completed;
    private final boolean faded;
    private float hoverAmount;
    private Timer hoverTimer;

    public TimetableCard(ClassSession session, String status, String countdown, boolean reminderEnabled,
                         Consumer<Boolean> reminderAction, Runnable editAction, Runnable deleteAction) {
        this(session, status, countdown, reminderEnabled, false, reminderAction, editAction, deleteAction);
    }

    public TimetableCard(ClassSession session, String status, String countdown, boolean reminderEnabled, boolean faded,
                         Consumer<Boolean> reminderAction, Runnable editAction, Runnable deleteAction) {
        super(Theme.CARD_RADIUS);
        this.completed = status.startsWith("Completed");
        this.faded = faded || completed;
        setOpaque(false);
        setLayout(new BorderLayout(Theme.SPACE_3, 0));
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_2, Theme.CARD_PADDING,
                Theme.SPACE_2, Theme.CARD_PADDING));
        setPreferredSize(new Dimension(700, 100));
        setMinimumSize(new Dimension(0, 100));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(new AccentBar(session.getType()));
        left.add(Box.createHorizontalStrut(Theme.SPACE_2));
        left.add(createTimeBlock(session));
        add(left, BorderLayout.WEST);
        add(createDetails(session), BorderLayout.CENTER);
        add(createActions(session, status, countdown, reminderEnabled, reminderAction, editAction, deleteAction),
                BorderLayout.EAST);
        installHover();
    }

    private JComponent createTimeBlock(ClassSession session) {
        RoundedPanel time = new RoundedPanel(10, Theme.ACCENT_BLUE_BG);
        time.setLayout(new BoxLayout(time, BoxLayout.Y_AXIS));
        time.setBorder(BorderFactory.createEmptyBorder(7, 6, 7, 6));
        time.setPreferredSize(new Dimension(96, 64));
        time.setMinimumSize(new Dimension(96, 64));
        time.setMaximumSize(new Dimension(96, 64));
        JPanel startRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        startRow.setOpaque(false);
        startRow.add(new JLabel(new TimetableIcon(TimetableIcon.Shape.CLOCK, Theme.PRIMARY, 10)));
        JLabel start = new JLabel(session.getStart().format(TIME));
        start.setFont(Theme.FONT_BODY_BOLD.deriveFont(15f));
        start.setForeground(Theme.PRIMARY);
        startRow.add(start);
        JLabel end = new JLabel(session.getEnd().format(TIME));
        end.setFont(Theme.FONT_SMALL);
        end.setForeground(Theme.TEXT_MUTED);
        JSeparator divider = new JSeparator();
        divider.setForeground(Theme.PRIMARY_BORDER);
        divider.setBackground(Theme.PRIMARY_BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        time.add(startRow);
        time.add(Box.createVerticalStrut(3));
        time.add(divider);
        time.add(Box.createVerticalStrut(3));
        time.add(end);
        return time;
    }

    private JComponent createDetails(ClassSession session) {
        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 8, 0);

        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        title.setOpaque(false);
        JLabel code = new JLabel(session.getCourse().getCode());
        code.setFont(Theme.FONT_CARD_TITLE);
        code.setForeground(Theme.PRIMARY);
        JLabel name = new JLabel(session.getDisplayName());
        name.setFont(Theme.FONT_CARD_TITLE);
        name.setForeground(Theme.TEXT_MAIN);
        title.add(code);
        title.add(name);
        Color[] typeColors = typeColors(session.getType());
        title.add(new Badge(session.getType().getLabel(), typeColors[0], typeColors[1]));
        details.add(title, c);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_4, 0));
        meta.setOpaque(false);
        meta.add(metadata(new TimetableIcon(TimetableIcon.Shape.LOCATION, Theme.TEXT_MUTED, 14),
                session.getVenue().getName()));
        meta.add(metadata(new TimetableIcon(TimetableIcon.Shape.PERSON, Theme.TEXT_MUTED, 14),
                session.getLecturer().getName()));
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        details.add(meta, c);
        return details;
    }

    private JPanel metadata(Icon icon, String text) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        group.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        JLabel value = new JLabel(text);
        value.setFont(Theme.FONT_BODY);
        value.setForeground(Theme.TEXT_MUTED);
        group.add(iconLabel);
        group.add(value);
        return group;
    }

    private JComponent createActions(ClassSession session, String status, String countdown,
                                     boolean reminderEnabled, Consumer<Boolean> reminderAction,
                                     Runnable editAction, Runnable deleteAction) {
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (!status.isBlank()) {
            JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            chipRow.setOpaque(false);
            if (!countdown.isBlank() && status.startsWith("Up next")) status = "Up next · " + countdown.replace("Starts ", "");
            chipRow.add(new StatusChip(status));
            actions.add(chipRow);
        }
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controlRow.setOpaque(false);
        if (session.getType() == ClassType.PERSONAL) {
            JButton edit = miniButton("Edit");
            JButton delete = miniButton("Delete");
            edit.addActionListener(e -> editAction.run());
            delete.addActionListener(e -> deleteAction.run());
            controlRow.add(edit);
            controlRow.add(delete);
        } else {
            IconButton reminder = new IconButton(
                    new TimetableIcon(TimetableIcon.Shape.BELL_OFF, Theme.TEXT_MUTED, 16),
                    new TimetableIcon(TimetableIcon.Shape.BELL, Theme.PRIMARY, 16),
                    reminderEnabled, reminderEnabled ? "Disable reminder" : "Enable reminder");
            reminder.addActionListener(e -> {
                reminder.setToolTipText(reminder.isSelected() ? "Disable reminder" : "Enable reminder");
                reminderAction.accept(reminder.isSelected());
            });
            controlRow.add(reminder);
        }
        if (!status.isBlank()) actions.add(Box.createVerticalStrut(4));
        actions.add(controlRow);
        return actions;
    }

    private static JButton miniButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_TINY);
        button.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(Theme.PRIMARY);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static Color[] typeColors(ClassType type) {
        return switch (type) {
            case LECTURE -> new Color[]{Theme.PRIMARY_LIGHT, Theme.PRIMARY};
            case PRACTICAL -> new Color[]{Theme.TYPE_PRACTICAL_BG, Theme.ACCENT_PURPLE};
            case TUTORIAL -> new Color[]{Theme.TYPE_TUTORIAL_BG, Theme.TYPE_TUTORIAL};
            case PERSONAL -> new Color[]{Theme.TYPE_PERSONAL_BG, Theme.TYPE_PERSONAL};
        };
    }

    private void installHover() {
        MouseAdapter listener = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animateHover(true); }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), TimetableCard.this);
                if (!contains(p)) animateHover(false);
            }
        };
        addMouseListener(listener);
        for (Component component : getComponents()) component.addMouseListener(listener);
    }

    private void animateHover(boolean hovered) {
        if (hoverTimer != null) hoverTimer.stop();
        hoverTimer = new Timer(15, event -> {
            hoverAmount += hovered ? 0.15f : -0.15f;
            hoverAmount = Math.max(0, Math.min(1, hoverAmount));
            repaint();
            if ((hovered && hoverAmount >= 1) || (!hovered && hoverAmount <= 0)) ((Timer) event.getSource()).stop();
        });
        hoverTimer.start();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        float alpha = faded ? 0.70f : 1f;
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(new Color(15, 23, 42, 10 + Math.round(hoverAmount * 7)));
        g.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
        g.setColor(new Color(15, 23, 42, 6 + Math.round(hoverAmount * 4)));
        g.fillRoundRect(0, 2, getWidth() - 2, getHeight() - 3, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
        super.paintComponent(g);
        g.setColor(hoverAmount > 0
                ? blend(Theme.TIMETABLE_CARD_BORDER, Theme.PRIMARY_BORDER, hoverAmount)
                : Theme.TIMETABLE_CARD_BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
        g.dispose();
    }

    @Override protected void paintChildren(Graphics graphics) {
        if (!faded) {
            super.paintChildren(graphics);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.SrcOver.derive(0.70f));
        super.paintChildren(g);
        g.dispose();
    }

    private static Color blend(Color from, Color to, float amount) {
        return new Color((int) (from.getRed() + (to.getRed() - from.getRed()) * amount),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
    }

    private static final class AccentBar extends JComponent {
        private final Color accent;
        private AccentBar(ClassType type) {
            accent = switch (type) {
                case LECTURE -> Theme.PRIMARY;
                case PRACTICAL -> Theme.ACCENT_PURPLE;
                case TUTORIAL -> Theme.TYPE_TUTORIAL;
                case PERSONAL -> Theme.TYPE_PERSONAL;
            };
            setPreferredSize(new Dimension(4, 64));
            setMinimumSize(new Dimension(4, 64));
            setMaximumSize(new Dimension(4, 64));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(accent);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            g.dispose();
        }
    }
}
