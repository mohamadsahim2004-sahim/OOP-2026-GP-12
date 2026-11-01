package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.Ui;
import com.facultyams.model.*;
import com.facultyams.service.SubmissionService;

/**
 * Activity row (~64px) with icon, details, hover highlight, and actions.
 */
public class ActivityRow extends JPanel {
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH);

    private final Activity activity;
    private final Consumer<Assignment> onAssignmentClick;
    private final SubmissionService submissionService;
    private final String studentId;
    private boolean hover = false;
    private StatusPill statusPill;

    public ActivityRow(Activity activity, String studentId, SubmissionService submissionService, Consumer<Assignment> onAssignmentClick) {
        this.activity = activity;
        this.studentId = studentId;
        this.submissionService = submissionService;
        this.onAssignmentClick = onAssignmentClick;

        setLayout(new BorderLayout(14, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(500, 64));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        initUI();
        initListeners();
    }

    private void initUI() {
        if (activity instanceof Announcement a) {
            ActivityIcon icon = new ActivityIcon(ActivityIcon.Type.ANNOUNCEMENT);
            add(icon, BorderLayout.WEST);

            JLabel title = new JLabel(a.title());
            title.setFont(Theme.FONT_BODY_BOLD);
            title.setForeground(Theme.TEXT_MAIN);
            add(title, BorderLayout.CENTER);

        } else if (activity instanceof Resource r) {
            ActivityIcon icon = new ActivityIcon(ActivityIcon.Type.RESOURCE, r.fileType());
            add(icon, BorderLayout.WEST);

            JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            center.setOpaque(false);

            JLabel title = new JLabel(r.title());
            title.setFont(Theme.FONT_BODY_BOLD);
            title.setForeground(Theme.PRIMARY);

            Ui.Pill tag = new Ui.Pill(r.fileType(), new Color(241, 245, 249), Theme.TEXT_MUTED);

            center.add(title);
            center.add(tag);
            add(center, BorderLayout.CENTER);

        } else if (activity instanceof Assignment as) {
            ActivityIcon icon = new ActivityIcon(ActivityIcon.Type.ASSIGNMENT);
            add(icon, BorderLayout.WEST);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            JLabel title = new JLabel(as.title());
            title.setFont(Theme.FONT_BODY_BOLD);
            title.setForeground(Theme.PRIMARY);

            String datesText = String.format("Opened: %s    Due: %s",
                    as.opensAt() != null ? as.opensAt().format(DATE_FMT) : "Now",
                    as.dueAt() != null ? as.dueAt().format(DATE_FMT) : "No due date");
            JLabel dates = new JLabel(datesText);
            dates.setFont(Theme.FONT_SMALL);
            dates.setForeground(Theme.TEXT_MUTED);

            center.add(title);
            center.add(Box.createVerticalStrut(2));
            center.add(dates);
            add(center, BorderLayout.CENTER);

            // Status pill on the right
            SubmissionStatus status = resolveStatus(as);
            statusPill = new StatusPill(status);
            JPanel east = new JPanel(new GridBagLayout());
            east.setOpaque(false);
            east.add(statusPill);
            add(east, BorderLayout.EAST);
        }
    }

    private SubmissionStatus resolveStatus(Assignment as) {
        if (submissionService != null && studentId != null) {
            var subOpt = submissionService.find(studentId, as.id());
            if (subOpt.isPresent()) {
                return subOpt.get().status();
            }
        }
        if (as.dueAt() != null && java.time.LocalDateTime.now().isAfter(as.dueAt())) {
            return SubmissionStatus.LATE;
        }
        return SubmissionStatus.NOT_SUBMITTED;
    }

    public void updateStatus() {
        if (activity instanceof Assignment as && statusPill != null) {
            statusPill.setStatus(resolveStatus(as));
        }
    }

    private void initListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick();
            }
        });
    }

    private void handleClick() {
        if (activity instanceof Assignment as) {
            if (onAssignmentClick != null) {
                onAssignmentClick.accept(as);
            }
        } else if (activity instanceof Resource r) {
            handleResourceOpen(r);
        } else if (activity instanceof Announcement a) {
            JOptionPane.showMessageDialog(this,
                    "No new announcements for this course.",
                    a.title(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleResourceOpen(Resource r) {
        File file = new File(r.path());
        if (file.exists() && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
                return;
            } catch (Exception ignored) {
            }
        }
        // Save as / Download prompt
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(r.title() + "." + r.fileType().toLowerCase()));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "File downloaded successfully to: " + chooser.getSelectedFile().getName(),
                    "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        if (hover) {
            g2.setColor(Theme.HOVER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        // 1px subtle bottom divider
        g2.setColor(Theme.DIVIDER);
        g2.drawLine(16, getHeight() - 1, getWidth() - 16, getHeight() - 1);

        g2.dispose();
        super.paintComponent(g);
    }
}
