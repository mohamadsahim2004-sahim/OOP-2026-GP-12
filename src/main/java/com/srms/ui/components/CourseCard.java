package com.srms.ui.components;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.Course;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Moodle-style CourseCard with procedural/image banner (160px), semester pill,
 * course code/title, "⋮" menu, 4px completion bar, and hover states.
 */
public class CourseCard extends Ui.Card {
    private static final int BANNER_HEIGHT = 160;

    private final Course course;
    private final Consumer<Course> onOpen;
    private boolean hover = false;
    private boolean isFavourite = false;

    public CourseCard(Course course, Consumer<Course> onOpen) {
        super(12, Theme.CARD_BG, Theme.CARD_BORDER);
        this.course = course;
        this.onOpen = onOpen;

        setLayout(new BorderLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(300, 275));

        initUI();
        initListeners();
    }

    private void initUI() {
        // Top banner container (160px)
        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                CoverPainter.paintBanner(g2, getWidth(), getHeight(), course.code(), course.coverPath(), 12);
                g2.dispose();
            }
        };
        bannerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        bannerPanel.setPreferredSize(new Dimension(300, BANNER_HEIGHT));
        bannerPanel.setOpaque(false);

        // Dark pill badge for semester on the banner
        Ui.Pill semesterPill = new Ui.Pill(course.semester(), new Color(15, 23, 42, 180), Color.WHITE);
        bannerPanel.add(semesterPill);
        add(bannerPanel, BorderLayout.NORTH);

        // Details Panel
        JPanel detailsPanel = new JPanel(new BorderLayout(8, 6));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel codeLbl = new JLabel(course.code());
        codeLbl.setFont(Theme.FONT_SMALL);
        codeLbl.setForeground(Theme.TEXT_MUTED);

        JLabel titleLbl = new JLabel("<html><body style='width: 220px;'>" + course.title() + "</body></html>");
        titleLbl.setFont(Theme.FONT_BODY_BOLD);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        textStack.add(codeLbl);
        textStack.add(Box.createVerticalStrut(4));
        textStack.add(titleLbl);
        detailsPanel.add(textStack, BorderLayout.CENTER);

        // Bottom Bar (Progress + Menu button)
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setOpaque(false);

        // 4px progress line + label
        JPanel progressStack = new JPanel();
        progressStack.setLayout(new BoxLayout(progressStack, BoxLayout.Y_AXIS));
        progressStack.setOpaque(false);

        JLabel progressLbl = new JLabel(course.progressPercent() + "% complete");
        progressLbl.setFont(Theme.FONT_TINY);
        progressLbl.setForeground(Theme.TEXT_MUTED);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(course.progressPercent());
        progressBar.setPreferredSize(new Dimension(140, 4));
        progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 4));
        progressBar.setForeground(Theme.PRIMARY);
        progressBar.setBackground(new Color(229, 231, 235));
        progressBar.setBorderPainted(false);

        progressStack.add(progressLbl);
        progressStack.add(Box.createVerticalStrut(3));
        progressStack.add(progressBar);
        bottomBar.add(progressStack, BorderLayout.CENTER);

        // "⋮" menu button
        JButton menuBtn = new JButton("⋮");
        menuBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        menuBtn.setForeground(Theme.TEXT_MUTED);
        menuBtn.setFocusPainted(false);
        menuBtn.setBorderPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuBtn.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemOpen = new JMenuItem("Open");
        itemOpen.addActionListener(e -> {
            if (onOpen != null) onOpen.accept(course);
        });
        JMenuItem itemFav = new JMenuItem("Mark as favourite");
        itemFav.addActionListener(e -> {
            isFavourite = !isFavourite;
            itemFav.setText(isFavourite ? "Remove from favourites" : "Mark as favourite");
            JOptionPane.showMessageDialog(this,
                    isFavourite ? "Marked as favourite!" : "Removed from favourites.",
                    course.code(), JOptionPane.INFORMATION_MESSAGE);
        });
        JMenuItem itemHide = new JMenuItem("Hide");
        itemHide.addActionListener(e -> setVisible(false));

        popup.add(itemOpen);
        popup.add(itemFav);
        popup.add(itemHide);

        menuBtn.addActionListener(e -> popup.show(menuBtn, 0, menuBtn.getHeight()));
        bottomBar.add(menuBtn, BorderLayout.EAST);

        detailsPanel.add(bottomBar, BorderLayout.SOUTH);
        add(detailsPanel, BorderLayout.CENTER);
    }

    private void initListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                setBorderColor(Theme.PRIMARY);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                setBorderColor(Theme.CARD_BORDER);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onOpen != null) {
                    onOpen.accept(course);
                }
            }
        });
    }

    public Course getCourse() {
        return course;
    }

    public boolean isFavourite() {
        return isFavourite;
    }
}
