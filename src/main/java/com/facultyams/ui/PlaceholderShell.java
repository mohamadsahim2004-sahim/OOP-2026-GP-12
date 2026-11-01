package com.facultyams.ui;

import com.facultyams.model.User;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.ui.admin.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * Temporary shell for Lecturer / Technical Officer / Undergraduate users: the same sidebar and header
 * as the Admin window with a single "Dashboard" page. A team member replaces it by writing their own
 * {@link AppShell} subclass and returning it from {@link DashboardRouter}.
 */
public final class PlaceholderShell extends AppShell {

    private static final long serialVersionUID = 1L;
    private final String moduleName;

    public PlaceholderShell(JFrame owner, User user, String moduleName) {
        super(owner, user, user.getRole());
        this.moduleName = moduleName;
        init();
    }

    @Override
    protected void registerPages() {
        addPage("dashboard", "Dashboard", "Dashboard", new WelcomePage());
    }

    /** The single placeholder page. */
    private final class WelcomePage extends JPanel implements Refreshable {

        private static final long serialVersionUID = 1L;
        private final JLabel line1 = new JLabel("", SwingConstants.LEFT);
        private final JLabel line2 = new JLabel("", SwingConstants.LEFT);
        private final JLabel line3 = new JLabel("", SwingConstants.LEFT);

        WelcomePage() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            line1.setFont(AdminTheme.SECTION);
            JPanel lines = new JPanel(new GridLayout(3, 1, 0, 8));
            lines.setOpaque(false);
            lines.add(line1);
            lines.add(line2);
            lines.add(line3);
            add(lines, BorderLayout.NORTH);
        }

        @Override
        public void refresh() {
            User me = com.facultyams.security.Session.getCurrentUser();
            if (me != null) {
                line1.setText("Welcome, " + me.getFullName());
                line2.setText("Logged in as: " + me.getRole().getDisplayName() + " (" + me.getUsername() + ")");
            }
            line3.setText(moduleName + " is under development.");
        }
    }
}