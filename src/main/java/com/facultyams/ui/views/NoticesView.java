package com.facultyams.ui.views;

import com.facultyams.model.Notice;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.BadgeLabel;
import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.components.NoticeDetailsDialog;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import com.facultyams.service.NoticeService;
import com.srms.controller.NavigationListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Notice board page. Layout and interaction state are kept local to this view
 * so the other portal screens are unaffected.
 */
public class NoticesView extends JPanel {
    private static final String[] CATEGORIES = {"ALL", "Examinations", "Academic", "General", "Events"};
    private final DataService dataService = DataService.getInstance();
    private final NoticeService noticeService = NoticeService.getInstance();
    private final NavigationListener navigationListener;
    private final Set<Integer> readNoticeIds = new HashSet<>();
    private final Set<Integer> savedNoticeIds = new HashSet<>();
    private final ButtonGroup categoryGroup = new ButtonGroup();
    private final Map<String, PillTab> categoryTabs = new LinkedHashMap<>();
    private final JPanel noticeListPanel = new NoticeListPanel();
    private final JLabel resultCount = new JLabel();
    private final JTextField searchField = new PlaceholderTextField();
    private final javax.swing.Timer searchTimer;
    private String selectedCategory = "ALL";
    private String searchKeyword = "";

    /** Creates the notice board screen. */
    public NoticesView() {
        this(null);
    }

    public NoticesView(NavigationListener navigationListener) {
        this.navigationListener = navigationListener;
        setOpaque(false);
        setLayout(new BorderLayout());
        searchTimer = new javax.swing.Timer(300, e -> {
            searchKeyword = searchField.getText().trim().toLowerCase(Locale.ROOT);
            renderNotices();
        });
        searchTimer.setRepeats(false);

        JPanel page = new NoticePage();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(24, 24, 24, 24));
        page.add(createHeaderCard());
        page.add(Box.createVerticalStrut(16));
        page.add(createToolbar());
        page.add(Box.createVerticalStrut(16));
        noticeListPanel.setOpaque(false);
        noticeListPanel.setLayout(new BoxLayout(noticeListPanel, BoxLayout.Y_AXIS));
        page.add(noticeListPanel);
        page.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        noticeService.addListener(notice -> SwingUtilities.invokeLater(() -> {
            readNoticeIds.add(notice.getId());
            renderNotices();
        }));
        renderNotices();
    }

    private JComponent createHeaderCard() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Official Notice Board");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Important circulars, exam notices, department bulletins, and event schedules.");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.TEXT_MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        FlatNoticeButton markRead = new FlatNoticeButton("Mark all as read", false);
        markRead.addActionListener(e -> {
            for (Notice notice : dataService.getNotices()) {
                readNoticeIds.add(notice.getId());
                noticeService.markAsRead(notice);
            }
            renderNotices();
        });
        JPanel action = new JPanel(new GridBagLayout());
        action.setOpaque(false);
        action.add(markRead);
        card.add(titleBlock, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        card.setPreferredSize(new Dimension(500, 88));
        card.setMinimumSize(new Dimension(0, 88));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JComponent createToolbar() {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(400, 40));
        row.setMinimumSize(new Dimension(0, 40));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tabs.setOpaque(false);
        for (String category : CATEGORIES) {
            PillTab tab = new PillTab(category);
            tab.setSelected(category.equals(selectedCategory));
            categoryGroup.add(tab);
            categoryTabs.put(category, tab);
            tab.addActionListener(e -> {
                selectedCategory = category;
                refreshCategoryTabs();
                renderNotices();
            });
            tabs.add(tab);
        }
        row.add(tabs, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);
        resultCount.setFont(Theme.FONT_SMALL);
        resultCount.setForeground(Theme.TEXT_MUTED);
        right.add(resultCount);
        right.add(createSearchBox());
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent createSearchBox() {
        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(true);
        search.setBackground(Color.WHITE);
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CONTROL_BORDER, 1),
                new EmptyBorder(0, 10, 0, 6)));
        search.setPreferredSize(new Dimension(280, 36));
        search.setMinimumSize(new Dimension(180, 36));
        search.setMaximumSize(new Dimension(360, 36));
        JLabel icon = new JLabel(IconHelper.getIcon("search", 16, Theme.TEXT_MUTED));
        searchField.setFont(Theme.FONT_BODY);
        searchField.setForeground(Theme.TEXT_MAIN);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.addActionListener(e -> {
            searchTimer.stop();
            searchKeyword = searchField.getText().trim().toLowerCase(Locale.ROOT);
            renderNotices();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });
        JButton clear = new JButton("x");
        clear.setFont(Theme.FONT_SMALL);
        clear.setForeground(Theme.TEXT_MUTED);
        clear.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        clear.setContentAreaFilled(false);
        clear.setFocusPainted(false);
        clear.setToolTipText("Clear search");
        clear.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });
        search.add(icon, BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);
        search.add(clear, BorderLayout.EAST);
        return search;
    }

    private void renderNotices() {
        noticeListPanel.removeAll();
        List<Notice> all = dataService.getNotices();
        List<Notice> matches = new ArrayList<>();
        for (Notice notice : all) {
            boolean categoryMatch = selectedCategory.equals("ALL")
                    || notice.getCategory().equalsIgnoreCase(selectedCategory);
            String query = searchKeyword;
            boolean searchMatch = query.isEmpty()
                    || contains(notice.getTitle(), query)
                    || contains(notice.getContent(), query)
                    || contains(notice.getPublisher(), query)
                    || contains(notice.getCategory(), query);
            if (categoryMatch && searchMatch) {
                matches.add(notice);
            }
        }
        resultCount.setText("Showing " + matches.size() + " of " + all.size() + " notices");
        for (int i = 0; i < matches.size(); i++) {
            Notice notice = matches.get(i);
            noticeListPanel.add(createNoticeCard(notice));
            if (i < matches.size() - 1) {
                noticeListPanel.add(Box.createVerticalStrut(12));
            }
        }
        if (matches.isEmpty()) {
            noticeListPanel.add(createEmptyState());
        }
        noticeListPanel.revalidate();
        noticeListPanel.repaint();
    }

    private JComponent createNoticeCard(Notice notice) {
        Color accent = categoryColor(notice.getCategory());
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, notice.isImportant() ? Theme.ACCENT_ROSE : accent),
                new EmptyBorder(14, 16, 14, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel iconTile = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(categoryTint(notice.getCategory()));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        iconTile.setOpaque(false);
        iconTile.setPreferredSize(new Dimension(48, 48));
        iconTile.setMinimumSize(new Dimension(48, 48));
        iconTile.setMaximumSize(new Dimension(48, 48));
        iconTile.add(new JLabel(IconHelper.getIcon(categoryIcon(notice.getCategory()), 22, accent)));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel(highlight(notice.getTitle()));
        title.setFont(readNoticeIds.contains(notice.getId()) ? Theme.FONT_CARD_TITLE : Theme.FONT_CARD_TITLE.deriveFont(Font.BOLD));
        title.setForeground(Theme.TEXT_MAIN);
        titleRow.add(title);
        titleRow.add(BadgeLabel.createStatusBadge(notice.getCategory()));
        if (notice.isImportant()) {
            titleRow.add(new BadgeLabel("IMPORTANT", Theme.ACCENT_ROSE_BG, Theme.ACCENT_ROSE));
        }
        details.add(titleRow);
        details.add(Box.createVerticalStrut(6));

        JTextArea body = new JTextArea(2, 60);
        body.setText(notice.getContent());
        body.setFont(Theme.FONT_BODY);
        body.setForeground(Theme.TEXT_MUTED);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder());
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        details.add(body);
        details.add(Box.createVerticalStrut(8));

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.add(metaItem("profile", notice.getPublisher()));
        meta.add(metaItem("calendar", notice.getDate()));
        details.add(meta);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setAlignmentY(Component.TOP_ALIGNMENT);
        FlatNoticeButton view = new FlatNoticeButton("View circular", false);
        view.setAlignmentX(Component.RIGHT_ALIGNMENT);
        view.addActionListener(e -> showNoticeDetailDialog(notice));
        JButton save = new JButton(savedNoticeIds.contains(notice.getId()) ? "Saved" : "Save");
        save.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        save.setForeground(savedNoticeIds.contains(notice.getId()) ? Theme.ACCENT_AMBER : Theme.TEXT_MUTED);
        save.setToolTipText(savedNoticeIds.contains(notice.getId()) ? "Remove saved notice" : "Save notice");
        save.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        save.setContentAreaFilled(false);
        save.setFocusPainted(false);
        save.setAlignmentX(Component.RIGHT_ALIGNMENT);
        save.addActionListener(e -> {
            if (!savedNoticeIds.add(notice.getId())) {
                savedNoticeIds.remove(notice.getId());
            }
            renderNotices();
        });
        right.add(view);
        right.add(Box.createVerticalStrut(4));
        right.add(save);

        card.add(iconTile, BorderLayout.WEST);
        card.add(details, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        Dimension preferred = card.getPreferredSize();
        int height = Math.max(106, preferred.height);
        card.setPreferredSize(new Dimension(600, height));
        card.setMinimumSize(new Dimension(0, height));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return card;
    }

    private JLabel metaItem(String iconName, String text) {
        JLabel label = new JLabel(text, IconHelper.getIcon(iconName, 14, Theme.TEXT_MUTED), SwingConstants.LEFT);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private JComponent createEmptyState() {
        CardPanel empty = new CardPanel(12);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(new EmptyBorder(32, 16, 32, 16));
        JLabel icon = new JLabel(IconHelper.getIcon("notice", 28, Theme.TEXT_SUBTLE));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel text = new JLabel(searchKeyword.isEmpty()
                ? "No notices in this category."
                : "No notices match \"" + searchField.getText().trim() + "\".");
        text.setFont(Theme.FONT_BODY);
        text.setForeground(Theme.TEXT_MUTED);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(icon);
        empty.add(Box.createVerticalStrut(12));
        empty.add(text);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension size = new Dimension(500, 132);
        empty.setPreferredSize(size);
        empty.setMinimumSize(new Dimension(0, size.height));
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
        return empty;
    }

    private void showNoticeDetailDialog(Notice notice) {
        noticeService.markAsRead(notice);
        readNoticeIds.add(notice.getId());
        renderNotices();
        Window owner = SwingUtilities.getWindowAncestor(this);
        NoticeDetailsDialog dialog = new NoticeDetailsDialog(owner, notice, noticeService, navigationListener);
        dialog.setVisible(true);
    }

    private void refreshCategoryTabs() {
        for (Map.Entry<String, PillTab> entry : categoryTabs.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(selectedCategory));
        }
    }

    private String highlight(String text) {
        if (searchKeyword.isEmpty()) return escapeHtml(text);
        String safe = escapeHtml(text);
        int index = safe.toLowerCase(Locale.ROOT).indexOf(escapeHtml(searchKeyword).toLowerCase(Locale.ROOT));
        if (index < 0) return safe;
        String query = escapeHtml(searchKeyword);
        StringBuilder result = new StringBuilder();
        int from = 0;
        String lower = safe.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        while ((index = lower.indexOf(lowerQuery, from)) >= 0) {
            result.append(safe, from, index).append("<span style='background:#FEF08A;'>")
                    .append(safe, index, index + query.length()).append("</span>");
            from = index + query.length();
        }
        result.append(safe.substring(from));
        return "<html>" + result + "</html>";
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String categoryIcon(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "examinations" -> "exams";
            case "academic" -> "graduation-cap";
            case "events" -> "calendar";
            default -> "notice";
        };
    }

    private static Color categoryColor(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "examinations" -> Theme.ACCENT_ROSE;
            case "academic" -> Theme.PRIMARY;
            case "events" -> Theme.ACCENT_GREEN;
            default -> Theme.ACCENT_GRAY;
        };
    }

    private static Color categoryTint(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "examinations" -> Theme.ACCENT_ROSE_BG;
            case "academic" -> Theme.ACCENT_BLUE_BG;
            case "events" -> Theme.ACCENT_GREEN_BG;
            default -> Theme.ACCENT_GRAY_BG;
        };
    }

    private final class PillTab extends JToggleButton {
        private final String label;

        PillTab(String text) {
            super(text);
            label = text;
            setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            setForeground(Theme.TEXT_MUTED);
            setPreferredSize(new Dimension(Math.max(58, getFontMetrics(getFont()).stringWidth(text) + 24), 36));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { repaint(); }
                @Override public void mouseExited(MouseEvent e) { repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean hover = getModel().isRollover();
            if (isSelected()) {
                g.setColor(Theme.PRIMARY);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                setForeground(Color.WHITE);
            } else {
                g.setColor(hover ? Theme.PRIMARY_LIGHT : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                setForeground(hover ? Theme.PRIMARY : Theme.TEXT_MUTED);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class FlatNoticeButton extends JButton {
        private final boolean primary;

        FlatNoticeButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            setPreferredSize(new Dimension(Math.max(104, getFontMetrics(getFont()).stringWidth(text) + 28), 36));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean hover = getModel().isRollover();
            if (primary) {
                g.setColor(hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                setForeground(Color.WHITE);
            } else {
                g.setColor(hover ? Theme.PRIMARY_LIGHT : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                setForeground(Theme.PRIMARY);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class PlaceholderTextField extends JTextField {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.TEXT_SUBTLE);
                g.setFont(getFont());
                Insets insets = getInsets();
                g.drawString("Search notices...", insets.left + 1,
                        (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                g.dispose();
            }
        }
    }

    private static class NoticePage extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static class NoticeListPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
