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

        JPanel tables = new JPanel(new GridLayout(1, 3, 16, 0));
        tables.add(section("Users by role", rolesModel));
        tables.add(section("Departments", departmentsModel));
        tables.add(section("Timetable load", timetableModel));

        add(top, BorderLayout.NORTH);
        add(tables, BorderLayout.CENTER);
        AdminTheme.apply(this);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JPanel section(String title, DefaultTableModel model) {
        JTable table = new JTable(model);
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
                rolesModel.addRow(new Object[]{r.role(), r.total(), r.active(), r.total() - r.active()});
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