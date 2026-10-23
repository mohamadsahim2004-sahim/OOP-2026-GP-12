package com.facultyams.ui.components;

import com.facultyams.model.Notice;
import com.facultyams.model.NoticeAttachment;
import com.facultyams.model.NoticeCategory;
import com.facultyams.model.NoticePriority;
import com.facultyams.service.NoticeService;
import com.facultyams.ui.theme.Theme;
import com.srms.controller.NavigationListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.DefaultCaret;

/** Undecorated, content-sized notice details dialog. */
public final class NoticeDetailsDialog extends JDialog {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);
    private final Notice notice;
    private final NoticeService noticeService;
    private final NavigationListener navigationListener;
    private final RoundedPanel shell = new RoundedPanel();
    private Point dragOrigin;
    private Point dialogOrigin;

    public NoticeDetailsDialog(Window owner, Notice notice, NoticeService noticeService,
                               NavigationListener navigationListener) {
        super(owner, "Notice Details", ModalityType.APPLICATION_MODAL);
        this.notice = notice;
        this.noticeService = noticeService;
        this.navigationListener = navigationListener;
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(Color.WHITE);
        buildContent();
        installKeyboardActions();
        pack();
        constrainHeight();
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { fadeIn(); }
        });
    }

    private void buildContent() {
        shell.setLayout(new BorderLayout());
        shell.setBorder(BorderFactory.createEmptyBorder(Theme.NOTICE_DIALOG_PADDING,
                Theme.NOTICE_DIALOG_PADDING, Theme.NOTICE_DIALOG_PADDING, Theme.NOTICE_DIALOG_PADDING));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildHeader());
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(buildTitle());
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(buildIssuer());
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(new Divider());
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(buildBody());

        List<NoticeAttachment> attachments = notice.getAttachments();
        if (!attachments.isEmpty()) {
            content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
            for (NoticeAttachment attachment : attachments) {
                content.add(new AttachmentRow(attachment));
                content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_ELEMENT_GAP));
            }
        }
        if (NoticeCategory.fromLabel(notice.getCategory()) == NoticeCategory.EXAMINATIONS
                && navigationListener != null) {
            content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_ELEMENT_GAP));
            content.add(buildTimetableLink());
        }
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(new Divider());
        content.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_SECTION_GAP));
        content.add(buildFooter());
        shell.add(content, BorderLayout.CENTER);
        JPanel shadow = new JPanel(new BorderLayout());
        shadow.setOpaque(false);
        shadow.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        shadow.add(shell);
        setContentPane(shadow);
        installDragging(content);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, Theme.NOTICE_DIALOG_ELEMENT_GAP);
        c.gridx = 0;
        c.weightx = 0;
        header.add(new CategoryIcon(NoticeCategory.fromLabel(notice.getCategory())), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.NOTICE_DIALOG_ELEMENT_GAP, 0));
        pills.setOpaque(false);
        pills.setAlignmentX(Component.LEFT_ALIGNMENT);
        pills.add(new CategoryPill(NoticeCategory.fromLabel(notice.getCategory())));
        if (notice.isImportant()) pills.add(new CategoryPill(NoticePriority.IMPORTANT));
        details.add(pills);
        details.add(Box.createVerticalStrut(Theme.NOTICE_DIALOG_ELEMENT_GAP));
        JLabel date = new JLabel(formatDate(notice.getDate()) + "  ·  " + relativeDate(notice.getDate()));
        date.setFont(Theme.ENROLLMENT_SMALL);
        date.setForeground(Theme.ENROLLMENT_MUTED);
        details.add(date);
        header.add(details, c);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        IconButton close = new IconButton();
        close.setToolTipText("Close");
        close.addActionListener(e -> dispose());
        header.add(close, c);
        return header;
    }

    private JComponent buildTitle() {
        JTextArea title = new JTextArea(notice.getTitle(), 1, 40);
        title.setFont(Theme.ENROLLMENT_PAGE_TITLE);
        title.setForeground(Theme.ENROLLMENT_TEXT);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);
        title.setBorder(BorderFactory.createEmptyBorder());
        title.setRows(Math.min(3, estimateWrappedLines(notice.getTitle(),
                title.getFontMetrics(title.getFont()), 500)));
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, title.getPreferredSize().height));
        return title;
    }

    private JComponent buildIssuer() {
        JPanel issuer = new JPanel(new GridBagLayout());
        issuer.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, Theme.NOTICE_DIALOG_ELEMENT_GAP);
        issuer.add(new AvatarBadge(initials(notice.getPublisher())), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        String issuerName = notice.getPublisher() == null ? "" : notice.getPublisher().trim();
        String department = "";
        int separator = issuerName.lastIndexOf('/');
        if (separator >= 0) {
            department = issuerName.substring(separator + 1).trim();
            issuerName = issuerName.substring(0, separator).trim();
        }
        JLabel name = new JLabel(issuerName);
        name.setFont(Theme.ENROLLMENT_BODY_BOLD);
        name.setForeground(Theme.ENROLLMENT_TEXT);
        JLabel dept = new JLabel(department);
        dept.setFont(Theme.ENROLLMENT_SMALL);
        dept.setForeground(Theme.ENROLLMENT_MUTED);
        labels.add(name);
        if (!department.isEmpty()) labels.add(dept);
        issuer.add(labels, c);
        return issuer;
    }

    private JComponent buildBody() {
        JTextArea body = new JTextArea(notice.getContent());
        body.setFont(Theme.ENROLLMENT_BODY);
        body.setForeground(Theme.NOTICE_DIALOG_BODY);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(true);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        body.setCaret(new DefaultCaret() {
            @Override public void paint(Graphics g) {}
        });
        body.setCaretColor(Theme.PRIMARY);
        body.setSelectionColor(Theme.PRIMARY_LIGHT);
        body.setSelectedTextColor(Theme.ENROLLMENT_TEXT);
        body.setColumns(56);
        body.setRows(estimateWrappedLines(notice.getContent(), body.getFontMetrics(body.getFont()), 500));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(
                new Dimension(Theme.NOTICE_DIALOG_SCROLLBAR_WIDTH, 0));
        int lineHeight = body.getFontMetrics(body.getFont()).getHeight();
        int desiredHeight = Math.max(lineHeight + Theme.NOTICE_DIALOG_ELEMENT_GAP,
                Math.min(Theme.NOTICE_DIALOG_BODY_MAX_HEIGHT, body.getRows() * lineHeight));
        scroll.setPreferredSize(new Dimension(Theme.NOTICE_DIALOG_WIDTH
                - Theme.NOTICE_DIALOG_PADDING * 2, desiredHeight));
        return scroll;
    }

    private JComponent buildTimetableLink() {
        JButton link = new JButton("View exam timetable");
        link.setFont(Theme.ENROLLMENT_BODY_BOLD);
        link.setForeground(Theme.PRIMARY);
        link.setBorder(BorderFactory.createEmptyBorder(Theme.NOTICE_DIALOG_ELEMENT_GAP, 0,
                Theme.NOTICE_DIALOG_ELEMENT_GAP, 0));
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.addActionListener(e -> {
            dispose();
            navigationListener.openTimetable();
        });
        return link;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.NOTICE_DIALOG_ELEMENT_GAP, 0));
        footer.setOpaque(false);
        NoticeButton markRead = new NoticeButton("Mark as read", false);
        markRead.setEnabled(!notice.isRead());
        if (notice.isRead()) markRead.setText("Read");
        markRead.addActionListener(e -> {
            noticeService.markAsRead(notice);
            markRead.setText("Read");
            markRead.setEnabled(false);
        });
        NoticeButton close = new NoticeButton("Close", true);
        close.addActionListener(e -> dispose());
        footer.add(markRead);
        footer.add(close);
        getRootPane().setDefaultButton(close);
        SwingUtilities.invokeLater(close::requestFocusInWindow);
        return footer;
    }

    private void installKeyboardActions() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void installDragging(Component header) {
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragOrigin = e.getLocationOnScreen();
                dialogOrigin = getLocation();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOrigin == null || dialogOrigin == null) return;
                Point current = e.getLocationOnScreen();
                setLocation(dialogOrigin.x + current.x - dragOrigin.x, dialogOrigin.y + current.y - dragOrigin.y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        for (Component child : ((Container) header).getComponents()) {
            child.addMouseListener(drag);
            child.addMouseMotionListener(drag);
        }
    }

    private void constrainHeight() {
        Rectangle screen = getGraphicsConfiguration() == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                : getGraphicsConfiguration().getBounds();
        int maxHeight = Math.max(Theme.NOTICE_DIALOG_CONTROL_HEIGHT * 4,
                (int) (screen.height * 0.7));
        if (getHeight() > maxHeight) setSize(Theme.NOTICE_DIALOG_WIDTH, maxHeight);
        else setSize(Theme.NOTICE_DIALOG_WIDTH, getHeight());
    }

    private void fadeIn() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) return;
        try {
            setOpacity(0f);
            Timer timer = new Timer(15, null);
            long started = System.nanoTime();
            timer.addActionListener(e -> {
                float progress = Math.min(1f, (System.nanoTime() - started) / 150_000_000f);
                setOpacity(progress);
                if (progress >= 1f) timer.stop();
            });
            timer.start();
        } catch (UnsupportedOperationException exception) {
            setOpacity(1f);
        }
    }

    private static int estimateWrappedLines(String text, FontMetrics metrics, int width) {
        if (text == null || text.isBlank()) return 1;
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            int lineWidth = 0;
            int paragraphLines = 1;
            for (String word : paragraph.split("\\s+")) {
                int wordWidth = metrics.stringWidth(word + " ");
                if (lineWidth > 0 && lineWidth + wordWidth > width) {
                    paragraphLines++;
                    lineWidth = wordWidth;
                } else {
                    lineWidth += wordWidth;
                }
            }
            lines += paragraphLines;
        }
        return Math.max(1, lines);
    }

    private static String formatDate(String value) {
        return parseDate(value).map(DATE_FORMAT::format).orElse(value == null ? "" : value);
    }

    private static String relativeDate(String value) {
        LocalDate date = parseDate(value).orElse(null);
        if (date == null) return "";
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days <= 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    private static java.util.Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(LocalDate.parse(value.trim(), DATE_FORMAT));
        } catch (DateTimeParseException exception) {
            return java.util.Optional.empty();
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "N";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private static final class RoundedPanel extends JPanel {
        RoundedPanel() {
            setOpaque(false);
            setBackground(Color.WHITE);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Theme.NOTICE_DIALOG_SHADOW);
            g.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 3,
                    Theme.NOTICE_DIALOG_RADIUS, Theme.NOTICE_DIALOG_RADIUS);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 4,
                    Theme.NOTICE_DIALOG_RADIUS, Theme.NOTICE_DIALOG_RADIUS);
            g.setColor(Theme.NOTICE_DIALOG_BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4,
                    Theme.NOTICE_DIALOG_RADIUS, Theme.NOTICE_DIALOG_RADIUS);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class Divider extends JComponent {
        Divider() { setPreferredSize(new Dimension(1, 1)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); }
        @Override protected void paintComponent(Graphics graphics) {
            graphics.setColor(Theme.NOTICE_DIALOG_BORDER);
            graphics.fillRect(0, 0, getWidth(), 1);
        }
    }

    private static final class CategoryIcon extends JComponent {
        private final NoticeCategory category;
        CategoryIcon(NoticeCategory category) {
            this.category = category;
            setPreferredSize(new Dimension(40, 40));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(category.tint());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.NOTICE_DIALOG_CONTROL_RADIUS,
                    Theme.NOTICE_DIALOG_CONTROL_RADIUS);
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            g.setColor(category.foreground());
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(x - 8, y - 6, 12, 12, 3, 3);
            g.drawLine(x + 4, y - 4, x + 9, y - 8);
            g.drawLine(x + 4, y + 4, x + 9, y + 8);
            g.drawLine(x - 4, y - 6, x - 4, y - 10);
            g.dispose();
        }
    }

    private static final class CategoryPill extends JComponent {
        private final String label;
        private final Color background;
        private final Color foreground;
        CategoryPill(NoticeCategory category) {
            this(category.displayLabel(), category.tint(), category.foreground());
        }
        CategoryPill(NoticePriority priority) {
            this(priority.displayLabel(), priority.tint(), priority.foreground());
        }
        private CategoryPill(String label, Color background, Color foreground) {
            this.label = label;
            this.background = background;
            this.foreground = foreground;
            setFont(Theme.ENROLLMENT_SMALL_BOLD);
            int width = getFontMetrics(getFont()).stringWidth(label) + Theme.NOTICE_DIALOG_ELEMENT_GAP * 2;
            setPreferredSize(new Dimension(width, Theme.NOTICE_DIALOG_CONTROL_HEIGHT - Theme.NOTICE_DIALOG_ELEMENT_GAP * 2));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setFont(getFont());
            g.setColor(foreground);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, (getWidth() - fm.stringWidth(label)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private static final class AvatarBadge extends JComponent {
        private final String text;
        AvatarBadge(String text) {
            this.text = text;
            setPreferredSize(new Dimension(28, 28));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Theme.PRIMARY_LIGHT);
            g.fillOval(0, 0, getWidth(), getHeight());
            g.setColor(Theme.PRIMARY);
            g.setFont(Theme.ENROLLMENT_SMALL_BOLD);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private static final class IconButton extends JButton {
        IconButton() {
            setPreferredSize(new Dimension(32, 32));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (getModel().isRollover()) {
                g.setColor(Theme.NOTICE_DIALOG_HOVER);
                g.fillOval(0, 0, getWidth(), getHeight());
            }
            g.setColor(Theme.ENROLLMENT_MUTED);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            g.drawLine(centerX - 4, centerY - 4, centerX + 4, centerY + 4);
            g.drawLine(centerX + 4, centerY - 4, centerX - 4, centerY + 4);
            g.dispose();
        }
    }

    private static final class NoticeButton extends JButton {
        private final boolean primary;
        NoticeButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(Theme.ENROLLMENT_BODY_BOLD);
            setForeground(primary ? Color.WHITE : Theme.NOTICE_DIALOG_BODY);
            setPreferredSize(new Dimension(Math.max(96, getFontMetrics(getFont()).stringWidth(text) + 32),
                    Theme.NOTICE_DIALOG_CONTROL_HEIGHT));
            setMinimumSize(new Dimension(96, Theme.NOTICE_DIALOG_CONTROL_HEIGHT));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (primary) {
                g.setColor(getModel().isPressed() ? Theme.NOTICE_DIALOG_PRESSED
                        : getModel().isRollover() ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.NOTICE_DIALOG_CONTROL_RADIUS,
                        Theme.NOTICE_DIALOG_CONTROL_RADIUS);
            } else {
                g.setColor(getModel().isRollover() ? Theme.NOTICE_DIALOG_SECONDARY_HOVER : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.NOTICE_DIALOG_CONTROL_RADIUS,
                        Theme.NOTICE_DIALOG_CONTROL_RADIUS);
                g.setColor(Theme.NOTICE_DIALOG_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.NOTICE_DIALOG_CONTROL_RADIUS, Theme.NOTICE_DIALOG_CONTROL_RADIUS);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class AttachmentRow extends JPanel {
        AttachmentRow(NoticeAttachment attachment) {
            setOpaque(true);
            setBackground(Theme.NOTICE_DIALOG_ATTACHMENT_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.NOTICE_DIALOG_BORDER),
                    BorderFactory.createEmptyBorder(Theme.NOTICE_DIALOG_ELEMENT_GAP,
                            Theme.NOTICE_DIALOG_ELEMENT_GAP, Theme.NOTICE_DIALOG_ELEMENT_GAP,
                            Theme.NOTICE_DIALOG_ELEMENT_GAP)));
            setLayout(new BorderLayout(Theme.NOTICE_DIALOG_ELEMENT_GAP, 0));
            Icon glyph = new VectorIcon("file", Theme.ENROLLMENT_MUTED);
            JLabel icon = new JLabel(glyph);
            add(icon, BorderLayout.WEST);
            JPanel names = new JPanel();
            names.setOpaque(false);
            names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
            JLabel fileName = new JLabel(attachment.fileName());
            fileName.setFont(Theme.ENROLLMENT_BODY_BOLD);
            JLabel size = new JLabel(attachment.fileSize());
            size.setFont(Theme.ENROLLMENT_SMALL);
            size.setForeground(Theme.ENROLLMENT_MUTED);
            names.add(fileName);
            names.add(size);
            add(names, BorderLayout.CENTER);
            JButton download = new JButton("Download", new VectorIcon("download", Theme.PRIMARY));
            download.setFont(Theme.ENROLLMENT_SMALL_BOLD);
            download.setForeground(Theme.PRIMARY);
            download.setBorder(BorderFactory.createEmptyBorder());
            download.setContentAreaFilled(false);
            download.setFocusPainted(false);
            download.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            download.addActionListener(e -> openAttachment(attachment));
            add(download, BorderLayout.EAST);
        }
        private static void openAttachment(NoticeAttachment attachment) {
            if (attachment.filePath() == null || attachment.filePath().isBlank()) return;
            File file = new File(attachment.filePath());
            if (file.exists() && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException("Could not open notice attachment: " + file, exception);
                }
            }
        }
    }

    private static final class VectorIcon implements Icon {
        private final String type;
        private final Color color;
        VectorIcon(String type, Color color) { this.type = type; this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if ("file".equals(type)) {
                g.drawRoundRect(x + 3, y + 1, 10, 14, 2, 2);
                g.drawLine(x + 5, y + 6, x + 11, y + 6);
                g.drawLine(x + 5, y + 9, x + 11, y + 9);
            } else {
                g.drawLine(x + 8, y + 1, x + 8, y + 10);
                g.drawLine(x + 5, y + 7, x + 8, y + 10);
                g.drawLine(x + 11, y + 7, x + 8, y + 10);
                g.drawLine(x + 3, y + 12, x + 13, y + 12);
                g.drawLine(x + 3, y + 12, x + 3, y + 15);
                g.drawLine(x + 13, y + 12, x + 13, y + 15);
            }
            g.dispose();
        }
    }

    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { thumbColor = Theme.NOTICE_DIALOG_SCROLLBAR; }
        @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
        private JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        @Override protected void paintTrack(Graphics graphics, JComponent c, Rectangle bounds) {}
        @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(thumbColor);
            g.fillRoundRect(bounds.x + 2, bounds.y, bounds.width - 4, bounds.height, 8, 8);
            g.dispose();
        }
    }
}
