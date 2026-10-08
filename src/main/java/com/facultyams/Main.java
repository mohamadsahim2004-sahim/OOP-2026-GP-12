package com.facultyams;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.ui.LoginFrame;
import com.facultyams.util.UIUtil;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Application entry point. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException e) {
                System.err.println("Using default look and feel: " + e.getMessage());
            }

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);

            if (!DatabaseConnection.testConnection()) {
                UIUtil.showError(loginFrame, "Cannot connect to the MySQL database.\n"
                        + "1. Start MySQL\n"
                        + "2. Run database/schema.sql and database/sample_data.sql\n"
                        + "3. Check src/main/resources/db.properties (see README)");
            }
        });
    }
}