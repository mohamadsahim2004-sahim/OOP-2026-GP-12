package com.facultyams.ui.admin;

import com.facultyams.dao.ReportDAO.DaySummary;
import com.facultyams.dao.ReportDAO.DepartmentSummary;
import com.facultyams.dao.ReportDAO.RoleSummary;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.service.ReportService;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Admin reports: users by role, department overview and timetable load, with CSV export. */
public class ReportsPanel extends JPanel implements Refreshable {

    private final ReportService reportService = new ReportService();
    private final DefaultTableModel rolesModel = model("Role", "Total", "Active", "Inactive");
    private final DefaultTableModel departmentsModel = model("Code", "Department", "Courses", "Users");
    private final DefaultTableModel timetableModel = model("Day", "Lecture slots");

    public ReportsPanel() {
        super(new BorderLayout(0, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");
        refreshButton.addActionListener(e -> refresh());
        exportButton.addActionListener(e -> exportCsv());
        top.add(refreshButton);
        top.add(exportButton);

        // Unequal shares: the department table needs the most room, the two-column timetable table the least.
        JPanel tables = new ScrollablePanel(new java.awt.GridBagLayout(), 640);
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.fill = java.awt.GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.insets = new java.awt.Insets(0, 0, 0, 16);
        gc.gridx = 0;
        gc.weightx = 1.0;
        tables.add(section("Users by role", rolesModel, 130, 55, 55, 65), gc);
        gc.gridx = 1;
        gc.weightx = 1.3;
        tables.add(section("Departments", departmentsModel, 55, 170, 65, 55), gc);
        gc.gridx = 2;
        gc.weightx = 0.7;
        gc.insets = new java.awt.Insets(0, 0, 0, 0);
        tables.add(section("Timetable load", timetableModel, 110, 90), gc);

        // below 640 px wide the three tables scroll sideways instead of cutting their columns short
        JScrollPane tableScroll = new JScrollPane(tables);
        tableScroll.setBorder(null);
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setOpaque(false);
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        AdminTheme.apply(this);
    }

    /** "TECHNICAL_OFFICER" -> "Technical Officer"; unknown values are shown as stored. */
    private static String roleName(String stored) {
        try {
            return com.facultyams.security.Role.valueOf(stored).getDisplayName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return String.valueOf(stored);
        }
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JPanel section(String title, DefaultTableModel model, int... widths) {
        JTable table = new JTable(model);
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        JLabel heading = new JLabel(title);
        heading.setFont(AdminTheme.SECTION);
        heading.setForeground(AdminTheme.TEXT);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        try {
            rolesModel.setRowCount(0);
            for (RoleSummary r : reportService.getUsersByRole()) {
                rolesModel.addRow(new Object[]{roleName(r.role()), r.total(), r.active(), r.total() - r.active()});
            }
            departmentsModel.setRowCount(0);
            for (DepartmentSummary d : reportService.getDepartmentOverview()) {
                departmentsModel.addRow(new Object[]{d.code(), d.name(), d.courses(), d.users()});
            }
            timetableModel.setRowCount(0);
            for (DaySummary d : reportService.getTimetableLoad()) {
                timetableModel.addRow(new Object[]{d.day(), d.slots()});
            }
        } catch (AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        }
    }

    private void exportCsv() {
        try {
            String csv = reportService.exportCsv();
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("faculty-report.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = chooser.getSelectedFile();
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            UIUtil.showInfo(this, "Report saved to " + file.getAbsolutePath());
        } catch (AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
        } catch (IOException e) {
            UIUtil.showInfo(this, "Could not save the file: " + e.getMessage());
        }
    }
}