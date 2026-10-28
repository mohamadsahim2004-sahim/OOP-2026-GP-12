package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.*;

/**
 * Modern 64px course attendance row with thin progress bar, eligibility pill,
 * smooth expansion animation, absent/medical date chips, and smart hints.
 */
public class ExpandableCourseRow extends JPanel {
    private static final DateTimeFormatter CHIP_DATE_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private final CourseAttendance courseAttendance;
    private final List<SessionAttendance> sessions;
    private boolean expanded = false;

    private final JPanel headerRow = new JPanel(new BorderLayout(16, 0));
    private final JPanel detailsWrapper = new JPanel(new BorderLayout());
    private final JPanel detailsContent = new JPanel();
    private Timer animTimer;

    public ExpandableCourseRow(CourseAttendance courseAttendance, List<SessionAttendance> sessions) {
        this.courseAttendance = courseAttendance;
        this.sessions = sessions != null ? sessions : List.of();

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        initHeader();
        initDetails();
    }

    private void initHeader() {
        headerRow.setOpaque(false);
        headerRow.setPreferredSize(new Dimension(800, 64));
        headerRow.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 1. Left: Code + Title
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(240, 40));

        JLabel codeLbl = new JLabel(courseAttendance.courseCode());
        codeLbl.setFont(Theme.FONT_BODY_BOLD);
        codeLbl.setForeground(Theme.PRIMARY);

        JLabel titleLbl = new JLabel(courseAttendance.courseTitle());
        titleLbl.setFont(Theme.FONT_BODY);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        left.add(codeLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(titleLbl);
        headerRow.add(left, BorderLayout.WEST);

        // 2. Middle: Thin progress bar
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        ThinProgressBar bar = new ThinProgressBar(courseAttendance.percent(), AttendancePolicy.MIN_PERCENT);
        bar.setPreferredSize(new Dimension(180, 16));
        center.add(bar);
        headerRow.add(center, BorderLayout.CENTER);

        // 3. Right: Counts + Percent + Pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        right.setOpaque(false);

        JLabel countLbl = new JLabel(courseAttendance.hasRecords()
                ? (courseAttendance.attended() + courseAttendance.medicalApproved()) + " / " + courseAttendance.totalSessions() + " sessions"
                : "No sessions recorded");
        countLbl.setFont(Theme.FONT_SMALL);
        countLbl.setForeground(Theme.TEXT_MUTED);

        JLabel percentLbl = new JLabel(courseAttendance.hasRecords()
                ? String.format(Locale.ENGLISH, "%.1f%%", courseAttendance.percent()) : "\u2014");
        percentLbl.setFont(Theme.FONT_BODY_BOLD);
        percentLbl.setForeground(!courseAttendance.hasRecords() ? Theme.TEXT_MUTED
                : courseAttendance.percent() >= AttendancePolicy.MIN_PERCENT ? Theme.TEXT_MAIN : Theme.DANGER);

        Ui.Pill pill;
        if (!courseAttendance.hasRecords()) {
            pill = new Ui.Pill("No data yet", Theme.DIVIDER, Theme.TEXT_MUTED);
        } else if (courseAttendance.percent() >= AttendancePolicy.MIN_PERCENT) {
            pill = new Ui.Pill("Eligible", Theme.SUCCESS_BG, Theme.SUCCESS);
        } else if (courseAttendance.percent() >= AttendancePolicy.MIN_PERCENT - 5.0) {
            pill = new Ui.Pill("At risk", Theme.WARNING_BG, Theme.WARNING);
        } else {
            pill = new Ui.Pill("Not eligible", Theme.DANGER_BG, Theme.DANGER);
        }

        right.add(countLbl);
        right.add(percentLbl);
        right.add(pill);
        headerRow.add(right, BorderLayout.EAST);

        headerRow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });

        add(headerRow, BorderLayout.NORTH);
    }

    private void initDetails() {
        detailsContent.setLayout(new BoxLayout(detailsContent, BoxLayout.Y_AXIS));
        detailsContent.setOpaque(true);
        detailsContent.setBackground(new Color(249, 250, 251)); // #F9FAFB
        detailsContent.setBorder(BorderFactory.createEmptyBorder(12, 22, 14, 22));

        // Chips for absent and medical dates
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        chipsRow.setOpaque(false);

        JLabel absentHeader = new JLabel("Recent absences:");
        absentHeader.setFont(Theme.FONT_TINY);
        absentHeader.setForeground(Theme.TEXT_MUTED);
        chipsRow.add(absentHeader);

        boolean hasDates = false;
        for (SessionAttendance sa : sessions) {
            if (sa.status() == SessionStatus.ABSENT) {
                hasDates = true;
                chipsRow.add(createDateChip(sa.session().date().format(CHIP_DATE_FMT), Theme.DANGER_BG, Theme.DANGER));
            } else if (sa.status() == SessionStatus.MEDICAL_APPROVED) {
                hasDates = true;
                chipsRow.add(createDateChip(sa.session().date().format(CHIP_DATE_FMT) + " (Med)", Theme.INFO_BG, Theme.INFO));
            }
        }
        if (!hasDates) {
            JLabel noneLbl = new JLabel("No absences recorded");
            noneLbl.setFont(Theme.FONT_SMALL);
            noneLbl.setForeground(Theme.SUCCESS);
            chipsRow.add(noneLbl);
        }
        detailsContent.add(chipsRow);
        detailsContent.add(Box.createVerticalStrut(8));

        // Smart Calculation Hint
        String hintText = computeSmartHint();
        JLabel hintLbl = new JLabel("💡 " + hintText);
        hintLbl.setFont(Theme.FONT_BODY_BOLD);
        hintLbl.setForeground(courseAttendance.isEligible() ? Theme.SUCCESS : Theme.WARNING);
        detailsContent.add(hintLbl);

        detailsWrapper.setOpaque(false);
        detailsWrapper.add(detailsContent, BorderLayout.CENTER);
        detailsWrapper.setVisible(false);
        detailsWrapper.setPreferredSize(new Dimension(0, 0));

        add(detailsWrapper, BorderLayout.CENTER);
    }

    private JPanel createDateChip(String text, Color bg, Color fg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        p.setOpaque(true);
        p.setBackground(bg);
        p.setBorder(BorderFactory.createLineBorder(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 60), 1));
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_TINY);
        l.setForeground(fg);
        p.add(l);
        return p;
    }

    /** Uses the project rule: percentage = (present + approved medical) / total sessions of the course. */
    private String computeSmartHint() {
        if (!courseAttendance.hasRecords()) {
            return "No sessions have been recorded for this course yet.";
        }
        int total = courseAttendance.totalSessions();
        int have = courseAttendance.attended() + courseAttendance.medicalApproved();
        int required = (int) Math.ceil(AttendancePolicy.MIN_PERCENT / 100.0 * total);
        int remaining = Math.max(0, total - courseAttendance.recorded());
        int pct = (int) AttendancePolicy.MIN_PERCENT;
        if (have >= required) {
            int canMiss = Math.max(0, have + remaining - required);
            return remaining == 0 ? "You meet the " + pct + "% attendance requirement."
                    : "You meet the " + pct + "% requirement. You can still miss " + Math.min(canMiss, remaining)
                    + " of the remaining " + remaining + " session" + (remaining == 1 ? "" : "s") + ".";
        }
        int needed = required - have;
        if (needed <= remaining) {
            return "Attend at least " + needed + " of the remaining " + remaining + " session"
                    + (remaining == 1 ? "" : "s") + " to reach " + pct + "%.";
        }
        return pct + "% can no longer be reached this semester (" + remaining + " session"
                + (remaining == 1 ? "" : "s") + " left).";
    }

    public void toggle() {
        setExpanded(!expanded, true);
    }

    public void setExpanded(boolean exp, boolean animate) {
        if (this.expanded == exp) return;
        this.expanded = exp;

        int fullH = detailsContent.getPreferredSize().height;
        if (!animate) {
            detailsWrapper.setVisible(exp);
            detailsWrapper.setPreferredSize(exp ? null : new Dimension(0, 0));
            revalidate();
            repaint();
            return;
        }

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        detailsWrapper.setVisible(true);

        int targetH = exp ? fullH : 0;
        int[] curH = { detailsWrapper.getHeight() };
        int step = Math.max(12, Math.abs(targetH - curH[0]) / 8);

        animTimer = new Timer(15, e -> {
            curH[0] = expanded ? Math.min(targetH, curH[0] + step) : Math.max(targetH, curH[0] - step);
            detailsWrapper.setPreferredSize(new Dimension(getWidth(), curH[0]));
            detailsWrapper.revalidate();
            Container parent = getParent();
            if (parent != null) parent.revalidate();

            if (curH[0] == targetH) {
                animTimer.stop();
                if (!expanded) detailsWrapper.setVisible(false);
                else detailsWrapper.setPreferredSize(null);
                revalidate();
                repaint();
            }
        });
        animTimer.start();
    }

    public CourseAttendance getCourseAttendance() {
        return courseAttendance;
    }
}
