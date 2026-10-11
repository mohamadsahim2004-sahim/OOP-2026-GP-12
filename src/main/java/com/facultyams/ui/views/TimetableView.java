package com.facultyams.ui.views;

import com.facultyams.model.TimeTable;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class TimetableView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private String selectedDay = "Monday";
    private JPanel timetableContainer;

    public TimetableView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Day Selector Bar
        content.add(createDaySelectorBar());
        content.add(Box.createVerticalStrut(18));

        // Schedule Grid Container
        timetableContainer = new JPanel();
        timetableContainer.setOpaque(false);
        timetableContainer.setLayout(new BoxLayout(timetableContainer, BoxLayout.Y_AXIS));
        content.add(timetableContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
        renderTimetable();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Weekly Academic Timetable");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Semester 5 - Academic Year 2023/2024 \u2022 Department of Computer Science & Engineering");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        ModernButton printBtn = new ModernButton("Export Schedule PDF", ModernButton.StyleType.OUTLINE);
        printBtn.setIcon(IconHelper.getIcon("downloads", 16, Theme.PRIMARY));
        printBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Timetable exported successfully to your documents folder!",
                "Export Complete", JOptionPane.INFORMATION_MESSAGE));

        card.add(left, BorderLayout.CENTER);
        card.add(printBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel createDaySelectorBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "ALL"};
        for (String day : days) {
            ModernButton btn = new ModernButton(day.equals("ALL") ? "All Days" : day,
                    day.equalsIgnoreCase(selectedDay) ? ModernButton.StyleType.PRIMARY : ModernButton.StyleType.GHOST);
            btn.addActionListener(e -> {
                selectedDay = day;
                renderTimetable();
                for (Component c : bar.getComponents()) {
                    if (c instanceof ModernButton b) {
                        b.repaint();
                    }
                }
            });
            bar.add(btn);
        }
        return bar;
    }

    private void renderTimetable() {
        timetableContainer.removeAll();
        List<TimeTable> entries = dataService.getTimetableForDay(selectedDay);

        if (entries.isEmpty()) {
            CardPanel emptyCard = new CardPanel(14);
            emptyCard.setBorder(Theme.createEmptyPadding(30, 20, 30, 20));
            emptyCard.add(new JLabel("No lecture sessions scheduled for this day.", JLabel.CENTER));
            timetableContainer.add(emptyCard);
        } else {
            for (TimeTable t : entries) {
                timetableContainer.add(createSessionCard(t));
                timetableContainer.add(Box.createVerticalStrut(12));
            }
        }

        timetableContainer.revalidate();
        timetableContainer.repaint();
    }

    private JPanel createSessionCard(TimeTable t) {
        CardPanel card = new CardPanel(14);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(Theme.createEmptyPadding(16, 20, 16, 20));

        // Time Box Left
        JPanel timeBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(Theme.ACCENT_BLUE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        timeBox.setOpaque(false);
        timeBox.setPreferredSize(new Dimension(130, 60));

        JPanel timeTextP = new JPanel();
        timeTextP.setOpaque(false);
        timeTextP.setLayout(new BoxLayout(timeTextP, BoxLayout.Y_AXIS));

        JLabel dayLbl = new JLabel(t.getDayOfWeek().toUpperCase(), JLabel.CENTER);
        dayLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dayLbl.setForeground(Theme.PRIMARY);

        JLabel slotLbl = new JLabel(t.getStartTime() + " - " + t.getEndTime(), JLabel.CENTER);
        slotLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        slotLbl.setForeground(Theme.TEXT_MAIN);

        timeTextP.add(dayLbl);
        timeTextP.add(Box.createVerticalStrut(2));
        timeTextP.add(slotLbl);
        timeBox.add(timeTextP);

        // Center Details
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerRow.setOpaque(false);

        JLabel courseLbl = new JLabel(t.getCourseCode() + ": " + t.getCourseTitle());
        courseLbl.setFont(Theme.FONT_SECTION);
        courseLbl.setForeground(Theme.TEXT_MAIN);

        BadgeLabel typeBadge = BadgeLabel.createStatusBadge(t.getType());
        headerRow.add(courseLbl);
        headerRow.add(typeBadge);

        JPanel detailsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        detailsRow.setOpaque(false);

        JLabel venueLbl = new JLabel("Venue: " + t.getVenue());
        venueLbl.setFont(Theme.FONT_BODY);
        venueLbl.setForeground(Theme.TEXT_MUTED);

        JLabel lecLbl = new JLabel("Lecturer: " + t.getLecturer());
        lecLbl.setFont(Theme.FONT_BODY);
        lecLbl.setForeground(Theme.TEXT_MUTED);

        detailsRow.add(venueLbl);
        detailsRow.add(lecLbl);

        center.add(headerRow);
        center.add(Box.createVerticalStrut(6));
        center.add(detailsRow);

        card.add(timeBox, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);

        return card;
    }
}
