package com.srms.ui.components;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

/** Compact themed course card used only by the course overview. */
public class CourseCard extends Ui.Card {
    public static final int CARD_HEIGHT = 224;
    private static final Color[] TINTS = {
        new Color(219, 234, 254), new Color(220, 252, 231), new Color(254, 243, 199),
        new Color(255, 228, 230), new Color(237, 233, 254), new Color(204, 251, 241)
    };
    private final Course course;
    private boolean hover;

    public CourseCard(Course course, Consumer<Course> onOpen) {
        this(course, onOpen, null);
    }

    public CourseCard(Course course, Consumer<Course> onOpen, Runnable onManageEnrollment) {
        super(16, Color.WHITE, Theme.CARD_BORDER);
        this.course = course;
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(280, CARD_HEIGHT));
        setMinimumSize(new Dimension(220, CARD_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JPanel codeGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        codeGroup.setOpaque(false);
        JLabel icon = new JLabel(course.code().isEmpty() ? "C" : course.code().substring(0, 1).toUpperCase());
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        icon.setForeground(Theme.PRIMARY);
        icon.setOpaque(true);
        icon.setBackground(tint(course));
        icon.setPreferredSize(new Dimension(32, 32));
        codeGroup.add(icon);
        JLabel code = new JLabel(course.code().toUpperCase());
        code.setFont(Theme.FONT_SMALL);
        code.setForeground(Theme.TEXT_MUTED);
        codeGroup.add(code);
        top.add(codeGroup, BorderLayout.WEST);

        JLabel semester = new Ui.Pill(shortSemester(course.semester()), new Color(243, 244, 246), Theme.TEXT_MUTED);
        semester.setFont(Theme.FONT_SMALL);
        top.add(semester, BorderLayout.EAST);
        content.add(top);
        content.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("<html><body style='width: 220px'>" + escape(course.title()) + "</body></html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(8));

        String lecturer = course.lecturerName() == null || course.lecturerName().isBlank()
                ? "Lecturer not listed" : course.lecturerName();
        JLabel details = new JLabel(lecturer + " · " + course.credits() + " credits");
        details.setFont(Theme.FONT_SMALL);
        details.setForeground(Theme.TEXT_MUTED);
        content.add(details);
        content.add(Box.createVerticalGlue());

        JPanel progressRow = new JPanel(new BorderLayout(8, 0));
        progressRow.setOpaque(false);
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(0);
        progress.setPreferredSize(new Dimension(100, 6));
        progress.setBorderPainted(false);
        progress.setBackground(Theme.CARD_BORDER);
        progress.setForeground(tint(course).darker());
        progress.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics graphics, JComponent component) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g2);
                int y = component.getHeight() / 2 - 3;
                g2.setColor(Theme.CARD_BORDER);
                g2.fillRoundRect(0, y, component.getWidth(), 6, 6, 6);
                int filled = (int) (component.getWidth() * progress.getPercentComplete());
                g2.setColor(tint(course).darker());
                g2.fillRoundRect(0, y, filled, 6, 6, 6);
                g2.dispose();
            }
        });
        JLabel percent = new JLabel(course.progressPercent() + "%");
        percent.setFont(Theme.FONT_TINY);
        percent.setForeground(Theme.TEXT_MAIN);
        progressRow.add(progress, BorderLayout.CENTER);
        progressRow.add(percent, BorderLayout.EAST);
        content.add(progressRow);
        add(content, BorderLayout.CENTER);

        JButton menu = new JButton("⋯");
        menu.setFont(new Font("Segoe UI", Font.BOLD, 18));
        menu.setForeground(Theme.TEXT_MUTED);
        menu.setFocusPainted(false);
        menu.setBorderPainted(false);
        menu.setContentAreaFilled(false);
        menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menu.setPreferredSize(new Dimension(30, 30));
        JPopupMenu popup = new JPopupMenu();
        JMenuItem open = new JMenuItem("View course");
        open.addActionListener(e -> onOpen.accept(course));
        JMenuItem materials = new JMenuItem("Materials");
        materials.addActionListener(e -> onOpen.accept(course));
        popup.add(open);
        popup.add(materials);
        if (onManageEnrollment != null) {
            JMenuItem unenroll = new JMenuItem("Unenroll");
            unenroll.addActionListener(e -> onManageEnrollment.run());
            popup.addSeparator();
            popup.add(unenroll);
        }
        menu.addActionListener(e -> popup.show(menu, 0, menu.getHeight()));
        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        topRight.add(menu, BorderLayout.EAST);
        add(topRight, BorderLayout.NORTH);

        Timer animation = new Timer(16, null);
        animation.addActionListener(e -> {
            int value = Math.min(course.progressPercent(), progress.getValue() + 5);
            progress.setValue(value);
            if (value >= course.progressPercent()) animation.stop();
        });
        animation.start();
        installHover(this, onOpen);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D shadow = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(shadow);
        shadow.setColor(hover ? new Color(15, 23, 42, 18) : new Color(15, 23, 42, 8));
        shadow.fillRoundRect(1, 3, getWidth() - 3, getHeight() - 4, 16, 16);
        shadow.dispose();
        super.paintComponent(graphics);
    }

    private void installHover(Component component, Consumer<Course> onOpen) {
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hover = true;
                setBorderColor(Theme.PRIMARY_BORDER);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(component, e.getPoint(), CourseCard.this);
                if (!contains(p)) {
                    hover = false;
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
            for (Component child : container.getComponents()) installHover(child, onOpen);
        }
    }

    private static Color tint(Course course) {
        return TINTS[Math.floorMod(course.code().hashCode(), TINTS.length)];
    }

    private static String shortSemester(String value) {
        return value.replace("Semester", "Sem");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
