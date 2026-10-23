package com.facultyams.ui.technical;

import com.facultyams.ui.admin.Refreshable;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class AttendanceReportPanel extends JPanel implements Refreshable {

    public AttendanceReportPanel() {
        add(new JLabel("Report page"));
    }

    @Override
    public void refresh() {
    }
}