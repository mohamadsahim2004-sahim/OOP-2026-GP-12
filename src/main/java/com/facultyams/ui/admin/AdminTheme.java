package com.facultyams.ui.admin;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Colours, fonts and small styling helpers shared by every Admin screen so the
 * whole module has the navy-blue / white look of the reference design.
 */
public final class AdminTheme {

    public static final Color NAVY = new Color(0x0B2A66);
    public static final Color NAVY_DARK = new Color(0x08204F);
    public static final Color NAVY_ACTIVE = new Color(0x1E5FD8);
    public static final Color HEADER_BLUE = new Color(0x0D3B8E);
    public static final Color BLUE = new Color(0x1D63D8);
    public static final Color BLUE_LIGHT = new Color(0xE8F0FC);
    public static final Color PAGE_BG = new Color(0xFFFFFF);
    public static final Color PANEL_BG = new Color(0xF4F7FC);
    public static final Color BORDER = new Color(0xD5DDEB);
    public static final Color TEXT = new Color(0x1B2A41);
    public static final Color TEXT_MUTED = new Color(0x5F6F89);
    public static final Color DANGER = new Color(0xD93025);
    public static final Color SUCCESS = new Color(0x1E9E4A);
    public static final Color SUCCESS_BG = new Color(0xDDF3E4);

    public static final Color CARD_BLUE = new Color(0x1E6FE0);
    public static final Color CARD_GREEN = new Color(0x1F9D4D);
    public static final Color CARD_PURPLE = new Color(0x7B4BC4);
    public static final Color CARD_ORANGE = new Color(0xF59A1B);

    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SECTION = new Font("SansSerif", Font.BOLD, 16);

    /** Set this client property to TRUE on a component that styles itself; apply() then leaves it alone. */
    public static final String SKIP = "adminTheme.skip";

    private AdminTheme() {
        // utility class
    }

    /** Blue call-to-action button (Add, Search, Update ...). */
    public static void primary(AbstractButton b) {
        style(b, BLUE, Color.WHITE);
    }

    /** Red button for destructive actions (Delete). */
    public static void danger(AbstractButton b) {
        style(b, DANGER, Color.WHITE);
    }

    /** Light button for secondary actions (Clear, Show all). */
    public static void secondary(AbstractButton b) {
        style(b, BLUE_LIGHT, NAVY);
    }

    private static void style(AbstractButton b, Color background, Color foreground) {
        b.setBackground(background);
        b.setForeground(foreground);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Wrapping, read-only red text for load/validation problems shown inside a page (instead of a pop-up).
     * Empty text means "no problem"; callers hide it with setVisible(false).
     */
    public static JTextArea errorText() {
        JTextArea area = new JTextArea() {
            private static final long serialVersionUID = 1L;
            private int laidOutWidth;

            /** Wrapped text is as tall as its width allows, and the width is only known after a layout pass. */
            @Override
            public Dimension getPreferredSize() {
                if (getWidth() <= 0) {
                    return new Dimension(100, getFontMetrics(getFont()).getHeight());
                }
                return super.getPreferredSize();
            }

            /** Once the real width is assigned, lay out again so the height matches the wrapped text. */
            @Override
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x, y, width, height);
                if (width != laidOutWidth) {
                    laidOutWidth = width;
                    revalidate();
                }
            }
        };
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(FONT);
        area.setForeground(DANGER);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); // room for the descenders of the last line
        return area;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    /** Gives a table the reference look: navy header, light rows, no heavy grid. */
    public static void styleTable(JTable table) {
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setSelectionBackground(BLUE_LIGHT);
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(NAVY);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(FONT_BOLD);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        header.setDefaultRenderer(headerRenderer);
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 32));
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer();
        cell.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(Integer.class, cell);
    }

    /**
     * Walks a finished screen and applies the theme to plain Swing controls, so each
     * screen only has to build its layout. Buttons named Delete are red, Clear /
     * Show all are light, every other button is blue.
     */
    public static void apply(Container root) {
        if (root instanceof JPanel rootPanel) {
            rootPanel.setOpaque(true);
            rootPanel.setBackground(PAGE_BG);
        }
        applyToChildren(root);
    }

    private static void applyToChildren(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof javax.swing.JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(SKIP))) {
                continue;
            }
            if (c instanceof JButton button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase();
                if (text.startsWith("delete") || text.startsWith("remove")) {
                    danger(button);
                } else if (text.startsWith("clear") || text.startsWith("show all") || text.startsWith("reset")) {
                    secondary(button);
                } else {
                    primary(button);
                }
            } else if (c instanceof JTable table) {
                styleTable(table);
            } else if (c instanceof JTextField || c instanceof JComboBox<?> || c instanceof JTextArea) {
                c.setFont(FONT);
            } else if (c instanceof JLabel label && label.getFont().getStyle() == Font.PLAIN) {
                label.setFont(FONT);
                label.setForeground(TEXT);
            } else if (c instanceof JScrollPane scroll) {
                scroll.setBorder(BorderFactory.createLineBorder(BORDER));
                scroll.getViewport().setBackground(Color.WHITE);
            }
            if (c instanceof JPanel panel) {
                if (panel.getBorder() instanceof TitledBorder tb) {
                    tb.setTitleColor(NAVY);
                    tb.setTitleFont(FONT_BOLD);
                }
                panel.setOpaque(true);
                panel.setBackground(PAGE_BG);
            }
            if (c instanceof Container child) {
                applyToChildren(child);
            }
        }
    }
}package com.facultyams.ui.admin;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Colours, fonts and styling helpers shared by every Admin screen: dark-navy sidebar, light page background,
 * white rounded cards and blue actions, as in the reference design.
 *
 * <p>Buttons, inputs and combo boxes are painted by small UI delegates defined here, so they look identical
 * whichever Look-and-Feel the operating system provides.
 */
public final class AdminTheme {

    // ---- palette
    public static final Color NAVY = new Color(0x0A1B3D);          // sidebar
    public static final Color NAVY_DARK = new Color(0x07142E);
    public static final Color NAVY_ACTIVE = new Color(0x2563EB);   // selected sidebar item
    public static final Color HEADER_BLUE = new Color(0x0D3B8E);
    public static final Color BLUE = new Color(0x2563EB);
    public static final Color BLUE_DARK = new Color(0x1D4ED8);
    public static final Color BLUE_LIGHT = new Color(0xE8F0FE);
    public static final Color PAGE_BG = Color.WHITE;                // cards, dialogs
    public static final Color PANEL_BG = new Color(0xF5F8FC);       // page background
    public static final Color BORDER = new Color(0xDDE5F0);
    public static final Color TEXT = new Color(0x0F1F3D);
    public static final Color TEXT_MUTED = new Color(0x64748B);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color DANGER_BG = new Color(0xFDECEC);
    public static final Color SUCCESS = new Color(0x15803D);
    public static final Color SUCCESS_BG = new Color(0xDCFCE7);

    public static final Color CARD_BLUE = new Color(0x2563EB);
    public static final Color CARD_GREEN = new Color(0x16A34A);
    public static final Color CARD_PURPLE = new Color(0x7C3AED);
    public static final Color CARD_ORANGE = new Color(0xF59E0B);

    // ---- fonts
    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font SECTION = new Font("SansSerif", Font.BOLD, 16);

    /** Set this client property to TRUE on a component that styles itself; apply() then leaves it alone. */
    public static final String SKIP = "adminTheme.skip";

    private static final int ARC = 10;

    private AdminTheme() {
        // utility class
    }

    // ================================================================== buttons

    /** Blue call-to-action button (Add, Save, Login ...). */
    public static void primary(AbstractButton b) {
        style(b, BLUE, Color.WHITE);
    }

    /** Red button for destructive actions (Delete). */
    public static void danger(AbstractButton b) {
        style(b, DANGER, Color.WHITE);
    }

    /** Light-blue button for secondary actions (Cancel, Reset ...). */
    public static void secondary(AbstractButton b) {
        style(b, BLUE_LIGHT, BLUE_DARK);
    }

    /** White button with a border, for toggles and quiet actions. */
    public static void outline(AbstractButton b) {
        style(b, Color.WHITE, TEXT);
        b.putClientProperty("adminTheme.outline", Boolean.TRUE);
    }

    private static void style(AbstractButton b, Color background, Color foreground) {
        b.setUI(new PillButtonUI());
        b.setBackground(background);
        b.setForeground(foreground);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setIconTextGap(8);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static boolean isStyled(AbstractButton b) {
        return b.getUI() instanceof PillButtonUI;
    }

    static Color blend(Color a, Color b, float amountOfB) {
        float k = 1f - amountOfB;
        return new Color(Math.round(a.getRed() * k + b.getRed() * amountOfB),
                Math.round(a.getGreen() * k + b.getGreen() * amountOfB),
                Math.round(a.getBlue() * k + b.getBlue() * amountOfB));
    }

    /** Rounded button with hover / pressed / disabled states and a visible keyboard-focus ring. */
    private static final class PillButtonUI extends BasicButtonUI {
        private Color disabledText = TEXT_MUTED;

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Color bg = b.getBackground();
            boolean outline = Boolean.TRUE.equals(b.getClientProperty("adminTheme.outline"));
            if (!b.isEnabled()) {
                bg = blend(bg, Color.WHITE, 0.55f);
            } else if (m.isPressed()) {
                bg = blend(bg, Color.BLACK, 0.18f);
            } else if (m.isRollover()) {
                bg = blend(bg, outline ? BLUE : Color.BLACK, outline ? 0.06f : 0.10f);
            }
            disabledText = blend(b.getForeground(), bg, 0.5f);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), ARC, ARC);
            if (outline) {
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, ARC, ARC);
            }
            if (b.isFocusOwner()) {
                g2.setColor(blend(BLUE, Color.WHITE, 0.2f));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, ARC, ARC);
            }
            g2.dispose();
            super.paint(g, c);
        }

        @Override
        protected void paintText(Graphics g, AbstractButton b, java.awt.Rectangle textRect, String text) {
            if (b.isEnabled()) {
                super.paintText(g, b, textRect, text);
                return;
            }
            FontMetrics fm = g.getFontMetrics(b.getFont());
            g.setFont(b.getFont());
            g.setColor(disabledText);
            g.drawString(text, textRect.x, textRect.y + fm.getAscent());
        }
    }

    // ================================================================== inputs

    /** Rounded 1px border that turns blue while the field has keyboard focus. */
    static final class FieldBorder extends AbstractBorder implements UIResource {
        private static final long serialVersionUID = 1L;
        private final Insets padding;

        FieldBorder(int vertical, int horizontal) {
            this.padding = new Insets(vertical, horizontal, vertical, horizontal);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focus = c.hasFocus() || (c instanceof Container k && hasFocusedChild(k));
            g2.setColor(!c.isEnabled() ? BORDER : focus ? BLUE : new Color(0xC9D4E5));
            g2.drawRoundRect(x, y, w - 1, h - 1, 8, 8);
            if (focus) {
                g2.setColor(new Color(37, 99, 235, 70));
                g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 7, 7);
            }
            g2.dispose();
        }

        private static boolean hasFocusedChild(Container k) {
            for (Component child : k.getComponents()) {
                if (child.hasFocus()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padding.top + 1, padding.left + 1, padding.bottom + 1, padding.right + 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets i = getBorderInsets(c);
            insets.set(i.top, i.left, i.bottom, i.right);
            return insets;
        }
    }

    /** Rounded border for text inputs; repaints on focus so the blue ring follows the keyboard. */
    public static void field(JTextComponent f) {
        f.setFont(FONT);
        f.setForeground(TEXT);
        f.setBackground(Color.WHITE);
        f.setCaretColor(TEXT);
        f.setBorder(new FieldBorder(6, 10));
        f.addPropertyChangeListener("enabled", e -> f.setBackground(f.isEnabled() ? Color.WHITE : new Color(0xF1F5F9)));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.repaint();
            }
        });
    }

    /** White rounded combo box with a chevron button. */
    public static void combo(JComboBox<?> box) {
        box.setUI(new ComboUI());
        box.setFont(FONT);
        box.setForeground(TEXT);
        box.setBackground(Color.WHITE);
        box.setOpaque(false);
        box.setBorder(new FieldBorder(3, 6));
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static final class ComboUI extends BasicComboBoxUI {
        @Override
        protected javax.swing.JButton createArrowButton() {
            JButton arrow = new JButton(AdminIcons.of(AdminIcons.Kind.CHEVRON_DOWN, 14, TEXT_MUTED));
            arrow.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            arrow.setContentAreaFilled(false);
            arrow.setFocusable(false);
            arrow.setOpaque(false);
            return arrow;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? Color.WHITE : PANEL_BG);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
            g2.dispose();
            super.paint(g, c);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            // the rounded background is painted in paint()
        }
    }

    // ================================================================== scroll bars

    /** Thin rounded scroll bars without arrow buttons. */
    public static void slimScroll(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new SlimScrollUI());
        scroll.getHorizontalScrollBar().setUI(new SlimScrollUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 11));
    }

    private static final class SlimScrollUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(0xC3CEE0);
            trackColor = new Color(0, 0, 0, 0);
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setFocusable(false);
            return b;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle bounds) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging || isThumbRollover() ? new Color(0x9FB0CC) : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    // ================================================================== labels

    /**
     * Wrapping, read-only red text for load/validation problems shown inside a page (instead of a pop-up).
     * Empty text means "no problem"; callers hide it with setVisible(false).
     */
    public static JTextArea errorText() {
        JTextArea area = new JTextArea() {
            private static final long serialVersionUID = 1L;
            private int laidOutWidth;

            /** Wrapped text is as tall as its width allows, and the width is only known after a layout pass. */
            @Override
            public Dimension getPreferredSize() {
                if (getWidth() <= 0) {
                    return new Dimension(100, getFontMetrics(getFont()).getHeight());
                }
                return super.getPreferredSize();
            }

            /** Once the real width is assigned, lay out again so the height matches the wrapped text. */
            @Override
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x, y, width, height);
                if (width != laidOutWidth) {
                    laidOutWidth = width;
                    revalidate();
                }
            }
        };
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(FONT);
        area.setForeground(DANGER);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); // room for the descenders of the last line
        return area;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    // ================================================================== tables

    /** Light header, roomy rows, hairline separators; the look of the reference tables. */
    public static void styleTable(JTable table) {
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEDF1F7));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(BLUE_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new Cell(true));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        Cell cell = new Cell(false);
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(Integer.class, cell);
    }

    /**
     * Padded, left-aligned text cell. DefaultTableCellRenderer resets its border and colours on every call, so the
     * look has to be re-applied inside getTableCellRendererComponent.
     */
    private static final class Cell extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final boolean header;

        Cell(boolean header) {
            this.header = header;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(t, value, selected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            setHorizontalAlignment(LEADING);
            if (header) {
                setBackground(new Color(0xF3F6FB));
                setForeground(TEXT_MUTED);
                setFont(FONT_BOLD.deriveFont(12f));
            } else {
                setBackground(selected ? t.getSelectionBackground() : Color.WHITE);
                setForeground(TEXT);
                setFont(FONT);
            }
            return this;
        }
    }

    /** Draws the cell text as a rounded status pill: green for Active, red for Inactive, grey otherwise. */
    public static void badgeColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new BadgeRenderer());
    }

    private static final class BadgeRenderer extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private Color fill = PANEL_BG;
        private boolean selected;
        private Color selectedColor = BLUE_LIGHT;

        BadgeRenderer() {
            setFont(FONT_BOLD.deriveFont(12f));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean focus,
                                                       int row, int column) {
            String text = value == null ? "" : value.toString();
            setText(text);
            selected = isSelected;
            selectedColor = t.getSelectionBackground();
            switch (text.toLowerCase()) {
                case "active", "connected" -> {
                    fill = SUCCESS_BG;
                    setForeground(SUCCESS);
                }
                case "inactive", "disconnected" -> {
                    fill = DANGER_BG;
                    setForeground(DANGER);
                }
                default -> {
                    fill = new Color(0xEEF2F7);
                    setForeground(TEXT_MUTED);
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? selectedColor : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            FontMetrics fm = g2.getFontMetrics(getFont());
            int w = fm.stringWidth(getText()) + 20;
            int h = 22;
            int x = 14;
            int y = (getHeight() - h) / 2;
            g2.setColor(fill);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(getText(), x + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ================================================================== whole-screen styling

    /**
     * Walks a finished screen and applies the theme to plain Swing controls, so each screen only has to build
     * its layout. Buttons named Delete / Remove are red, Clear / Show all / Reset / Cancel are light, every other
     * button is blue. Controls that were already styled (or carry {@link #SKIP}) are left alone.
     */
    public static void apply(Container root) {
        if (root instanceof JPanel rootPanel) {
            rootPanel.setOpaque(false);
        }
        applyToChildren(root);
    }

    private static void applyToChildren(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(SKIP))) {
                continue;
            }
            if (c instanceof JButton button) {
                if (!isStyled(button)) {
                    String text = button.getText() == null ? "" : button.getText().toLowerCase();
                    if (text.startsWith("delete") || text.startsWith("remove")) {
                        danger(button);
                    } else if (text.startsWith("clear") || text.startsWith("show all") || text.startsWith("reset")
                            || text.startsWith("cancel") || text.startsWith("browse")) {
                        secondary(button);
                    } else {
                        primary(button);
                    }
                }
                continue;
            }
            if (c instanceof JComboBox<?> box) {
                if (!(box.getUI() instanceof ComboUI)) {
                    combo(box);
                }
                continue; // never touch the arrow button the UI created inside it
            }
            if (c instanceof JScrollBar) {
                continue;
            }
            if (c instanceof JTable table) {
                styleTable(table);
            } else if (c instanceof JTextComponent text) {
                if (text.getBorder() instanceof UIResource && !(text.getBorder() instanceof FieldBorder)
                        && !(c instanceof JTextArea ta && !ta.isEditable())) {
                    field(text);
                }
            } else if (c instanceof JCheckBox box) {
                box.setFont(FONT);
                box.setOpaque(false);
                box.setForeground(TEXT);
            } else if (c instanceof JLabel label) {
                if (label.getFont() instanceof UIResource || label.getFont().getStyle() == Font.BOLD
                        && label.getForeground() instanceof UIResource) {
                    label.setFont(label.getFont() instanceof UIResource ? FONT : label.getFont());
                }
                if (label.getForeground() instanceof UIResource) {
                    label.setForeground(TEXT);
                }
            } else if (c instanceof JScrollPane scroll) {
                scroll.getViewport().setBackground(Color.WHITE);
                slimScroll(scroll);
            }
            if (c instanceof JPanel panel) {
                if (panel.getBorder() instanceof TitledBorder tb) {
                    tb.setTitleColor(TEXT);
                    tb.setTitleFont(FONT_BOLD);
                }
                if (!(panel instanceof RoundedPanel)) {
                    panel.setOpaque(false);
                }
            }
            if (c instanceof Container child) {
                applyToChildren(child);
            }
        }
    }
}