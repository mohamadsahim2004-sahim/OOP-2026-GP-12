package com.facultyams.ui.technical;

import com.facultyams.model.DashboardData;
import com.facultyams.model.MedicalRow;
import com.facultyams.model.RecentSession;
import com.facultyams.service.TechnicalDashboardService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.Display;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Technical Officer home page: live totals (students, attendance records, pending / approved / rejected
 * medicals), the most recently recorded sessions, the latest pending approvals and quick navigation.
 */
public class TODashboardPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private final transient TechnicalDashboardService service = new TechnicalDashboardService();
    private final StatCard students = new StatCard("Students", AdminTheme.CARD_BLUE);
    private final StatCard records = new StatCard("Attendance Records", AdminTheme.CARD_PURPLE);
    private final StatCard presentToday = new StatCard("Present Today", AdminTheme.CARD_GREEN);
    private final StatCard absentToday = new StatCard("Absent Today", AdminTheme.DANGER);
    private final StatCard pending = new StatCard("Pending Medicals", AdminTheme.CARD_ORANGE);
    private final StatCard approved = new StatCard("Approved Medicals", AdminTheme.CARD_GREEN);
    private final StatCard rejected = new StatCard("Rejected Medicals", AdminTheme.DANGER);

    private final ReadOnlyTableModel recentModel = new ReadOnlyTableModel(
            new String[] {"Date", "Course", "Type", "Session", "Present", "Absent", "Late"},
            new Class<?>[] {String.class, String.class, String.class, Integer.class, Integer.class, Integer.class, Integer.class});
    private final ReadOnlyTableModel pendingModel = ReadOnlyTableModel.ofStrings(
            "Reg No", "Student", "Course", "Session date", "Submitted", "Reason");
    private final JLabel recentEmpty = label("No attendance has been recorded yet.");
    private final JLabel pendingEmpty = label("No medicals are waiting for approval.");

    public TODashboardPanel(Consumer<String> navigate) {
        JPanel cards = new JPanel(new WrapLayout(FlowLayout.LEFT, 14, 8));
        for (StatCard c : List.of(students, records, presentToday, absentToday, pending, approved, rejected)) {
            cards.add(c);
        }

        JPanel quick = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 6));
        quick.add(quickButton("Mark Attendance", TechnicalOfficerShell.PAGE_ATTENDANCE, navigate));
        quick.add(quickButton("Attendance Records", TechnicalOfficerShell.PAGE_RECORDS, navigate));
        quick.add(quickButton("Medical Records", TechnicalOfficerShell.PAGE_MEDICAL_RECORDS, navigate));
        quick.add(quickButton("Submit Medical", TechnicalOfficerShell.PAGE_MEDICAL_SUBMIT, navigate));
        quick.add(quickButton("Approve Medicals", TechnicalOfficerShell.PAGE_MEDICAL_APPROVAL, navigate));
        quick.add(quickButton("Attendance Report", TechnicalOfficerShell.PAGE_ATTENDANCE_REPORT, navigate));
        quick.add(quickButton("Medical Report", TechnicalOfficerShell.PAGE_MEDICAL_REPORT, navigate));
        JButton refresh = button("Refresh", "Reload the figures from the database", java.awt.event.KeyEvent.VK_R, this::refresh);
        quick.add(refresh);

        JPanel lists = new JPanel(new GridLayout(1, 2, 14, 0));
        lists.add(section("Recent Attendance Activity", recentEmpty, table(recentModel)));
        lists.add(section("Pending Medical Approvals", pendingEmpty, table(pendingModel)));
        lists.setPreferredSize(new Dimension(100, 330));

        JPanel column = new TrackingPanel(new BorderLayout(0, 8));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(statusArea());
        top.add(cards);
        JLabel navHeading = heading("Quick Navigation");
        navHeading.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        top.add(navHeading);
        top.add(quick);
        alignLeft(top);
        column.add(top, BorderLayout.NORTH);
        column.add(lists, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(column);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private static JButton quickButton(String text, String pageKey, Consumer<String> navigate) {
        JButton b = button(text, "Open " + text, 0, () -> navigate.accept(pageKey));
        b.setName("quick." + pageKey);
        return b;
    }

    private static JPanel section(String title, JLabel empty, JTable table) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JPanel head = new JPanel(new BorderLayout());
        head.add(heading(title), BorderLayout.NORTH);
        head.add(empty, BorderLayout.SOUTH);
        empty.setVisible(false);
        p.add(head, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        for (StatCard c : List.of(students, records, presentToday, absentToday, pending, approved, rejected)) {
            c.setValue("...");
        }
        load("Loading the dashboard...", service::load, this::show);
    }

    private void show(DashboardData d) {
        students.setValue(String.valueOf(d.students()));
        records.setValue(String.valueOf(d.attendanceRecords()));
        presentToday.setValue(d.presentToday() + (d.lateToday() > 0 ? " (+" + d.lateToday() + " late)" : ""));
        absentToday.setValue(String.valueOf(d.absentToday()));
        pending.setValue(String.valueOf(d.pendingMedicals()));
        approved.setValue(String.valueOf(d.approvedMedicals()));
        rejected.setValue(String.valueOf(d.rejectedMedicals()));

        List<Object[]> recent = new ArrayList<>();
        for (RecentSession r : d.recentSessions()) {
            recent.add(new Object[] {Display.date(r.date()), r.courseCode(), Display.capitalize(r.type()),
                    r.sessionNo(), r.present(), r.absent(), r.late()});
        }
        recentModel.setRows(recent);
        recentEmpty.setVisible(recent.isEmpty());

        List<Object[]> waiting = new ArrayList<>();
        for (MedicalRow m : d.pendingList()) {
            waiting.add(new Object[] {m.registrationNo(), m.studentName(), m.courseCode() + " " + Display.capitalize(m.type()),
                    Display.date(m.sessionDate()), Display.date(m.submittedDate()), m.reason()});
        }
        pendingModel.setRows(waiting);
        pendingEmpty.setVisible(waiting.isEmpty());
        revalidate();
    }

    /** Rounded coloured tile with one big number and its caption. */
    private static final class StatCard extends JPanel {

        private static final long serialVersionUID = 1L;
        private final JLabel value = new JLabel("...", SwingConstants.CENTER);
        private final Color color;

        StatCard(String caption, Color color) {
            super(new BorderLayout());
            this.color = color;
            putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setPreferredSize(new Dimension(172, 88));
            value.setFont(value.getFont().deriveFont(java.awt.Font.BOLD, 28f));
            value.setForeground(Color.WHITE);
            JLabel cap = new JLabel(caption, SwingConstants.CENTER);
            cap.setFont(AdminTheme.FONT_BOLD);
            cap.setForeground(Color.WHITE);
            add(value, BorderLayout.CENTER);
            add(cap, BorderLayout.SOUTH);
            getAccessibleContext().setAccessibleName(caption);
        }

        void setValue(String text) {
            value.setText(text);
            getAccessibleContext().setAccessibleDescription(value.getText());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}