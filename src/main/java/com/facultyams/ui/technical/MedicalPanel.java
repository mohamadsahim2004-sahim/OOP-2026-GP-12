package com.facultyams.ui.technical;

import com.facultyams.ui.admin.Refreshable;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class MedicalPanel extends JPanel implements Refreshable {

    public MedicalPanel() {
        add(new JLabel("Medical page"));
    }

    @Override
    public void refresh() {
    }
}