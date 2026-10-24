package com.facultyams;

import com.facultyams.ui.LoginFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                // The system look and feel is optional: keep Swing's default and carry on.
                System.err.println("Could not apply the system look and feel, using the default: " + e.getMessage());
            }

            new LoginFrame().setVisible(true);
        });
    }
}