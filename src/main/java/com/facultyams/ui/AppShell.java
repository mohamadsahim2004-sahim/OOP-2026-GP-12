package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.AdminIcons;
import com.facultyams.ui.admin.AdminIcons.Kind;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.ui.admin.Refreshable;
import com.facultyams.ui.admin.RoundedPanel;
import com.facultyams.util.UIUtil;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable application shell: navy-blue sidebar (user badge, navigation, logout), blue header bar
 * with the welcome text, and a white content area that shows one page at a time.
 *
 * <p>Abstraction: this class owns all layout, navigation and logout behaviour; a role's window only
 * has to say <em>which pages it has</em> by implementing {@link #registerPages()}.<br>
 * Polymorphism: {@code AdminShell} and {@code PlaceholderShell} (and any shell a team member adds)
 * are used through this type, and every page is used through {@link Refreshable}.<br>
 * Encapsulation: widgets and the page registry are private; subclasses use {@link #addPage} and
 * {@link #showPage}.
 *
 * <p>Access control: the constructor refuses to build the shell unless the session belongs to the
 * given user and has the required role, so a screen can never be shown to the wrong role.
 *
 * <p>To plug in a module: extend this class, call {@code super(owner, user, Role.X)}, implement
 * {@link #registerPages()} with {@code addPage(...)} calls and call {@link #init()} last in your constructor.
 */
public abstract class AppShell extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String LOGOUT = "logout";
    private static final String SELECTED = "nav.selected";
    private static final String COMPACT = "nav.compact";
    private static final int SIDEBAR_WIDTH = 248;
    private static final int SIDEBAR_COMPACT_WIDTH = 72;
    /** Below this window width the sidebar shrinks to icons only (tablet / phone-sized windows). */
    private static final int COMPACT_BELOW = 800;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel navPanel = new JPanel();
    private final JPanel sidebar = new JPanel();
    private final JPanel names = new JPanel(new GridLayout(2, 1));
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> pages = new LinkedHashMap<>();
    private final Map<String, JPanel> pageHeaders = new LinkedHashMap<>();
    private final JLabel welcomeLabel = new JLabel();
    private final JLabel sidebarName = new JLabel();
    private final JPanel userChip = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 10, 5), 36);
    private final JFrame owner;
    private String firstPage;
    private String currentPage;
    private boolean compact;

    /**
     * @param owner        the window that hosts this shell (closed on logout)
     * @param user         the logged-in user; must be the session's current user
     * @param requiredRole the only role allowed to see this shell
     * @throws IllegalStateException if nobody is logged in, or the session user does not have the required role
     */
    protected AppShell(JFrame owner, User user, Role requiredRole) {
        super(new BorderLayout());
        if (user == null || Session.getCurrentUser() != user || !Session.hasRole(requiredRole)) {
            throw new IllegalStateException(requiredRole.getDisplayName() + " access required.");
        }
        this.owner = owner;
        add(buildSidebar(user.getRole()), BorderLayout.WEST);
        content.setOpaque(true);
        content.setBackground(AdminTheme.PANEL_BG);
        buildUserChip();
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(buildTopBar(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setCompact(getWidth() > 0 && getWidth() < COMPACT_BELOW);
            }
        });
    }

    /** Adds this shell's pages by calling {@link #addPage} once per page, in navigation order. */
    protected abstract void registerPages();

    /** Registers the pages, fills in the user's name and opens the first page. Call last in a subclass constructor. */
    protected final void init() {
        registerPages();
        refreshUserLabels();
        if (firstPage != null) {
            showPage(firstPage);
        }
    }

    /** Adds a page with the default (dashboard) sidebar icon and no sub-heading. */
    protected final <T extends JComponent & Refreshable> void addPage(String key, String navText, String title, T screen) {
        addPage(key, navText, title, null, Kind.DASHBOARD, screen);
    }

    /**
     * Adds a page to the content area and a button for it to the sidebar.
     *
     * @param key      unique id used by {@link #showPage}
     * @param navText  text of the sidebar button
     * @param title    heading shown above the page
     * @param subtitle small grey line under the heading (may be null)
     * @param icon     sidebar icon
     * @param screen   the page; refreshed every time it is shown
     */
    protected final <T extends JComponent & Refreshable> void addPage(String key, String navText, String title,
                                                                      String subtitle, Kind icon, T screen) {
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel heading = AdminTheme.title(title);
        heading.setAlignmentX(0f);
        titles.add(heading);
        if (subtitle != null) {
            JLabel sub = AdminTheme.muted(subtitle);
            sub.setAlignmentX(0f);
            sub.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            titles.add(sub);
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        page.add(header, BorderLayout.NORTH);
        page.add(screen, BorderLayout.CENTER);
        AdminTheme.apply(screen);
        content.add(page, key);
        pages.put(key, screen);
        pageHeaders.put(key, header);
        addNav(key, navText, icon);
        if (firstPage == null) {
            firstPage = key;
        }
    }

    /** Shows the page with the given key, highlights its sidebar button and reloads its data. */
    public void showPage(String key) {
        if (!pages.containsKey(key)) {
            return;
        }
        cards.show(content, key);
        currentPage = key;
        navButtons.forEach((k, b) -> {
            b.putClientProperty(SELECTED, k.equals(key));
            b.repaint();
        });
        revalidate();
        pages.get(key).refresh();
    }

    /** Key of the page being shown, or null before the first page is opened. */
    public String getCurrentPage() {
        return currentPage;
    }

    /** Re-reads the current user from the session (e.g. after a profile update) and updates the name labels. */
    protected final void refreshUserLabels() {
        User me = Session.getCurrentUser();
        if (me != null) {
            welcomeLabel.setText("Welcome, " + me.getFullName());
            sidebarName.setText(me.getFullName());
        }
    }

    // ------------------------------------------------------------------ sidebar

    private JPanel buildSidebar(Role role) {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AdminTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 100));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 0, 18, 0));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 20, 22, 12));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        brand.setAlignmentX(0f);
        JLabel logo = new JLabel(AdminIcons.of(Kind.CAP, 22, Color.WHITE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminTheme.BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setHorizontalAlignment(JLabel.CENTER);
        logo.setPreferredSize(new Dimension(40, 40));
        JLabel roleLabel = new JLabel(role.getDisplayName());
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setFont(AdminTheme.FONT_BOLD.deriveFont(15f));
        sidebarName.setForeground(new Color(0x9FB3D9));
        sidebarName.setFont(AdminTheme.FONT_SMALL);
        names.setOpaque(false);
        names.add(roleLabel);
        names.add(sidebarName);
        JPanel logoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoWrap.setOpaque(false);
        logoWrap.add(logo);
        brand.add(logoWrap, BorderLayout.WEST);
        brand.add(names, BorderLayout.CENTER);
        sidebar.add(brand);

        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setAlignmentX(0f);
        sidebar.add(navPanel);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(navButton(LOGOUT, "Logout", Kind.LOGOUT));
        return sidebar;
    }

    /** Navy bar across the top of the content area: the system name on the left, the signed-in user on the right. */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AdminTheme.HEADER_BLUE);
        bar.setBorder(BorderFactory.createEmptyBorder(9, 28, 9, 20));
        JLabel title = new JLabel("Faculty Academic Management System");
        title.setForeground(Color.WHITE);
        title.setFont(AdminTheme.FONT_BOLD.deriveFont(15f));
        bar.add(title, BorderLayout.WEST);
        bar.add(userChip, BorderLayout.EAST);
        return bar;
    }

    private void buildUserChip() {
        JLabel avatar = new JLabel(AdminIcons.of(Kind.PROFILE, 18, AdminTheme.BLUE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminTheme.BLUE_LIGHT);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setHorizontalAlignment(JLabel.CENTER);
        avatar.setPreferredSize(new Dimension(30, 30));
        welcomeLabel.setFont(AdminTheme.FONT_BOLD);
        welcomeLabel.setForeground(AdminTheme.TEXT);
        userChip.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 8));
        userChip.add(avatar);
        userChip.add(welcomeLabel);
    }

    private void setCompact(boolean value) {
        if (value == compact) {
            return;
        }
        compact = value;
        sidebar.setPreferredSize(new Dimension(value ? SIDEBAR_COMPACT_WIDTH : SIDEBAR_WIDTH, 100));
        names.setVisible(!value);
        navButtons.values().forEach(b -> {
            b.putClientProperty(COMPACT, value);
            b.setToolTipText(value ? b.getText() : null);
        });
        revalidate();
        repaint();
    }

    private void addNav(String key, String text, Kind icon) {
        navPanel.add(navButton(key, text, icon));
        navPanel.revalidate();
    }

    private JButton navButton(String key, String text, Kind kind) {
        JButton b = new JButton(text);
        b.setUI(new NavUI());
        b.setFont(AdminTheme.FONT);
        b.setForeground(new Color(0xC3D0EA));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setHorizontalAlignment(JButton.LEFT);
        b.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                boolean on = Boolean.TRUE.equals(b.getClientProperty(SELECTED));
                AdminIcons.of(kind, 20, on ? Color.WHITE : new Color(0xA9BADB)).paintIcon(c, g, x, y);
            }

            @Override
            public int getIconWidth() {
                return 20;
            }

            @Override
            public int getIconHeight() {
                return 20;
            }
        });
        b.getAccessibleContext().setAccessibleName(text);
        b.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 46));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        b.setAlignmentX(0f);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(AdminTheme.SKIP, Boolean.TRUE);
        b.addActionListener(e -> {
            if (LOGOUT.equals(key)) {
                requestLogout();
            } else {
                showPage(key);
            }
        });
        navButtons.put(key, b);
        return b;
    }

    /** Sidebar item: rounded blue pill when selected, soft highlight on hover, icon only when compact. */
    private static final class NavUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            boolean selected = Boolean.TRUE.equals(b.getClientProperty(SELECTED));
            boolean small = Boolean.TRUE.equals(b.getClientProperty(COMPACT));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = c.getWidth() - 20;
            if (selected) {
                g2.setColor(AdminTheme.NAVY_ACTIVE);
                g2.fillRoundRect(10, 3, w, c.getHeight() - 6, 12, 12);
            } else if (b.getModel().isRollover() || b.getModel().isPressed()) {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(10, 3, w, c.getHeight() - 6, 12, 12);
            }
            if (b.isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 190));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(11, 4, w - 2, c.getHeight() - 8, 12, 12);
            }
            Icon icon = b.getIcon();
            int iconX = small ? (c.getWidth() - icon.getIconWidth()) / 2 : 28;
            icon.paintIcon(c, g2, iconX, (c.getHeight() - icon.getIconHeight()) / 2);
            if (!small) {
                g2.setFont(b.getFont());
                g2.setColor(selected ? Color.WHITE : b.getForeground());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(b.getText(), iconX + icon.getIconWidth() + 12,
                        (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /**
     * Asks for confirmation, then ends the session, closes this window and returns to the login window.
     * Used by the sidebar Logout button and by any page that offers its own Logout button.
     */
    protected final void requestLogout() {
        if (UIUtil.confirm(this, "Are you sure you want to log out?")) {
            DashboardRouter.logout(owner);
        }
    }
}