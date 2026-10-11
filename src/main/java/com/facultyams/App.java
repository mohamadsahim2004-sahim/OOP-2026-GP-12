package com.facultyams;

import com.facultyams.ui.UndergraduateDashboardFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            UndergraduateDashboardFrame dashboard = new UndergraduateDashboardFrame();
            dashboard.setVisible(true);
        });
    }
}
