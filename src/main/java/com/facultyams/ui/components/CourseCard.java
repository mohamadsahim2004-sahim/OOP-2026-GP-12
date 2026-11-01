package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.CourseSummary;

/** Aligned, compact course card for the course overview grid. */
public class CourseCard extends Ui.Card {
    public static final int CARD_HEIGHT = 224;
    private static final Tint[] PALETTE = {
        new Tint(new Color(219, 234, 254), new Color(37, 99, 235)),
        new Tint(new Color(220, 252, 231), new Color(22, 163, 74)),
        new Tint(new Color(254, 243, 199), new Color(217, 119, 6)),
        new Tint(new Color(255, 228, 230), new Color(225, 29, 72)),
        new Tint(new Color(237, 233, 254), new Color(124, 58, 237)),
        new Tint(new Color(204, 251, 241), new Color(13, 148, 136))
    };
    private final CourseSummary course;
    private boolean hovered;

    public CourseCard(CourseSummary course, Consumer<CourseSummary> onOpen) {
        this(course, onOpen, null);
    }

    public CourseCard(CourseSummary course, Consumer<CourseSummary> onOpen, Runnable onManageEnrollment) {
        super(Theme.RADIUS_CARD, Color.WHITE, Theme.CARD_BORDER);
        this.course = course;
        setLayout(new GridBagLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(280, CARD_HEIGHT));
        setMinimumSize(new Dimension(220, CARD_HEIGHT));
        setBorder(new EmptyBorder(Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));

        Tint tint = tintFor(course);
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.fill = GridBagConstraints.HORIZONTAL;
        row.anchor = GridBagConstraints.WEST;
        row.weightx = 1;

        row.gridy = 0;
        row.weighty = 0;
        row.insets = new Insets(0, 0, Theme.CONTROL_GAP, 0);
        add(buildHeader(tint, onOpen, onManageEnrollment), row);

        row.gridy = 1;
        row.insets = new Insets(0, 0, Theme.CONTROL_GAP / 2, 0);
        add(new TwoLineTitle(course.title()), row);

        row.gridy = 2;
        row.insets = new Insets(0, 0, 0, 0);
        String lecturer = course.lecturerName() == null || course.lecturerName().isBlank()
                ? "Lecturer not listed" : course.lecturerName();
        JLabel metadata = new JLabel(lecturer + " · " + course.credits() + " credits");
        metadata.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        metadata.setForeground(Theme.COURSE_TEXT_MUTED);
        metadata.setMinimumSize(new Dimension(0, metadata.getPreferredSize().height));
        add(metadata, row);

        row.gridy = 3;
        row.weighty = 1;
        row.fill = GridBagConstraints.BOTH;
        add(Box.createGlue(), row);

        row.gridy = 4;
        row.weighty = 0;
        row.fill = GridBagConstraints.HORIZONTAL;
        add(buildProgress(tint), row);

        installOpenAction(this, onOpen);
    }

    private JPanel buildHeader(Tint tint, Consumer<CourseSummary> onOpen, Runnable onManageEnrollment) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        identity.setOpaque(false);
        JLabel badge = new JLabel(courseNumber(course.code()), SwingConstants.CENTER);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(tint.strong());
        badge.setOpaque(true);
        badge.setBackground(tint.background());
        badge.setPreferredSize(new Dimension(40, 40));
        badge.setBorder(BorderFactory.createLineBorder(tint.background(), 1, true));
        identity.add(badge);
        JLabel code = new JLabel(course.code().toUpperCase());
        code.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        code.setForeground(Theme.COURSE_TEXT_MUTED);
        identity.add(code);
        header.add(identity, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                Theme.CONTROL_GAP / 2, Theme.CONTROL_GAP / 2));
        actions.setOpaque(false);
        JLabel semester = new Ui.Pill(shortSemester(course.semester()),
                new Color(243, 244, 246), Theme.COURSE_TEXT_MUTED);
        semester.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        semester.setBorder(new EmptyBorder(3, 10, 3, 10));
        semester.setPreferredSize(new Dimension(semester.getPreferredSize().width, 24));
        actions.add(semester);
        JButton menu = new IconActionButton(Icons.Type.DOTS);
        menu.setToolTipText("Course actions");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem open = new JMenuItem("View course");
        open.addActionListener(e -> {
            if (onOpen != null) onOpen.accept(course);
        });
        JMenuItem materials = new JMenuItem("Materials");
        materials.addActionListener(e -> {
            if (onOpen != null) onOpen.accept(course);
        });
        popup.add(open);
        popup.add(materials);
        if (onManageEnrollment != null) {
            JMenuItem unenroll = new JMenuItem("Unenroll");
            unenroll.addActionListener(e -> onManageEnrollment.run());
            popup.addSeparator();
            popup.add(unenroll);
        }
        menu.addActionListener(e -> popup.show(menu, 0, menu.getHeight()));
        actions.add(menu);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JComponent buildProgress(Tint tint) {
        JPanel progressRow = new JPanel(new GridBagLayout());
        progressRow.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;

        RoundedProgressBar progress = new RoundedProgressBar(0, tint.strong());
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP / 2);
        progressRow.add(progress, c);

        JLabel percent = new JLabel(course.progressPercent() + "%", SwingConstants.RIGHT);
        percent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        percent.setForeground(Theme.COURSE_TEXT_MAIN);
        percent.setPreferredSize(new Dimension(40, 16));
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        progressRow.add(percent, c);

        Timer animation = new Timer(16, null);
        animation.addActionListener(e -> {
            int next = Math.min(course.progressPercent(), progressValue(progress) + 5);
            progress.setValue(next);
            if (next >= course.progressPercent()) animation.stop();
        });
        animation.start();
        return progressRow;
    }

    private int progressValue(RoundedProgressBar bar) {
        return bar.getValue();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D shadow = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(shadow);
        shadow.setColor(hovered ? new Color(17, 24, 39, 22) : new Color(17, 24, 39, 9));
        shadow.fillRoundRect(1, 3, getWidth() - 3, getHeight() - 4,
                Theme.RADIUS_CARD, Theme.RADIUS_CARD);
        shadow.dispose();
        super.paintComponent(graphics);
    }

    private void installOpenAction(Component component, Consumer<CourseSummary> onOpen) {
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hovered = true;
                setBorderColor(Theme.PRIMARY);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                Point point = SwingUtilities.convertPoint(component, e.getPoint(), CourseCard.this);
                if (!contains(point)) {
                    hovered = false;
                    setBorderColor(Theme.CARD_BORDER);
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (!(component instanceof JButton) && onOpen != null) onOpen.accept(course);
            }
        });
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) installOpenAction(child, onOpen);
        }
    }

    private static String courseNumber(String code) {
        String digits = code.replaceAll("\\D+", "");
        return digits.isEmpty() ? "SR" : digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
    }

    private static String shortSemester(String value) {
        return value.replace("Semester", "Sem");
    }

    private static Tint tintFor(CourseSummary course) {
        return PALETTE[Math.floorMod(course.code().hashCode(), PALETTE.length)];
    }

    private record Tint(Color background, Color strong) {}

    private static final class IconActionButton extends JButton {
        private boolean hovered;
        IconActionButton(Icons.Type icon) {
            setPreferredSize(new Dimension(28, 28));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
            putClientProperty("course.icon", icon);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (hovered) {
                g.setColor(Theme.HOVER_BG);
                g.fillOval(0, 0, getWidth(), getHeight());
            }
            Icons.paint(g, (Icons.Type) getClientProperty("course.icon"),
                    (getWidth() - 16) / 2, (getHeight() - 16) / 2, 16, Theme.COURSE_TEXT_MUTED);
            g.dispose();
        }
    }

    private static final class TwoLineTitle extends JComponent {
        private final String text;
        private final Font font = new Font("Segoe UI", Font.BOLD, 16);

        TwoLineTitle(String text) {
            this.text = text;
            setFont(font);
            setForeground(Theme.COURSE_TEXT_MAIN);
            setPreferredSize(new Dimension(120, font.getSize() * 2 + 8));
            setMinimumSize(new Dimension(0, font.getSize() * 2 + 8));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setFont(font);
            g.setColor(Theme.COURSE_TEXT_MAIN);
            FontMetrics metrics = g.getFontMetrics();
            String remaining = text;
            int baseline = metrics.getAscent();
            for (int line = 0; line < 2 && !remaining.isEmpty(); line++) {
                String displayed = fitLine(remaining, metrics, getWidth(), line == 1);
                g.drawString(displayed, 0, baseline + line * metrics.getHeight());
                if (displayed.endsWith("…")) break;
                remaining = remaining.substring(displayed.length()).stripLeading();
            }
            g.dispose();
        }

        private String fitLine(String source, FontMetrics metrics, int width, boolean lastLine) {
            if (metrics.stringWidth(source) <= width) return source;
            int end = source.length();
            while (end > 0 && metrics.stringWidth(source.substring(0, end) + (lastLine ? "…" : "")) > width) {
                end--;
            }
            while (end > 0 && end < source.length() && !Character.isWhitespace(source.charAt(end))) end--;
            if (end == 0) {
                end = source.length();
                while (end > 0 && metrics.stringWidth(source.substring(0, end) + "…") > width) end--;
                return end == 0 ? "" : source.substring(0, end) + "…";
            }
            return source.substring(0, end).stripTrailing() + (lastLine ? "…" : "");
        }
    }
}
