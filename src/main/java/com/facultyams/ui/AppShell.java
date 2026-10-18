package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.ui.admin.Refreshable;
import com.facultyams.util.UIUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel navPanel = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> pages = new LinkedHashMap<>();
    private final JLabel welcomeLabel = new JLabel();
    private final JLabel sidebarName = new JLabel();
    private final JFrame owner;
    private String firstPage;

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
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildHeader(), BorderLayout.NORTH);
        content.setBackground(Color.WHITE);
        main.add(content, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
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

    /**
     * Adds a page to the content area and a button for it to the sidebar.
     *
     * @param key     unique id used by {@link #showPage}
     * @param navText text of the sidebar button
     * @param title   heading shown above the page
     * @param screen  the page; refreshed every time it is shown
     */
    protected final <T extends JComponent & Refreshable> void addPage(String key, String navText, String title, T screen) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        page.add(AdminTheme.title(title), BorderLayout.NORTH);
        page.add(screen, BorderLayout.CENTER);
        AdminTheme.apply(screen);
        content.add(page, key);
        pages.put(key, screen);
        addNav(key, navText);
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
        navButtons.forEach((k, b) -> {
            if (!LOGOUT.equals(k)) {
                b.setBackground(k.equals(key) ? AdminTheme.NAVY_ACTIVE : AdminTheme.NAVY);
            }
        });
        pages.get(key).refresh();
    }

    /** Re-reads the current user from the session (e.g. after a profile update) and updates the name labels. */
    protected final void refreshUserLabels() {
        User me = Session.getCurrentUser();
        if (me != null) {
            welcomeLabel.setText("Welcome, " + me.getFullName());
            sidebarName.setText(me.getFullName());
        }
    }

    private JPanel buildSidebar(Role role) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AdminTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(230, 100));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

        JPanel profile = new JPanel(new BorderLayout(12, 0));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 10));
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        profile.setAlignmentX(0f);
        JLabel avatar = new JLabel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(AdminTheme.NAVY);
                g2.fillOval(getWidth() / 2 - 7, 9, 14, 14);
                g2.fillArc(getWidth() / 2 - 13, 26, 26, 26, 0, 180);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(46, 46));
        JLabel roleLabel = new JLabel(role.getDisplayName());
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setFont(AdminTheme.FONT_BOLD);
        sidebarName.setForeground(new Color(0xC9D6F2));
        sidebarName.setFont(AdminTheme.FONT);
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        names.add(roleLabel);
        names.add(sidebarName);
        profile.add(avatar, BorderLayout.WEST);
        profile.add(names, BorderLayout.CENTER);
        sidebar.add(profile);

        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setAlignmentX(0f);
        sidebar.add(navPanel);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(navButton(LOGOUT, "Logout"));
        return sidebar;
    }

    private void addNav(String key, String text) {
        navPanel.add(navButton(key, text));
        navPanel.revalidate();
    }

    private JButton navButton(String key, String text) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(AdminTheme.FONT);
        b.setForeground(Color.WHITE);
        b.setBackground(AdminTheme.NAVY);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(0f);
        b.addActionListener(e -> {
            if (LOGOUT.equals(key)) {
                logout();
            } else {
                showPage(key);
            }
        });
        navButtons.put(key, b);
        return b;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AdminTheme.HEADER_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JLabel app = new JLabel("Faculty Academic Management System");
        app.setForeground(Color.WHITE);
        app.setFont(new Font("SansSerif", Font.BOLD, 16));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(AdminTheme.FONT);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(welcomeLabel);
        header.add(app, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Asks for confirmation, then ends the session and returns to the login window. */
    private void logout() {
        if (UIUtil.confirm(this, "Are you sure you want to log out?")) {
            DashboardRouter.logout(owner);
        }
    }
}