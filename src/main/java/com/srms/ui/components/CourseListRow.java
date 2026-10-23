package com.srms.ui.components;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Compact horizontal course presentation for list mode. */
public class CourseListRow extends Ui.Card {
    public static final int ROW_HEIGHT = 88;

    public CourseListRow(Course course, Consumer<Course> onOpen) {
        super(12, Color.WHITE, Theme.CARD_BORDER);
        setLayout(new BorderLayout(16, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(720, ROW_HEIGHT));
        setMinimumSize(new Dimension(240, ROW_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel code = new JLabel(course.code() + "  ·  " + course.semester().replace("Semester", "Sem"));
        code.setFont(Theme.FONT_SMALL);
        code.setForeground(Theme.COURSE_TEXT_MUTED);
        JLabel title = new JLabel(course.title());
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.COURSE_TEXT_MAIN);
        JLabel lecturer = new JLabel((course.lecturerName() == null ? "" : course.lecturerName())
                + "  ·  " + course.credits() + " credits");
        lecturer.setFont(Theme.FONT_SMALL);
        lecturer.setForeground(Theme.COURSE_TEXT_MUTED);
        details.add(code);
        details.add(Box.createVerticalStrut(4));
        details.add(title);
        details.add(lecturer);
        add(details, BorderLayout.CENTER);

        JPanel progress = new JPanel();
        progress.setOpaque(false);
        progress.setLayout(new BoxLayout(progress, BoxLayout.Y_AXIS));
        JLabel percent = new JLabel(course.progressPercent() + "%");
        percent.setFont(Theme.COURSE_BODY_BOLD);
        percent.setForeground(Theme.PRIMARY);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(course.progressPercent());
        bar.setPreferredSize(new Dimension(120, 6));
        bar.setBorderPainted(false);
        bar.setForeground(Theme.PRIMARY);
        bar.setBackground(Theme.CARD_BORDER);
        progress.add(percent);
        progress.add(Box.createVerticalStrut(6));
        progress.add(bar);
        add(progress, BorderLayout.EAST);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onOpen.accept(course); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { setBorderColor(Theme.PRIMARY_BORDER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { setBorderColor(Theme.CARD_BORDER); }
        });
    }
}
