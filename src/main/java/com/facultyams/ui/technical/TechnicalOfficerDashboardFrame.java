package com.facultyams.ui.technical;

import com.facultyams.model.User;

import javax.swing.JFrame;
import java.awt.Dimension;

public class TechnicalOfficerDashboardFrame extends JFrame {

    public TechnicalOfficerDashboardFrame(User user) {
        super("FoT-AMS - Technical Officer Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new TechnicalOfficerShell(this, user));
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }
}