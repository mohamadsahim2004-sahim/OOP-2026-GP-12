package com.facultyams.ui.admin;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.config.DatabaseConnection.ConnectionInfo;
import com.facultyams.security.Role;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Read-only system settings: the database connection in use (password never shown)
 * with a connection test, and a summary of what each role may do. The database
 * itself is configured in application.properties / db.properties, not in the GUI.
 */
public class SettingsPanel extends JPanel implements Refreshable {

    private final JLabel resultLabel = new JLabel(" ");
    private final JButton testButton = new JButton("Test Connection");
    private final JTextField hostField = readOnlyField();
    private final JTextField portField = readOnlyField();
    private final JTextField databaseField = readOnlyField();
    private final JTextField userField = readOnlyField();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JButton databaseTab = new JButton("Database Connection");
    private final JButton rolesTab = new JButton("Role Management");

    public SettingsPanel() {
        super(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(card(buildDatabaseTab()), "database");
        body.add(card(buildRoleTab()), "roles");

        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        switcher.setOpaque(false);
        switcher.add(databaseTab);
        switcher.add(rolesTab);
        databaseTab.addActionListener(e -> showTab("database"));
        rolesTab.addActionListener(e -> showTab("roles"));
        add(switcher, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        AdminTheme.apply(this);
        resultLabel.setFont(AdminTheme.FONT_BOLD);
        showTab("database");
    }

    /** Pill-style switch between the two sections; the selected one is blue, the other light. */
    private void showTab(String name) {
        boolean database = "database".equals(name);
        if (database) {
            AdminTheme.primary(databaseTab);
            AdminTheme.secondary(rolesTab);
        } else {
            AdminTheme.secondary(databaseTab);
            AdminTheme.primary(rolesTab);
        }
        cards.show(body, name);
    }

    /** White rounded card around one section. */
    private static JPanel card(JPanel content) {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.setOpaque(false);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JTextField readOnlyField() {
        JTextField f = new JTextField(22);
        f.setEditable(false);
        f.setBackground(AdminTheme.PANEL_BG);
        return f;
    }

    private JPanel buildDatabaseTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        addRow(form, 0, "Host", hostField);
        addRow(form, 1, "Port", portField);
        addRow(form, 2, "Database name", databaseField);
        addRow(form, 3, "Username", userField);
        JTextField password = readOnlyField();
        password.setText("\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022");
        addRow(form, 4, "Password", password);

        testButton.addActionListener(e -> testConnection());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(testButton);
        buttonRow.add(resultLabel);

        JLabel note = new JLabel("<html>These values come from application.properties / db.properties "
                + "(or the FOTAMS_DB_* environment variables). Edit those files to change the connection.</html>");
        note.setForeground(AdminTheme.TEXT_MUTED);
        note.setFont(AdminTheme.FONT);

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(form, BorderLayout.NORTH);
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        south.add(buttonRow, BorderLayout.NORTH);
        south.add(note, BorderLayout.CENTER);
        tab.add(south, BorderLayout.CENTER);
        return tab;
    }

    private static void addRow(JPanel form, int row, String label, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 15);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(field, c);
    }

    private JPanel buildRoleTab() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Role", "Permissions"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addRow(new Object[]{Role.ADMIN, "All access: users, departments, courses, notices, timetables, settings"});
        model.addRow(new Object[]{Role.LECTURER, "View & manage own data (courses, marks, results)"});
        model.addRow(new Object[]{Role.TECHNICAL_OFFICER, "Manage attendance & medical records"});
        model.addRow(new Object[]{Role.STUDENT, "View own data, notices and timetable"});
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(180);

        JLabel note = new JLabel("Permissions are enforced in the service layer (Session.requireRole), "
                + "so they cannot be bypassed from the GUI.");
        note.setForeground(AdminTheme.TEXT_MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel tab = new JPanel(new BorderLayout());
        tab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        tab.add(note, BorderLayout.SOUTH);
        return tab;
    }

    @Override
    public void refresh() {
        ConnectionInfo info = DatabaseConnection.getConnectionInfo();
        hostField.setText(info.host());
        portField.setText(info.port());
        databaseField.setText(info.database());
        userField.setText(info.user());
        resultLabel.setText(" ");
    }

    /** Tests the connection off the Swing thread so the window never freezes. */
    private void testConnection() {
        testButton.setEnabled(false);
        resultLabel.setForeground(AdminTheme.TEXT_MUTED);
        resultLabel.setText("Testing...");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                boolean ok;
                try {
                    ok = get();
                } catch (Exception e) {
                    ok = false;
                }
                testButton.setEnabled(true);
                resultLabel.setForeground(ok ? AdminTheme.SUCCESS : AdminTheme.DANGER);
                resultLabel.setText(ok ? "Connection successful!"
                        : "Connection failed - check MySQL is running and the settings above.");
            }
        }.execute();
    }
}