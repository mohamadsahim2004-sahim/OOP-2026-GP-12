package com.facultyams.ui.views;

import com.facultyams.model.Notice;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;

public class NoticesView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private String selectedCategory = "ALL";
    private String searchKeyword = "";
    private JPanel noticeListPanel;

    public NoticesView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Filter and Search Row
        content.add(createFilterBar());
        content.add(Box.createVerticalStrut(18));

        // Notices List Container
        noticeListPanel = new JPanel();
        noticeListPanel.setOpaque(false);
        noticeListPanel.setLayout(new BoxLayout(noticeListPanel, BoxLayout.Y_AXIS));
        content.add(noticeListPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
        renderNotices();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Official Notice Board");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Important circulars, exam notices, department bulletins, and event schedules.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        card.add(left, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setOpaque(false);

        // Category Buttons
        JPanel catPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        catPanel.setOpaque(false);

        String[] categories = {"ALL", "Examinations", "Academic", "General", "Events"};
        for (String cat : categories) {
            ModernButton btn = new ModernButton(cat, cat.equals(selectedCategory) ? ModernButton.StyleType.PRIMARY : ModernButton.StyleType.GHOST);
            btn.addActionListener(e -> {
                selectedCategory = cat;
                renderNotices();
                // update button highlights
                for (Component c : catPanel.getComponents()) {
                    if (c instanceof ModernButton b) {
                        // re-render panel
                    }
                }
                bar.getParent().revalidate();
                bar.getParent().repaint();
            });
            catPanel.add(btn);
        }

        // Search input
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setOpaque(false);
        JTextField tf = new JTextField(16);
        tf.setFont(Theme.FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        tf.addActionListener(e -> {
            searchKeyword = tf.getText().trim().toLowerCase();
            renderNotices();
        });

        ModernButton searchBtn = new ModernButton("Search", ModernButton.StyleType.PRIMARY);
        searchBtn.addActionListener(e -> {
            searchKeyword = tf.getText().trim().toLowerCase();
            renderNotices();
        });

        searchBox.add(tf, BorderLayout.CENTER);
        searchBox.add(searchBtn, BorderLayout.EAST);

        bar.add(catPanel, BorderLayout.WEST);
        bar.add(searchBox, BorderLayout.EAST);
        return bar;
    }

    private void renderNotices() {
        noticeListPanel.removeAll();
        List<Notice> list = dataService.getNotices();

        for (Notice n : list) {
            boolean matchCat = selectedCategory.equals("ALL") || n.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchSearch = searchKeyword.isEmpty() ||
                    n.getTitle().toLowerCase().contains(searchKeyword) ||
                    n.getContent().toLowerCase().contains(searchKeyword);

            if (matchCat && matchSearch) {
                noticeListPanel.add(createNoticeCard(n));
                noticeListPanel.add(Box.createVerticalStrut(14));
            }
        }

        if (noticeListPanel.getComponentCount() == 0) {
            JLabel emptyLbl = new JLabel("No notices found matching the filter.", JLabel.CENTER);
            emptyLbl.setFont(Theme.FONT_BODY);
            emptyLbl.setForeground(Theme.TEXT_MUTED);
            noticeListPanel.add(emptyLbl);
        }

        noticeListPanel.revalidate();
        noticeListPanel.repaint();
    }

    private JPanel createNoticeCard(Notice n) {
        CardPanel card = new CardPanel(14);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(Theme.createEmptyPadding(16, 20, 16, 20));

        // Icon box on left
        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(n.isImportant() ? Theme.ACCENT_ROSE_BG : Theme.ACCENT_BLUE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(50, 50));
        iconBox.add(new JLabel(IconHelper.getIcon("loudspeaker", 24, n.isImportant() ? Theme.ACCENT_ROSE : Theme.PRIMARY)));

        // Center Details
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(Theme.FONT_SECTION);
        titleLbl.setForeground(Theme.TEXT_MAIN);

        BadgeLabel catBadge = BadgeLabel.createStatusBadge(n.getCategory());

        topRow.add(titleLbl);
        topRow.add(catBadge);
        if (n.isImportant()) {
            BadgeLabel impBadge = new BadgeLabel("IMPORTANT", Theme.ACCENT_ROSE_BG, Theme.ACCENT_ROSE);
            topRow.add(impBadge);
        }

        JLabel bodyLbl = new JLabel("<html><body style='width: 580px;'>" + n.getContent() + "</body></html>");
        bodyLbl.setFont(Theme.FONT_BODY);
        bodyLbl.setForeground(Theme.TEXT_MUTED);

        JPanel bottomMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        bottomMeta.setOpaque(false);
        JLabel pubLbl = new JLabel("Posted by: " + n.getPublisher());
        pubLbl.setFont(Theme.FONT_SMALL);
        pubLbl.setForeground(Theme.TEXT_SUBTLE);

        JLabel dateLbl = new JLabel("Published: " + n.getDate());
        dateLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dateLbl.setForeground(Theme.PRIMARY);

        bottomMeta.add(pubLbl);
        bottomMeta.add(dateLbl);

        center.add(topRow);
        center.add(Box.createVerticalStrut(6));
        center.add(bodyLbl);
        center.add(Box.createVerticalStrut(8));
        center.add(bottomMeta);

        // Right button
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        ModernButton viewBtn = new ModernButton("View Circular", ModernButton.StyleType.OUTLINE);
        viewBtn.addActionListener(e -> showNoticeDetailDialog(n));
        right.add(viewBtn);

        card.add(iconBox, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private void showNoticeDetailDialog(Notice n) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Notice Details", true);
        dlg.setSize(520, 360);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Color.WHITE);
        p.setBorder(Theme.createEmptyPadding(24, 24, 24, 24));

        JLabel title = new JLabel("<html><b>" + n.getTitle() + "</b></html>");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel meta = new JLabel("Published by " + n.getPublisher() + " \u2022 " + n.getDate());
        meta.setFont(Theme.FONT_SMALL);
        meta.setForeground(Theme.PRIMARY);

        JTextArea ta = new JTextArea(n.getContent());
        ta.setFont(Theme.FONT_BODY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setBackground(new Color(248, 250, 252));
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        ModernButton close = new ModernButton("Close", ModernButton.StyleType.PRIMARY);
        close.addActionListener(e -> dlg.dispose());

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(meta, BorderLayout.SOUTH);

        p.add(top, BorderLayout.NORTH);
        p.add(ta, BorderLayout.CENTER);
        p.add(close, BorderLayout.SOUTH);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }
}
