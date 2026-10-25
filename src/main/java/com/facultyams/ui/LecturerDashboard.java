package com.facultyams.ui;

import com.facultyams.model.Lecturer;
import com.facultyams.model.Notice;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.service.LecturerService;
import com.facultyams.service.NoticeService;
import com.facultyams.ui.admin.Refreshable;
import com.facultyams.ui.lecturer.CourseMaterialsPanel;
import com.facultyams.ui.lecturer.DashboardHomePanel;
import com.facultyams.ui.lecturer.EligibilityPanel;
import com.facultyams.ui.lecturer.EvaluationComponentsPanel;
import com.facultyams.ui.lecturer.LecturerAttendancePanel;
import com.facultyams.ui.lecturer.LecturerMedicalPanel;
import com.facultyams.ui.lecturer.LecturerUi;
import com.facultyams.ui.lecturer.MarksEntryPanel;
import com.facultyams.ui.lecturer.ResultsPanel;
import com.facultyams.ui.lecturer.StudentResultsPanel;
import com.facultyams.ui.lecturer.StudentsPanel;
import com.facultyams.util.UIUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Lecturer window. Opened by {@link DashboardRouter} after a lecturer logs in; it works for the
 * logged-in user only and cannot be started on its own. Every screen loads its data from the
 * database through the lecturer services, which enforce what this lecturer may see and change.
 */
public class LecturerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient NoticeService noticeService = new NoticeService();
    private final transient LecturerService lecturerService = new LecturerService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContent = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();

    private final Color navy = new Color(23, 54, 92);
    private final Color sidebarColor = new Color(229, 236, 245);
    private final Color backgroundColor = new Color(244, 247, 251);
    private final Color textColor = new Color(40, 69, 99);

    /** Section name -> the object that reloads that section's data when it is opened. */
    private final java.util.Map<String, Refreshable> pages = new java.util.LinkedHashMap<>();

    /**
     * Refuses anyone who is not the logged-in lecturer (same rule as the other role windows).
     *
     * @throws IllegalStateException when the session user is not this lecturer
     */
    static void requireLecturerSession(User user) {
        if (user == null || Session.getCurrentUser() != user || !Session.hasRole(Role.LECTURER)) {
            throw new IllegalStateException(Role.LECTURER.getDisplayName() + " access required.");
        }
    }

    public LecturerDashboard(User user) {
        requireLecturerSession(user);
        setTitle("FacultyAMS - Lecturer Portal");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createHeader(user);
        createPages();
        createSidebar();
        showSection("Dashboard");
    }

    // ------------------------------------------------------------------ frame

    private void createHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("FacultyAMS - Lecturer Portal");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel who = new JLabel(user.getFullName() + "   ");
        who.setForeground(Color.WHITE);

        JButton notification = new JButton("Notices");
        notification.addActionListener(e -> showSection("Notices"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(who);
        right.add(notification);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void createSidebar() {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(sidebarColor);
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));
        sidebar.setPreferredSize(new Dimension(275, 0));

        JLabel menuTitle = new JLabel("MAIN MENU");
        menuTitle.setFont(new Font("Arial", Font.BOLD, 12));
        menuTitle.setForeground(textColor);
        menuTitle.setBorder(new EmptyBorder(0, 8, 15, 0));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuTitle);

        for (String item : pages.keySet()) {
            JButton button = createMenuButton(item);
            button.addActionListener(e -> showSection(item));
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }

        JButton logout = createMenuButton("Logout");
        logout.addActionListener(e -> DashboardRouter.logout(this));
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logout);

        add(sidebar, BorderLayout.WEST);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setPreferredSize(new Dimension(250, 40));
        button.setBackground(sidebarColor);
        button.setForeground(textColor);
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ------------------------------------------------------------------ pages

    private void createPages() {
        mainContent.setBackground(backgroundColor);

        addPage("Dashboard", "Lecturer Dashboard", new DashboardHomePanel(this::showSection));
        addPage("My Profile", "My Profile", new ProfilePage());
        addPage("Evaluation Components", "Evaluation Components", new EvaluationComponentsPanel());
        addPage("Marks Entry", "Marks Entry", new MarksEntryPanel());
        addPage("Exam Eligibility", "Exam Eligibility (CA and attendance)", new EligibilityPanel());
        addPage("Final Results and Grades", "Final Results and Grades", new ResultsPanel());
        addPage("Student Results and GPA", "Student Academic Results, SGPA and CGPA", new StudentResultsPanel());

        JTabbedPane tabs = new JTabbedPane();
        LecturerAttendancePanel attendance = new LecturerAttendancePanel();
        LecturerMedicalPanel medical = new LecturerMedicalPanel();
        tabs.addTab("Attendance", attendance);
        tabs.addTab("Medical Records", medical);
        tabs.addChangeListener(e -> ((Refreshable) tabs.getSelectedComponent()).refresh());
        addPage("Attendance and Medical Records", "Attendance and Medical Records", new Refreshable() {
            @Override
            public void refresh() {
                ((Refreshable) tabs.getSelectedComponent()).refresh();
            }
        }, tabs);

        addPage("Undergraduate Details", "Undergraduate Details", new StudentsPanel());
        addPage("Course Materials", "Course Materials", new CourseMaterialsPanel());
        addPage("Notices", "Notices", new NoticesPage());

        add(mainContent, BorderLayout.CENTER);
    }

    private <T extends JComponent & Refreshable> void addPage(String key, String heading, T content) {
        addPage(key, heading, content, content);
    }

    private void addPage(String key, String heading, Refreshable refresher, JComponent content) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(backgroundColor);
        page.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel title = new JLabel(heading);
        title.setFont(new Font("Arial", Font.BOLD, 23));
        title.setForeground(navy);
        page.add(title, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        mainContent.add(page, key);
        pages.put(key, refresher);
    }

    private void showSection(String name) {
        if (!pages.containsKey(name)) {
            return;
        }
        cardLayout.show(mainContent, name);
        pages.get(name).refresh();
    }

    // ------------------------------------------------------------------ profile

    /** Profile of the LOGGED-IN lecturer (taken from the session, never a fixed id). */
    private final class ProfilePage extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JPanel rows = new JPanel();
        private final JLabel status = new JLabel(" ");

        ProfilePage() {
            super(new BorderLayout(0, 10));
            setOpaque(false);
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            rows.setBackground(Color.WHITE);
            rows.setBorder(new EmptyBorder(25, 25, 25, 25));

            JButton edit = new JButton("Edit Profile");
            edit.addActionListener(e -> editProfile());
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
            buttons.setOpaque(false);
            buttons.add(edit);

            JPanel center = new JPanel(new BorderLayout(0, 10));
            center.setOpaque(false);
            center.add(rows, BorderLayout.NORTH);
            center.add(buttons, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);
        }

        @Override
        public void refresh() {
            LecturerUi.load(this, status, lecturerService::getCurrentLecturer, this::show);
        }

        private void show(Lecturer l) {
            rows.removeAll();
            addRow("Lecturer ID:", String.valueOf(l.getLecturerID()));
            addRow("Name:", l.getFullName());
            addRow("Email:", l.getEmail());
            addRow("Phone:", l.getPhone() == null ? "" : l.getPhone());
            addRow("Designation:", l.getDesignation());
            rows.revalidate();
            rows.repaint();
            status.setText(" ");
        }

        private void addRow(String label, String value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel l = new JLabel(label);
            l.setPreferredSize(new Dimension(120, 30));
            row.add(l, BorderLayout.WEST);
            row.add(new JLabel(value), BorderLayout.CENTER);
            rows.add(row);
            rows.add(Box.createVerticalStrut(6));
        }

        private void editProfile() {
            LecturerUi.load(this, status, lecturerService::getCurrentLecturer, current -> {
                JTextField name = new JTextField(current.getFullName());
                JTextField email = new JTextField(current.getEmail());
                JTextField phone = new JTextField(current.getPhone());
                JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
                form.add(new JLabel("Name:"));
                form.add(name);
                form.add(new JLabel("Email:"));
                form.add(email);
                form.add(new JLabel("Phone:"));
                form.add(phone);
                if (JOptionPane.showConfirmDialog(this, form, "Edit Profile",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                LecturerUi.load(this, status, () -> {
                    lecturerService.updateProfile(current.getUserID(), name.getText(), email.getText(),
                            phone.getText(), current.getProfilePicture());
                    return Boolean.TRUE;
                }, ok -> {
                    UIUtil.showInfo(this, "Profile updated successfully.");
                    refresh();
                });
            });
        }
    }

    // ------------------------------------------------------------------ notices (read-only)

    private final class NoticesPage extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JTextField searchField = new JTextField(20);
        private final JTextArea noticesArea = new JTextArea();
        private final JLabel status = new JLabel(" ");

        NoticesPage() {
            super(new BorderLayout(10, 10));
            setOpaque(false);
            noticesArea.setEditable(false);
            noticesArea.setFont(new Font("Arial", Font.PLAIN, 14));
            noticesArea.setLineWrap(true);
            noticesArea.setWrapStyleWord(true);

            JButton searchButton = new JButton("Search");
            JButton refreshButton = new JButton("Refresh");
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setOpaque(false);
            searchPanel.add(new JLabel("Search notices:"));
            searchPanel.add(searchField);
            searchPanel.add(searchButton);
            searchPanel.add(refreshButton);

            searchButton.addActionListener(e -> refresh());
            searchField.addActionListener(e -> refresh());
            refreshButton.addActionListener(e -> {
                searchField.setText("");
                refresh();
            });
            add(searchPanel, BorderLayout.NORTH);
            add(new JScrollPane(noticesArea), BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);
        }

        @Override
        public void refresh() {
            String keyword = searchField.getText();
            LecturerUi.load(this, status, () -> keyword == null || keyword.isBlank()
                    ? noticeService.getAllNotices() : noticeService.searchNotices(keyword.trim()), this::show);
        }

        private void show(List<Notice> notices) {
            if (notices.isEmpty()) {
                noticesArea.setText("No notices found.");
            } else {
                StringBuilder out = new StringBuilder();
                for (Notice n : notices) {
                    out.append("Title: ").append(n.getTitle())
                            .append("\nDate: ").append(n.getNoticeDate())
                            .append("\nPosted by: ").append(n.getCreatedByName() == null ? "Unknown" : n.getCreatedByName())
                            .append("\n").append(n.getContent())
                            .append("\n----------------------------------------\n\n");
                }
                noticesArea.setText(out.toString());
            }
            noticesArea.setCaretPosition(0);
            status.setText(" ");
        }
    }
}
