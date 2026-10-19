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
 * Beautiful, minimal course card matching the reference grid design.
 */
public class CourseCard extends Ui.Card {
    public static final int CARD_HEIGHT = 245;
    private static final int BANNER_HEIGHT = 135;

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
        setPreferredSize(new Dimension(320, CARD_HEIGHT));
        setMinimumSize(new Dimension(240, CARD_HEIGHT));
        setMaximumSize(new Dimension(Short.MAX_VALUE, CARD_HEIGHT));

        initUI();
        initListeners();
    }

    private void initUI() {
        // 1. Top Cover Banner (135px)
        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                CoverPainter.paintBanner(g2, getWidth(), getHeight(), course.code(), course.coverPath(), 12);
                g2.dispose();
            }
        };
        bannerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        bannerPanel.setPreferredSize(new Dimension(300, BANNER_HEIGHT));
        bannerPanel.setOpaque(false);

        // Dark pill badge for semester on the banner
        Ui.Pill semesterPill = new Ui.Pill(course.semester(), new Color(15, 23, 42, 210), Color.WHITE);
        bannerPanel.add(semesterPill);
        add(bannerPanel, BorderLayout.NORTH);

        // 2. Details Panel (White surface)
        JPanel detailsPanel = new JPanel(new BorderLayout(8, 4));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel codeLbl = new JLabel(course.code());
        codeLbl.setFont(Theme.FONT_SMALL);
        codeLbl.setForeground(Theme.TEXT_MUTED);

        JLabel titleLbl = new JLabel("<html><body style='width: 200px;'>" + course.title() + "</body></html>");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(new Color(14, 116, 144)); // Dark Cyan / Blue tint matching reference

        textStack.add(codeLbl);
        textStack.add(Box.createVerticalStrut(3));
        textStack.add(titleLbl);
        detailsPanel.add(textStack, BorderLayout.CENTER);

        // 3. Bottom Row: Progress + Menu Button
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);

        // Progress bar + completion text
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
        progressBar.setBackground(new Color(226, 232, 240));
        progressBar.setBorderPainted(false);

        progressStack.add(progressLbl);
        progressStack.add(Box.createVerticalStrut(2));
        progressStack.add(progressBar);
        bottomBar.add(progressStack, BorderLayout.CENTER);

        // "⋮" menu button with round dark badge
        JButton menuBtn = new JButton("⋮") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(new Color(15, 118, 110)); // Deep teal round button like reference
                g2.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        menuBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuBtn.setForeground(Color.WHITE);
        menuBtn.setPreferredSize(new Dimension(24, 24));
        menuBtn.setFocusPainted(false);
        menuBtn.setBorderPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
}
